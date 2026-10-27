package br.insper.delivery.pedido.event;

import org.springframework.context.ApplicationEvent;

public class PedidoRetiradoEvent extends ApplicationEvent {

	private final PedidoRetiradoEvento evento;

	public PedidoRetiradoEvent(Object source, PedidoRetiradoEvento evento) {
		super(source);
		this.evento = evento;
	}

	public PedidoRetiradoEvento getEvento() {
		return evento;
	}
}
