package br.insper.delivery.pedido.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * De tempos em tempos cancela os pedidos que a loja não aceitou dentro do prazo dela.
 */
@Component
public class PrazoAceiteJob {

	private final PedidoService pedidoService;

	public PrazoAceiteJob(PedidoService pedidoService) {
		this.pedidoService = pedidoService;
	}

	@Scheduled(fixedDelayString = "${pedidos.prazo-aceite.intervalo-ms:30000}")
	public void cancelarVencidos() {
		pedidoService.cancelarPedidosComPrazoDeAceiteVencido();
	}
}
