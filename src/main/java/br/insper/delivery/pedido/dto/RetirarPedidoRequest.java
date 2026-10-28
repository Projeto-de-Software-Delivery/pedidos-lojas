package br.insper.delivery.pedido.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Corpo de POST /pedidos/{id}/retirar: o entregador confirma que pegou o pedido na loja, digitando o
 * código de retirada que a loja mostrou.
 */
public record RetirarPedidoRequest(
		@NotBlank String entregadorId,
		@NotBlank @Pattern(regexp = "\\d{4}", message = "Código de retirada deve ter 4 dígitos") String codigoRetirada) {
}
