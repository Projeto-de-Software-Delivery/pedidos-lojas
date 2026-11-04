package br.insper.delivery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Decoder manual (mesmo motivo do gateway): o auto-configurado pelo Spring Boot
 * busca o /.well-known/openid-configuration do Auth0 na subida da aplicação, o
 * que derrubaria o boot sem rede ou com AUTH0_DOMAIN ainda não configurado. O
 * JWK set aqui só é buscado na hora de validar um token de verdade.
 */
@Configuration
public class JwtDecoderConfig {

	@Bean
	public JwtDecoder jwtDecoder(Auth0Properties auth0Properties) {
		NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(auth0Properties.jwkSetUri()).build();

		OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(auth0Properties.issuerUri());
		OAuth2TokenValidator<Jwt> withAudience = new AudienceValidator(auth0Properties.audience());
		jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));

		return jwtDecoder;
	}

	private record AudienceValidator(String audience) implements OAuth2TokenValidator<Jwt> {

		@Override
		public OAuth2TokenValidatorResult validate(Jwt jwt) {
			if (jwt.getAudience().contains(audience)) {
				return OAuth2TokenValidatorResult.success();
			}
			OAuth2Error erro = new OAuth2Error("invalid_token", "O token não contém o audience exigido", null);
			return OAuth2TokenValidatorResult.failure(erro);
		}
	}
}
