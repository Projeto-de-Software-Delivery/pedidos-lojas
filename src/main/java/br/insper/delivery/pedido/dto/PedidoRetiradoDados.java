package br.insper.delivery.pedido.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload do tópico pedido.retirado: o entregador saiu da loja com o pedido. clienteId e lojaId são
 * opcionais na entrada (/eventos) e sempre preenchidos na publicação, porque o serviço de notificações
 * precisa deles para saber quem avisar.
 */
public record PedidoRetiradoDados(
		@NotBlank String pedidoId,
		@NotBlank String entregaId,
		@NotBlank String entregadorId,
		@NotNull Instant retiradoEm,
		@NotNull Integer etaEntregaMin,
		String clienteId,
		String lojaId) {
}
