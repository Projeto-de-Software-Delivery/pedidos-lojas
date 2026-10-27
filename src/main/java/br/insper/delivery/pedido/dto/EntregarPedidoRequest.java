package br.insper.delivery.pedido.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Corpo de POST /pedidos/{id}/entregar: o entregador informa o PIN de 4 dígitos que o cliente passou.
 */
public record EntregarPedidoRequest(
		@NotBlank String entregadorId,
		@NotBlank @Pattern(regexp = "\\d{4}", message = "PIN deve ter 4 dígitos") String pin) {
}
