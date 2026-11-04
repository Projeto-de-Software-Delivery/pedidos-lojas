package br.insper.delivery.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import br.insper.delivery.TestJwtHelper;

/**
 * Substitui o JwtDecoder real (JWKS do Auth0) por um que valida a assinatura HS256
 * contra a chave simétrica de TestJwtHelper, sem bater na rede nem exigir issuer/audience.
 */
@TestConfiguration
public class TestJwtDecoderConfig {

	@Bean
	@Primary
	public JwtDecoder testJwtDecoder() {
		return NimbusJwtDecoder.withSecretKey(TestJwtHelper.secretKey()).build();
	}
}
