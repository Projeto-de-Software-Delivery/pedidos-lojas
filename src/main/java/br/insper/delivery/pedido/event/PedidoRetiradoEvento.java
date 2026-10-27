package br.insper.delivery.pedido.event;

import java.time.Instant;
import java.util.UUID;

import br.insper.delivery.pedido.dto.PedidoRetiradoDados;

/**
 * Envelope do evento publicado no tópico pedido.retirado, conforme o contrato definido em
 * payloads.md.
 */
public record PedidoRetiradoEvento(String eventId, String eventType, int version, Instant occurredAt,
		PedidoRetiradoDados data) {

	public static PedidoRetiradoEvento de(PedidoRetiradoDados dados) {
		return new PedidoRetiradoEvento(UUID.randomUUID().toString(), "pedido.retirado", 1, Instant.now(), dados);
	}
}
