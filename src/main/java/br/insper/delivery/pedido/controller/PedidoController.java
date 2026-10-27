package br.insper.delivery.pedido.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.insper.delivery.pedido.dto.AceitarPedidoRequest;
import br.insper.delivery.pedido.dto.CriarPedidoRequest;
import br.insper.delivery.pedido.dto.EntregarPedidoRequest;
import br.insper.delivery.pedido.dto.PedidoResponse;
import br.insper.delivery.pedido.dto.PedidoStatusResponse;
import br.insper.delivery.pedido.dto.RetirarPedidoRequest;
import br.insper.delivery.pedido.service.PedidoService;
import jakarta.validation.Valid;

/**
 * Controlador de pedidos.
 */
@RestController
public class PedidoController {

	private final PedidoService pedidoService;

	public PedidoController(PedidoService pedidoService) {
		this.pedidoService = pedidoService;
	}

	@PostMapping("/clientes/{clienteId}/pedidos")
	public ResponseEntity<PedidoResponse> criar(@PathVariable Long clienteId,
			@Valid @RequestBody CriarPedidoRequest request) {
		PedidoResponse response = pedidoService.criar(clienteId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/pedidos/{id}")
	public ResponseEntity<PedidoResponse> buscarPorId(@PathVariable Long id) {
		return ResponseEntity.ok(pedidoService.buscarPorId(id));
	}

	@GetMapping("/pedidos/{id}/status")
	public ResponseEntity<PedidoStatusResponse> buscarStatus(@PathVariable Long id) {
		return ResponseEntity.ok(new PedidoStatusResponse(id, pedidoService.buscarStatus(id)));
	}

	/** App do entregador: corridas atribuídas a ele. */
	@GetMapping("/pedidos")
	public ResponseEntity<List<PedidoResponse>> listarPorEntregador(@RequestParam String entregadorId) {
		return ResponseEntity.ok(pedidoService.listarPorEntregador(entregadorId));
	}

	/** Entregador pegou o pedido na loja: transiciona pra EM_ENTREGA e publica pedido.retirado. */
	@PostMapping("/pedidos/{id}/retirar")
	public ResponseEntity<PedidoResponse> retirar(@PathVariable Long id,
			@Valid @RequestBody RetirarPedidoRequest request) {
		return ResponseEntity.ok(pedidoService.retirar(id, request.entregadorId()));
	}

	/** Valida o PIN, transiciona pra ENTREGUE, libera o entregador e publica pedido.entregue. */
	@PostMapping("/pedidos/{id}/entregar")
	public ResponseEntity<PedidoResponse> entregar(@PathVariable Long id,
			@Valid @RequestBody EntregarPedidoRequest request) {
		return ResponseEntity.ok(pedidoService.entregar(id, request.entregadorId(), request.pin()));
	}

	@GetMapping("/clientes/{clienteId}/pedidos")
	public ResponseEntity<List<PedidoResponse>> listarPorCliente(@PathVariable Long clienteId) {
		return ResponseEntity.ok(pedidoService.listarPorCliente(clienteId));
	}

	/** Painel da loja: pedidos ainda aguardando validação. */
	@GetMapping("/lojas/{lojaId}/pedidos/pendentes")
	public ResponseEntity<List<PedidoResponse>> listarPendentesPorLoja(@PathVariable Long lojaId) {
		return ResponseEntity.ok(pedidoService.listarPendentesPorLoja(lojaId));
	}

	/** Baixa o estoque dos itens, transiciona pra VALIDADO e publica pedido.validado. */
	@PostMapping("/lojas/{lojaId}/pedidos/{pedidoId}/aceitar")
	public ResponseEntity<PedidoResponse> aceitar(@PathVariable Long lojaId, @PathVariable Long pedidoId,
			@Valid @RequestBody AceitarPedidoRequest request) {
		return ResponseEntity.ok(pedidoService.aceitar(lojaId, pedidoId, request));
	}

	@PostMapping("/lojas/{lojaId}/pedidos/{pedidoId}/recusar")
	public ResponseEntity<PedidoResponse> recusar(@PathVariable Long lojaId, @PathVariable Long pedidoId) {
		return ResponseEntity.ok(pedidoService.recusar(lojaId, pedidoId));
	}
}
