package br.insper.delivery;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utilitário de testes para gerar tokens JWT com claims sub e email.
 */
public final class TestJwtHelper {

	private TestJwtHelper() {
	}

	public static String createToken(String sub, String email) {
		String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
		String emailClaim = email != null ? String.format(",\"email\":\"%s\"", email) : "";
		String payloadJson = String.format("{\"sub\":\"%s\"%s}", sub, emailClaim);
		String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
		return header + "." + payload + ".sig";
	}
}
