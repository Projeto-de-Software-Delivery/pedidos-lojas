package br.insper.delivery.loja.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.insper.delivery.loja.domain.Loja;

public interface LojaRepository extends JpaRepository<Loja, Long> {

	Optional<Loja> findByAuth0Sub(String auth0Sub);
}

