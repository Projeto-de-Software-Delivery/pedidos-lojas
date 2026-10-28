package br.insper.delivery.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro que intercepta o header Authorization Bearer e extrai as claims sub e email do token Auth0.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

	private final ObjectMapper objectMapper;

	public JwtAuthFilter() {
		this(new ObjectMapper());
	}

	public JwtAuthFilter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
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

		String[] parts = token.split("\\.");
		if (parts.length >= 2) {
			try {
				byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
				String payload = new String(decoded, StandardCharsets.UTF_8);
				JsonNode node = objectMapper.readTree(payload);
				String sub = node.has("sub") ? node.get("sub").asText() : null;
				String email = node.has("email") ? node.get("email").asText() : null;
				return new Auth0User(sub, email);
			} catch (Exception e) {
				log.warn("Erro ao decodificar payload JWT: {}", e.getMessage());
			}
		} else if (token.contains("|") || token.startsWith("auth0") || token.startsWith("google-oauth2")) {
			// Token em formato simplificado para mocks e testes locais
			return new Auth0User(token, null);
		}

		return null;
	}
}
