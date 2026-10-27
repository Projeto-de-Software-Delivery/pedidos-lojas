package br.insper.delivery.pedido.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.insper.delivery.cliente.service.ClienteService;
import br.insper.delivery.entregador.EntregadorClient;
import br.insper.delivery.entregador.EntregadorDados;
import br.insper.delivery.loja.service.LojaService;
import br.insper.delivery.pedido.domain.ItemPedido;
import br.insper.delivery.pedido.domain.Pedido;
import br.insper.delivery.pedido.domain.PedidoStatus;
import br.insper.delivery.pedido.dto.AceitarPedidoRequest;
import br.insper.delivery.pedido.dto.CriarPedidoRequest;
import br.insper.delivery.pedido.dto.EnderecoEntregaRequest;
import br.insper.delivery.pedido.dto.EnderecoEntregaResponse;
import br.insper.delivery.pedido.dto.EntregaAceitaDados;
import br.insper.delivery.pedido.dto.ItemPedidoResponse;
import br.insper.delivery.pedido.dto.PedidoEntregueDados;
import br.insper.delivery.pedido.dto.PedidoResponse;
import br.insper.delivery.pedido.dto.PedidoRetiradoDados;
import br.insper.delivery.pedido.dto.PedidoValidadoDados;
import br.insper.delivery.pedido.event.EntregaAceitaEvent;
import br.insper.delivery.pedido.event.EntregaAceitaEvento;
import br.insper.delivery.pedido.event.PedidoCriadoEvent;
import br.insper.delivery.pedido.event.PedidoCriadoEvento;
import br.insper.delivery.pedido.event.PedidoEntregueEvent;
import br.insper.delivery.pedido.event.PedidoEntregueEvento;
import br.insper.delivery.pedido.event.PedidoRetiradoEvent;
import br.insper.delivery.pedido.event.PedidoRetiradoEvento;
import br.insper.delivery.pedido.event.PedidoValidadoEvent;
import br.insper.delivery.pedido.event.PedidoValidadoEvento;
import br.insper.delivery.pedido.repository.ItemPedidoRepository;
import br.insper.delivery.pedido.repository.PedidoRepository;
import br.insper.delivery.produto.domain.Produto;
import br.insper.delivery.produto.service.ProdutoService;

/**
 * Serviço de pedidos: criação e consulta.
 */
@Service
public class PedidoService {

	private static final Logger log = LoggerFactory.getLogger(PedidoService.class);
	// ponytail: sem integracao com mapas (fora do escopo do servico de entregador); ETA fixo ate
	// que exista um calculo real de distancia/trafego.
	private static final int ETA_RETIRADA_PADRAO_MIN = 15;
	private static final int ETA_ENTREGA_PADRAO_MIN = 20;
	private static final String ENTREGADOR_STATUS_EM_ENTREGA = "EM_ENTREGA";
	private static final String ENTREGADOR_STATUS_DISPONIVEL = "DISPONIVEL";

	private final PedidoRepository pedidoRepository;
	private final ItemPedidoRepository itemPedidoRepository;
	private final ClienteService clienteService;
	private final LojaService lojaService;
	private final ProdutoService produtoService;
	private final EntregadorClient entregadorClient;
	private final ApplicationEventPublisher eventPublisher;

	public PedidoService(PedidoRepository pedidoRepository, ItemPedidoRepository itemPedidoRepository,
			ClienteService clienteService, LojaService lojaService, ProdutoService produtoService,
			EntregadorClient entregadorClient, ApplicationEventPublisher eventPublisher) {
		this.pedidoRepository = pedidoRepository;
		this.itemPedidoRepository = itemPedidoRepository;
		this.clienteService = clienteService;
		this.lojaService = lojaService;
		this.produtoService = produtoService;
		this.entregadorClient = entregadorClient;
		this.eventPublisher = eventPublisher;
	}

	/** Status inicial AGUARDANDO_VALIDACAO; publica pedido.criado. */
	public PedidoResponse criar(Long clienteId, CriarPedidoRequest request) {
		clienteService.buscarPorId(clienteId);
		lojaService.buscarPorId(request.lojaId());

		record ItemResolvido(Long produtoId, Integer quantidade, BigDecimal precoUnitario) {
		}

		List<ItemResolvido> resolvidos = request.itens().stream()
				.map(itemRequest -> {
					Produto produto = produtoService.buscarPorId(itemRequest.produtoId());
					if (!produto.getLojaId().equals(request.lojaId())) {
						throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
								"Produto " + produto.getId() + " não pertence à loja informada");
					}
					if (produto.getEstoque() < itemRequest.quantidade()) {
						throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
								"Estoque insuficiente para o produto " + produto.getId() + ": disponível="
										+ produto.getEstoque() + ", solicitado=" + itemRequest.quantidade());
					}
					return new ItemResolvido(itemRequest.produtoId(), itemRequest.quantidade(), produto.getPreco());
				})
				.toList();

		BigDecimal total = resolvidos.stream()
				.map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		Pedido pedido = new Pedido(clienteId, request.lojaId(), total, request.enderecoEntrega().rua(),
				request.enderecoEntrega().lat(), request.enderecoEntrega().lng());
		Pedido pedidoSalvo = pedidoRepository.save(pedido);

		List<ItemPedido> itensSalvos = resolvidos.stream()
				.map(item -> itemPedidoRepository
						.save(new ItemPedido(pedidoSalvo.getId(), item.produtoId(), item.quantidade(),
								item.precoUnitario())))
				.toList();

		PedidoCriadoEvento evento = PedidoCriadoEvento.de(pedidoSalvo, itensSalvos);
		eventPublisher.publishEvent(new PedidoCriadoEvent(this, evento));

		return paraResponse(pedidoSalvo, itensSalvos);
	}

	public PedidoResponse buscarPorId(Long id) {
		Pedido pedido = buscarEntidade(id);
		return paraResponse(pedido, itemPedidoRepository.findByPedidoId(pedido.getId()));
	}

	public PedidoStatus buscarStatus(Long id) {
		return buscarEntidade(id).getStatus();
	}

	public PedidoResponse aplicarPedidoValidado(PedidoValidadoDados dados) {
		return aplicarTransicao(dados.pedidoId(), PedidoStatus.AGUARDANDO_VALIDACAO, PedidoStatus.VALIDADO);
	}

	/**
	 * Fallback para quando a atribuição automática na aceitação não encontrou entregador: aplica a
	 * atribuição vinda do payload (quem chama já escolheu o entregador).
	 */
	public PedidoResponse aplicarEntregaAceita(EntregaAceitaDados dados) {
		Pedido pedido = buscarEntidade(parseId(dados.pedidoId()));
		try {
			pedido.transicionar(PedidoStatus.VALIDADO, PedidoStatus.ENTREGA_ACEITA);
		} catch (IllegalStateException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
		}
		pedido.atribuirEntregador(dados.entregadorId(), dados.entregaId());
		Pedido salvo = pedidoRepository.save(pedido);
		log.info("Pedido {} transicionou de VALIDADO para ENTREGA_ACEITA (entregador {})", salvo.getId(),
				dados.entregadorId());
		return paraResponse(salvo, itemPedidoRepository.findByPedidoId(salvo.getId()));
	}

	public PedidoResponse aplicarPedidoRetirado(PedidoRetiradoDados dados) {
		return aplicarTransicao(dados.pedidoId(), PedidoStatus.ENTREGA_ACEITA, PedidoStatus.EM_ENTREGA);
	}

	/** App do entregador: só o entregador atribuído pode retirar; publica pedido.retirado. */
	public PedidoResponse retirar(Long pedidoId, String entregadorId) {
		Pedido pedido = buscarEntidadeDoEntregador(pedidoId, entregadorId);
		try {
			pedido.transicionar(PedidoStatus.ENTREGA_ACEITA, PedidoStatus.EM_ENTREGA);
		} catch (IllegalStateException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
		}
		Pedido salvo = pedidoRepository.save(pedido);
		log.info("Pedido {} retirado pelo entregador {} e transicionou para EM_ENTREGA", salvo.getId(),
				entregadorId);

		PedidoRetiradoDados dados = new PedidoRetiradoDados(String.valueOf(salvo.getId()), salvo.getEntregaId(),
				entregadorId, Instant.now(), ETA_ENTREGA_PADRAO_MIN, String.valueOf(salvo.getClienteId()),
				String.valueOf(salvo.getLojaId()));
		eventPublisher.publishEvent(new PedidoRetiradoEvent(this, PedidoRetiradoEvento.de(dados)));

		return paraResponse(salvo, itemPedidoRepository.findByPedidoId(salvo.getId()));
	}

	/** Libera o entregador (volta a DISPONIVEL) depois de concluída a entrega. */
	public PedidoResponse aplicarPedidoEntregue(PedidoEntregueDados dados) {
		PedidoResponse response = aplicarTransicao(dados.pedidoId(), PedidoStatus.EM_ENTREGA, PedidoStatus.ENTREGUE);
		if (response.entregadorId() != null) {
			entregadorClient.atualizarStatus(response.entregadorId(), ENTREGADOR_STATUS_DISPONIVEL);
		}
		return response;
	}

	/**
	 * App do entregador: finaliza a corrida se o PIN bater com o do pedido, libera o entregador e
	 * publica pedido.entregue. PIN errado não altera nada (422).
	 */
	public PedidoResponse entregar(Long pedidoId, String entregadorId, String pin) {
		Pedido pedido = buscarEntidadeDoEntregador(pedidoId, entregadorId);
		if (pedido.getStatus() != PedidoStatus.EM_ENTREGA) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Pedido está em " + pedido.getStatus() + ", esperado EM_ENTREGA para entregar");
		}
		if (!pedido.getPin().equals(pin)) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Código de entrega incorreto");
		}
		pedido.transicionar(PedidoStatus.EM_ENTREGA, PedidoStatus.ENTREGUE);
		Pedido salvo = pedidoRepository.save(pedido);
		log.info("Pedido {} entregue pelo entregador {} com PIN validado", salvo.getId(), entregadorId);
		entregadorClient.atualizarStatus(entregadorId, ENTREGADOR_STATUS_DISPONIVEL);

		PedidoEntregueDados dados = new PedidoEntregueDados(String.valueOf(salvo.getId()), salvo.getEntregaId(),
				entregadorId, Instant.now(), true, String.valueOf(salvo.getClienteId()),
				String.valueOf(salvo.getLojaId()));
		eventPublisher.publishEvent(new PedidoEntregueEvent(this, PedidoEntregueEvento.de(dados)));

		return paraResponse(salvo, itemPedidoRepository.findByPedidoId(salvo.getId()));
	}

	public List<PedidoResponse> listarPorCliente(Long clienteId) {
		clienteService.buscarPorId(clienteId);
		return pedidoRepository.findByClienteId(clienteId).stream()
				.map(pedido -> paraResponse(pedido, itemPedidoRepository.findByPedidoId(pedido.getId())))
				.toList();
	}

	/** App do entregador: corridas atribuídas a ele, em qualquer status. */
	public List<PedidoResponse> listarPorEntregador(String entregadorId) {
		return pedidoRepository.findByEntregadorId(entregadorId).stream()
				.map(pedido -> paraResponse(pedido, itemPedidoRepository.findByPedidoId(pedido.getId())))
				.toList();
	}

	public List<PedidoResponse> listarPendentesPorLoja(Long lojaId) {
		lojaService.buscarPorId(lojaId);
		return pedidoRepository.findByLojaIdAndStatus(lojaId, PedidoStatus.AGUARDANDO_VALIDACAO).stream()
				.map(pedido -> paraResponse(pedido, itemPedidoRepository.findByPedidoId(pedido.getId())))
				.toList();
	}

	/**
	 * Tudo-ou-nada: se a baixa de estoque falhar, nada é persistido (nem o estoque, nem a
	 * transição pra VALIDADO). Depois de validado, tenta atribuir um entregador disponível na
	 * hora; se nenhum estiver livre, o pedido fica em VALIDADO aguardando uma nova tentativa.
	 */
	@Transactional
	public PedidoResponse aceitar(Long lojaId, Long pedidoId, AceitarPedidoRequest request) {
		lojaService.buscarPorId(lojaId);
		Pedido pedido = buscarEntidadeDaLoja(lojaId, pedidoId);
		if (pedido.getStatus() != PedidoStatus.AGUARDANDO_VALIDACAO) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Pedido está em " + pedido.getStatus() + ", esperado AGUARDANDO_VALIDACAO para aceitar");
		}

		List<ItemPedido> itens = itemPedidoRepository.findByPedidoId(pedido.getId());
		Map<Long, Integer> quantidadesPorProduto = itens.stream()
				.collect(Collectors.toMap(ItemPedido::getProdutoId, ItemPedido::getQuantidade, Integer::sum));
		produtoService.baixarEstoque(quantidadesPorProduto);

		pedido.transicionar(PedidoStatus.AGUARDANDO_VALIDACAO, PedidoStatus.VALIDADO);
		atribuirEntregadorDisponivel(pedido);
		Pedido salvo = pedidoRepository.save(pedido);
		log.info("Pedido {} aceito pela loja {} e transicionou para {}", salvo.getId(), lojaId, salvo.getStatus());

		EnderecoEntregaRequest enderecoEntrega = new EnderecoEntregaRequest(salvo.getEnderecoRua(),
				salvo.getEnderecoLat(), salvo.getEnderecoLng());
		PedidoValidadoDados dados = new PedidoValidadoDados(String.valueOf(salvo.getId()), String.valueOf(lojaId),
				request.enderecoRetirada(), enderecoEntrega, request.valorFrete(), request.tempoPreparoMin());
		eventPublisher.publishEvent(new PedidoValidadoEvent(this, PedidoValidadoEvento.de(dados)));

		return paraResponse(salvo, itens);
	}

	/**
	 * Serviço de entregador só tem CRUD + status (sem fila nem endpoint de oferta), então a
	 * "oferta de corrida" vira uma atribuição direta: pega o primeiro DISPONIVEL e marca ocupado.
	 */
	private void atribuirEntregadorDisponivel(Pedido pedido) {
		Optional<EntregadorDados> entregador = entregadorClient.buscarDisponivel();
		if (entregador.isEmpty()) {
			log.warn("Nenhum entregador disponível para o pedido {} no momento da aceitação", pedido.getId());
			return;
		}

		EntregadorDados escolhido = entregador.get();
		entregadorClient.atualizarStatus(escolhido.id(), ENTREGADOR_STATUS_EM_ENTREGA);
		String entregaId = UUID.randomUUID().toString();
		pedido.atribuirEntregador(escolhido.id(), entregaId);
		pedido.transicionar(PedidoStatus.VALIDADO, PedidoStatus.ENTREGA_ACEITA);
		log.info("Pedido {} atribuído ao entregador {}", pedido.getId(), escolhido.id());

		// payloads.md exemplifica veiculo em minusculo; o servico de entregador usa maiusculo.
		EntregaAceitaDados dados = new EntregaAceitaDados(String.valueOf(pedido.getId()),
				entregaId, escolhido.id(), escolhido.nome(),
				escolhido.veiculo().toLowerCase(), ETA_RETIRADA_PADRAO_MIN);
		eventPublisher.publishEvent(new EntregaAceitaEvent(this, EntregaAceitaEvento.de(dados)));
	}

	/** Nenhum estoque é alterado — ele só é baixado na aceitação, nunca antes. */
	public PedidoResponse recusar(Long lojaId, Long pedidoId) {
		lojaService.buscarPorId(lojaId);
		Pedido pedido = buscarEntidadeDaLoja(lojaId, pedidoId);
		try {
			pedido.transicionar(PedidoStatus.AGUARDANDO_VALIDACAO, PedidoStatus.CANCELADO);
		} catch (IllegalStateException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
		}
		Pedido salvo = pedidoRepository.save(pedido);
		log.info("Pedido {} recusado pela loja {} e transicionou para CANCELADO", salvo.getId(), lojaId);
		return paraResponse(salvo, itemPedidoRepository.findByPedidoId(salvo.getId()));
	}

	private Pedido buscarEntidadeDaLoja(Long lojaId, Long pedidoId) {
		Pedido pedido = buscarEntidade(pedidoId);
		if (!pedido.getLojaId().equals(lojaId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado para esta loja");
		}
		return pedido;
	}

	private Pedido buscarEntidadeDoEntregador(Long pedidoId, String entregadorId) {
		Pedido pedido = buscarEntidade(pedidoId);
		if (!entregadorId.equals(pedido.getEntregadorId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Pedido não está atribuído a este entregador");
		}
		return pedido;
	}

	private Pedido buscarEntidade(Long id) {
		return pedidoRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));
	}

	private PedidoResponse aplicarTransicao(String pedidoIdTexto, PedidoStatus statusEsperado,
			PedidoStatus novoStatus) {
		Pedido pedido = buscarEntidade(parseId(pedidoIdTexto));
		try {
			pedido.transicionar(statusEsperado, novoStatus);
		} catch (IllegalStateException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
		}
		Pedido salvo = pedidoRepository.save(pedido);
		log.info("Pedido {} transicionou de {} para {}", salvo.getId(), statusEsperado, novoStatus);
		return paraResponse(salvo, itemPedidoRepository.findByPedidoId(salvo.getId()));
	}

	private Long parseId(String texto) {
		try {
			return Long.valueOf(texto);
		} catch (NumberFormatException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pedidoId inválido: " + texto);
		}
	}

	private PedidoResponse paraResponse(Pedido pedido, List<ItemPedido> itens) {
		List<ItemPedidoResponse> itensResponse = itens.stream().map(ItemPedidoResponse::from).toList();
		EnderecoEntregaResponse enderecoEntrega = new EnderecoEntregaResponse(pedido.getEnderecoRua(),
				pedido.getEnderecoLat(), pedido.getEnderecoLng());
		return new PedidoResponse(pedido.getId(), pedido.getClienteId(), pedido.getLojaId(), itensResponse,
				pedido.getTotal(), enderecoEntrega, pedido.getStatus(), pedido.getDataCriacao(), pedido.getPin(),
				pedido.getEntregadorId());
	}
}
