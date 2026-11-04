package br.insper.delivery.entregador.escolha;

import java.util.List;
import java.util.Optional;

import br.insper.delivery.entregador.EntregadorDados;

/**
 * Padrão <b>Strategy</b>: regra de escolha de qual entregador recebe a corrida quando a loja
 * aceita um pedido. O {@code PedidoService} depende só desta interface; a implementação concreta
 * é escolhida pela {@link EscolhaEntregadorStrategyFactory} a partir da configuração
 * {@code entregador.escolha.estrategia}.
 */
public interface EscolhaEntregadorStrategy {

	/** Nome usado na configuração para selecionar esta estratégia. */
	String nome();

	/**
	 * @param disponiveis entregadores com status DISPONIVEL (pode ser vazia)
	 * @return o escolhido, ou vazio se não houver nenhum
	 */
	Optional<EntregadorDados> escolher(List<EntregadorDados> disponiveis);
}
