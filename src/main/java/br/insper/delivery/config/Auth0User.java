package br.insper.delivery.config;

/**
 * Representa os dados do usuário autenticado extraídos do token JWT do Auth0.
 */
public record Auth0User(String sub, String email) {
}
