package br.insper.delivery.entregador.escolha;

import java.util.List;
import java.util.Optional;

import br.insper.delivery.entregador.EntregadorDados;

/** Escolhe o primeiro entregador disponível na ordem devolvida pelo serviço de entregador. */
public class PrimeiroDisponivelStrategy implements EscolhaEntregadorStrategy {

	public static final String NOME = "primeiro-disponivel";

	@Override
	public String nome() {
		return NOME;
	}

	@Override
	public Optional<EntregadorDados> escolher(List<EntregadorDados> disponiveis) {
		return disponiveis.stream().findFirst();
	}
}
