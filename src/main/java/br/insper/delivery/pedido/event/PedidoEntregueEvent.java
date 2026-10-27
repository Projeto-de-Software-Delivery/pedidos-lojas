package br.insper.delivery.pedido.event;

import org.springframework.context.ApplicationEvent;

public class PedidoEntregueEvent extends ApplicationEvent {

	private final PedidoEntregueEvento evento;

	public PedidoEntregueEvent(Object source, PedidoEntregueEvento evento) {
		super(source);
		this.evento = evento;
	}

	public PedidoEntregueEvento getEvento() {
		return evento;
	}
}
