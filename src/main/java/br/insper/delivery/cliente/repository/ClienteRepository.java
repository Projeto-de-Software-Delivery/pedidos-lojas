package br.insper.delivery.cliente.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.insper.delivery.cliente.domain.Cliente;

/**
 * Repositório para gerenciar clientes.
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

	Optional<Cliente> findByAuth0Sub(String auth0Sub);

	Optional<Cliente> findByEmail(String email);
}

