package br.insper.delivery.entregador.escolha;

import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

import br.insper.delivery.entregador.EntregadorDados;

/** Sorteia um dos entregadores disponíveis, distribuindo as corridas entre eles. */
public class AleatorioStrategy implements EscolhaEntregadorStrategy {

	public static final String NOME = "aleatorio";

	private final RandomGenerator random;

	public AleatorioStrategy() {
		this(RandomGenerator.getDefault());
	}

	AleatorioStrategy(RandomGenerator random) {
		this.random = random;
	}

	@Override
	public String nome() {
		return NOME;
	}

	@Override
	public Optional<EntregadorDados> escolher(List<EntregadorDados> disponiveis) {
		if (disponiveis.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(disponiveis.get(random.nextInt(disponiveis.size())));
	}
}
