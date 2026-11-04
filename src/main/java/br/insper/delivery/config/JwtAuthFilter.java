package br.insper.delivery.config;

import java.io.IOException;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro que intercepta o header Authorization Bearer, valida a assinatura do
 * token (issuer, audience e JWKS do Auth0) e extrai as claims sub e email.
 * Um token sem assinatura válida nunca autentica.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

	private final JwtDecoder jwtDecoder;

	public JwtAuthFilter(JwtDecoder jwtDecoder) {
		this.jwtDecoder = jwtDecoder;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String authHeader = request.getHeader("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7).trim();
			Auth0User user = parseToken(token);
			if (user != null && user.sub() != null) {
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						user,
						token,
						Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
				);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		}
		filterChain.doFilter(request, response);
	}

	public Auth0User parseToken(String token) {
		if (token == null || token.isBlank()) {
			return null;
		}

		try {
			Jwt jwt = jwtDecoder.decode(token);
			String sub = jwt.getSubject();
			if (sub == null) {
				return null;
			}
			String email = jwt.getClaimAsString("email");
			return new Auth0User(sub, email);
		} catch (JwtException e) {
			log.warn("Token JWT inválido ou com assinatura incorreta: {}", e.getMessage());
			return null;
		}
	}
}
