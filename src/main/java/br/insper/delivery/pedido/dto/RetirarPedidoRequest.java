package br.insper.delivery.pedido.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo de POST /pedidos/{id}/retirar: o entregador confirma que pegou o pedido na loja.
 */
public record RetirarPedidoRequest(@NotBlank String entregadorId) {
}
