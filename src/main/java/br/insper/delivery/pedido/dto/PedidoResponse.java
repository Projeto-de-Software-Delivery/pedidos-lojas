package br.insper.delivery.pedido.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import br.insper.delivery.pedido.domain.PedidoStatus;

// ponytail: pin e codigoRetirada expostos pra qualquer chamador (loja/entregador inclusive), pois nao ha
// autorizacao por papel ainda (KAN-17). Quando ela existir, restringir pin ao cliente dono e
// codigoRetirada a loja.
public record PedidoResponse(Long id, Long clienteId, Long lojaId, List<ItemPedidoResponse> itens, BigDecimal total,
		EnderecoEntregaResponse enderecoEntrega, PedidoStatus status, Instant dataCriacao, String pin,
		String entregadorId, String codigoRetirada, boolean entregaConfirmadaPeloEntregador,
		boolean recebimentoConfirmadoPeloCliente, Instant prazoAceite, Instant prazoEntrega) {
}
