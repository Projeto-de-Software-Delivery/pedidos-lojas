package br.insper.delivery.entregador.escolha;

import java.util.List;
import java.util.Locale;

/**
 * Padrão <b>Factory</b>: cria a {@link EscolhaEntregadorStrategy} a partir do nome configurado,
 * escondendo do resto do código quais implementações existem e como são construídas.
 */
public final class EscolhaEntregadorStrategyFactory {

	public static final List<String> NOMES_SUPORTADOS = List.of(PrimeiroDisponivelStrategy.NOME,
			AleatorioStrategy.NOME, VeiculoPreferidoStrategy.NOME);

	private EscolhaEntregadorStrategyFactory() {
	}

	public static EscolhaEntregadorStrategy criar(String nome) {
		String normalizado = nome == null ? "" : nome.trim().toLowerCase(Locale.ROOT);
		return switch (normalizado) {
			case PrimeiroDisponivelStrategy.NOME -> new PrimeiroDisponivelStrategy();
			case AleatorioStrategy.NOME -> new AleatorioStrategy();
			case VeiculoPreferidoStrategy.NOME -> new VeiculoPreferidoStrategy();
			default -> throw new IllegalArgumentException("Estratégia de escolha de entregador desconhecida: '"
					+ nome + "'. Use uma de " + NOMES_SUPORTADOS);
		};
	}
}
