package br.insper.delivery.config;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utilitários de segurança para extração do usuário corrente autenticado via Auth0.
 */
public final class SecurityUtils {

	private SecurityUtils() {
	}

	public static Optional<Auth0User> getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof Auth0User user) {
			return Optional.of(user);
		}
		return Optional.empty();
	}

	public static Optional<String> getCurrentUserSub() {
		return getCurrentUser().map(Auth0User::sub);
	}

	public static Optional<String> getCurrentUserEmail() {
		return getCurrentUser().map(Auth0User::email);
	}
}
