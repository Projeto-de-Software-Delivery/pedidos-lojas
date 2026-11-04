package br.insper.delivery;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Utilitário de testes para gerar tokens JWT assinados (HS256) com claims sub e email.
 * A assinatura usa uma chave simétrica só de teste, verificada pelo TestJwtDecoderConfig
 * (ou por um NimbusJwtDecoder.withSecretKey(secretKey()) construído direto no teste) —
 * nunca o JWKS real do Auth0.
 */
public final class TestJwtHelper {

	public static final String TEST_SECRET = "chave-secreta-apenas-para-testes-nao-usar-em-producao";

	private TestJwtHelper() {
	}

	public static SecretKey secretKey() {
		return new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	public static String createToken(String sub, String email) {
		try {
			JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
					.subject(sub)
					.expirationTime(new Date(System.currentTimeMillis() + 3_600_000L));
			if (email != null) {
				claims.claim("email", email);
			}

			SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
			signedJWT.sign(new MACSigner(secretKey()));
			return signedJWT.serialize();
		} catch (JOSEException e) {
			throw new IllegalStateException("Erro ao gerar token JWT de teste", e);
		}
	}
}
