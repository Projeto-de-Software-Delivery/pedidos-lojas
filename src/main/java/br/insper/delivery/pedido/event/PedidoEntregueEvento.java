package br.insper.delivery.pedido.event;

import java.time.Instant;
import java.util.UUID;

import br.insper.delivery.pedido.dto.PedidoEntregueDados;

/**
 * Envelope do evento publicado no tópico pedido.entregue, conforme o contrato definido em
 * payloads.md.
 */
public record PedidoEntregueEvento(String eventId, String eventType, int version, Instant occurredAt,
		PedidoEntregueDados data) {

	public static PedidoEntregueEvento de(PedidoEntregueDados dados) {
		return new PedidoEntregueEvento(UUID.randomUUID().toString(), "pedido.entregue", 1, Instant.now(), dados);
	}
}
