package br.insper.delivery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth0")
public record Auth0Properties(String domain, String audience) {

	public String issuerUri() {
		return "https://" + domain + "/";
	}

	public String jwkSetUri() {
		return "https://" + domain + "/.well-known/jwks.json";
	}
}
