package br.insper.delivery.entregador.escolha;

import java.util.List;
import java.util.Optional;

import br.insper.delivery.entregador.EntregadorDados;

/**
 * Prefere entregadores de moto (mais rápidos no trânsito urbano); se não houver nenhum, cai para o
 * primeiro disponível com qualquer veículo.
 */
public class VeiculoPreferidoStrategy implements EscolhaEntregadorStrategy {

	public static final String NOME = "preferir-moto";
	private static final String VEICULO_PREFERIDO = "MOTO";

	@Override
	public String nome() {
		return NOME;
	}

	@Override
	public Optional<EntregadorDados> escolher(List<EntregadorDados> disponiveis) {
		return disponiveis.stream()
				.filter(entregador -> VEICULO_PREFERIDO.equalsIgnoreCase(entregador.veiculo()))
				.findFirst()
				.or(() -> disponiveis.stream().findFirst());
	}
}
