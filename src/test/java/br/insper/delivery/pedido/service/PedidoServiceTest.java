package br.insper.delivery.pedido.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import br.insper.delivery.cliente.domain.Cliente;
import br.insper.delivery.cliente.service.ClienteService;
import br.insper.delivery.entregador.EntregadorClient;
import br.insper.delivery.entregador.EntregadorDados;
import br.insper.delivery.loja.domain.Loja;
import br.insper.delivery.loja.service.LojaService;
import br.insper.delivery.pedido.domain.ItemPedido;
import br.insper.delivery.pedido.domain.Pedido;
import br.insper.delivery.pedido.domain.PedidoStatus;
import br.insper.delivery.pedido.dto.AceitarPedidoRequest;
import br.insper.delivery.pedido.dto.CriarPedidoRequest;
import br.insper.delivery.pedido.dto.EnderecoEntregaRequest;
import br.insper.delivery.pedido.dto.EntregaAceitaDados;
import br.insper.delivery.pedido.dto.ItemPedidoRequest;
import br.insper.delivery.pedido.dto.PedidoEntregueDados;
import br.insper.delivery.pedido.dto.PedidoResponse;
import br.insper.delivery.pedido.dto.PedidoRetiradoDados;
import br.insper.delivery.pedido.dto.PedidoValidadoDados;
import br.insper.delivery.pedido.event.EntregaAceitaEvent;
import br.insper.delivery.pedido.event.PedidoCriadoEvent;
import br.insper.delivery.pedido.event.PedidoEntregueEvent;
import br.insper.delivery.pedido.event.PedidoRetiradoEvent;
import br.insper.delivery.pedido.event.PedidoValidadoEvent;
import br.insper.delivery.pedido.repository.ItemPedidoRepository;
import br.insper.delivery.pedido.repository.PedidoRepository;
import br.insper.delivery.produto.domain.Produto;
import br.insper.delivery.produto.service.ProdutoService;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

	@Mock
	private PedidoRepository pedidoRepository;

	@Mock
	private ItemPedidoRepository itemPedidoRepository;

	@Mock
	private ClienteService clienteService;

	@Mock
	private LojaService lojaService;

	@Mock
	private ProdutoService produtoService;

	@Mock
	private EntregadorClient entregadorClient;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private PedidoService pedidoService;

	private static final Cliente CLIENTE = new Cliente("Ana", "ana@email.com", "11999999999");
	private static final Loja LOJA = new Loja("Padaria", "12345678000199", "Rua A, 1");

	private CriarPedidoRequest requestPadrao() {
		return new CriarPedidoRequest(2L, List.of(new ItemPedidoRequest(9L, 2)),
				new EnderecoEntregaRequest("Rua B, 2", -23.5, -46.6));
	}

	@Test
	void criarDevePersistirComStatusAguardandoValidacaoEPublicarEvento() {
		when(clienteService.buscarPorId(1L)).thenReturn(CLIENTE);
		when(lojaService.buscarPorId(2L)).thenReturn(LOJA);
		Produto produto = new Produto(2L, "Bolo", "Sobremesas", new BigDecimal("15.90"), 10, "foto.png");
		when(produtoService.buscarPorId(9L)).thenReturn(produto);
		Pedido salvo = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		when(pedidoRepository.save(any(Pedido.class))).thenReturn(salvo);
		ItemPedido itemSalvo = new ItemPedido(salvo.getId(), 9L, 2, new BigDecimal("15.90"));
		when(itemPedidoRepository.save(any(ItemPedido.class))).thenReturn(itemSalvo);

		PedidoResponse response = pedidoService.criar(1L, requestPadrao());

		assertThat(response.status()).isEqualTo(PedidoStatus.AGUARDANDO_VALIDACAO);
		assertThat(response.total()).isEqualByComparingTo("31.80");
		assertThat(response.itens()).hasSize(1);
		assertThat(response.enderecoEntrega().rua()).isEqualTo("Rua B, 2");
		assertThat(response.pin()).matches("\\d{4}");
		verify(eventPublisher).publishEvent(any(PedidoCriadoEvent.class));
	}

	@Test
	void criarDeveDefinirPrazoDeAceiteComOTempoDaLoja() {
		Loja loja = new Loja("Padaria", "12345678000199", "Rua A, 1");
		loja.definirPrazos(5, 30);
		when(clienteService.buscarPorId(1L)).thenReturn(CLIENTE);
		when(lojaService.buscarPorId(2L)).thenReturn(loja);
		Produto produto = new Produto(2L, "Bolo", "Sobremesas", new BigDecimal("15.90"), 10, "foto.png");
		when(produtoService.buscarPorId(9L)).thenReturn(produto);
		ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
		when(pedidoRepository.save(captor.capture())).thenAnswer(invocacao -> invocacao.getArgument(0));
		when(itemPedidoRepository.save(any(ItemPedido.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		pedidoService.criar(1L, requestPadrao());

		Pedido salvo = captor.getValue();
		assertThat(salvo.getPrazoAceite()).isEqualTo(salvo.getDataCriacao().plus(5, ChronoUnit.MINUTES));
	}

	@Test
	void criarDeveGerarPinDiferenteACadaPedido() {
		when(clienteService.buscarPorId(1L)).thenReturn(CLIENTE);
		when(lojaService.buscarPorId(2L)).thenReturn(LOJA);
		Produto produto = new Produto(2L, "Bolo", "Sobremesas", new BigDecimal("15.90"), 10, "foto.png");
		when(produtoService.buscarPorId(9L)).thenReturn(produto);
		when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(itemPedidoRepository.save(any(ItemPedido.class)))
				.thenReturn(new ItemPedido(null, 9L, 2, new BigDecimal("15.90")));

		List<String> pins = java.util.stream.IntStream.range(0, 20)
				.mapToObj(i -> pedidoService.criar(1L, requestPadrao()).pin())
				.toList();

		assertThat(pins).allMatch(pin -> pin.matches("\\d{4}"));
		assertThat(pins.stream().distinct().count()).as("pelo menos algum pin diferente em 20 geracoes")
				.isGreaterThan(1);
	}

	@Test
	void criarDeveLancarQuandoClienteNaoEncontrado() {
		when(clienteService.buscarPorId(1L))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));

		org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> pedidoService.criar(1L, requestPadrao()));
	}

	@Test
	void criarDeveLancarQuandoLojaNaoEncontrada() {
		when(clienteService.buscarPorId(1L)).thenReturn(CLIENTE);
		when(lojaService.buscarPorId(2L))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Loja não encontrada"));

		org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> pedidoService.criar(1L, requestPadrao()));
	}

	@Test
	void criarDeveLancarQuandoProdutoNaoEncontrado() {
		when(clienteService.buscarPorId(1L)).thenReturn(CLIENTE);
		when(lojaService.buscarPorId(2L)).thenReturn(LOJA);
		when(produtoService.buscarPorId(9L))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));

		org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> pedidoService.criar(1L, requestPadrao()));
	}

	@Test
	void criarDeveLancarBadRequestQuandoProdutoNaoPertenceALoja() {
		when(clienteService.buscarPorId(1L)).thenReturn(CLIENTE);
		when(lojaService.buscarPorId(2L)).thenReturn(LOJA);
		Produto produtoDeOutraLoja = new Produto(99L, "Bolo", "Sobremesas", new BigDecimal("15.90"), 10, "foto.png");
		when(produtoService.buscarPorId(9L)).thenReturn(produtoDeOutraLoja);

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.criar(1L, requestPadrao()));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void criarDeveLancarUnprocessableEntityQuandoEstoqueInsuficiente() {
		when(clienteService.buscarPorId(1L)).thenReturn(CLIENTE);
		when(lojaService.buscarPorId(2L)).thenReturn(LOJA);
		// requestPadrao pede quantidade 2, mas estoque só tem 1
		Produto produto = new Produto(2L, "Bolo", "Sobremesas", new BigDecimal("15.90"), 1, "foto.png");
		when(produtoService.buscarPorId(9L)).thenReturn(produto);

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.criar(1L, requestPadrao()));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
	}

	@Test
	void buscarPorIdDeveRetornarPedidoComItens() {
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		ItemPedido item = new ItemPedido(1L, 9L, 2, new BigDecimal("15.90"));
		when(itemPedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of(item));

		PedidoResponse resultado = pedidoService.buscarPorId(1L);

		assertThat(resultado.status()).isEqualTo(PedidoStatus.AGUARDANDO_VALIDACAO);
		assertThat(resultado.itens()).hasSize(1);
	}

	@Test
	void buscarPorIdDeveLancarQuandoNaoEncontrado() {
		when(pedidoRepository.findById(1L)).thenReturn(Optional.empty());

		org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> pedidoService.buscarPorId(1L));
	}

	@Test
	void buscarStatusDeveRetornarStatusDoPedido() {
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

		PedidoStatus status = pedidoService.buscarStatus(1L);

		assertThat(status).isEqualTo(PedidoStatus.AGUARDANDO_VALIDACAO);
	}

	@Test
	void listarPorClienteDeveRetornarPedidosDoRepositorio() {
		when(clienteService.buscarPorId(1L)).thenReturn(CLIENTE);
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		when(pedidoRepository.findByClienteId(1L)).thenReturn(List.of(pedido));
		when(itemPedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of());

		List<PedidoResponse> resultado = pedidoService.listarPorCliente(1L);

		assertThat(resultado).hasSize(1);
	}

	@Test
	void listarPorClienteDeveLancarQuandoClienteNaoEncontrado() {
		when(clienteService.buscarPorId(1L))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));

		org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> pedidoService.listarPorCliente(1L));
	}

	@Test
	void listarPendentesPorLojaDeveRetornarApenasAguardandoValidacao() {
		when(lojaService.buscarPorId(2L)).thenReturn(LOJA);
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		when(pedidoRepository.findByLojaIdAndStatus(2L, PedidoStatus.AGUARDANDO_VALIDACAO)).thenReturn(List.of(pedido));
		when(itemPedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of());

		List<PedidoResponse> resultado = pedidoService.listarPendentesPorLoja(2L);

		assertThat(resultado).hasSize(1);
	}

	@Test
	void listarPendentesPorLojaDeveLancarQuandoLojaNaoEncontrada() {
		when(lojaService.buscarPorId(2L))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Loja não encontrada"));

		org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> pedidoService.listarPendentesPorLoja(2L));
	}

	@Test
	void aplicarPedidoValidadoDeveTransicionarDeAguardandoValidacaoParaValidado() {
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		when(pedidoRepository.save(pedido)).thenReturn(pedido);
		when(itemPedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of());
		PedidoValidadoDados dados = new PedidoValidadoDados("1", "2", new EnderecoEntregaRequest("Rua A, 1", -23.56, -46.65),
				new EnderecoEntregaRequest("Rua B, 2", -23.5, -46.6), new BigDecimal("8.00"), 20);

		PedidoResponse response = pedidoService.aplicarPedidoValidado(dados);

		assertThat(response.status()).isEqualTo(PedidoStatus.VALIDADO);
	}

	@Test
	void aplicarPedidoValidadoDeveLancarConflitoQuandoPedidoNaoEstaAguardandoValidacao() {
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		pedido.transicionar(PedidoStatus.AGUARDANDO_VALIDACAO, PedidoStatus.VALIDADO);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		PedidoValidadoDados dados = new PedidoValidadoDados("1", "2", new EnderecoEntregaRequest("Rua A, 1", -23.56, -46.65),
				new EnderecoEntregaRequest("Rua B, 2", -23.5, -46.6), new BigDecimal("8.00"), 20);

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.aplicarPedidoValidado(dados));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void aplicarPedidoValidadoDeveLancarQuandoPedidoNaoEncontrado() {
		when(pedidoRepository.findById(1L)).thenReturn(Optional.empty());
		PedidoValidadoDados dados = new PedidoValidadoDados("1", "2", new EnderecoEntregaRequest("Rua A, 1", -23.56, -46.65),
				new EnderecoEntregaRequest("Rua B, 2", -23.5, -46.6), new BigDecimal("8.00"), 20);

		org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> pedidoService.aplicarPedidoValidado(dados));
	}

	@Test
	void aplicarEntregaAceitaDeveTransicionarDeValidadoParaEntregaAceita() {
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		pedido.transicionar(PedidoStatus.AGUARDANDO_VALIDACAO, PedidoStatus.VALIDADO);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		when(pedidoRepository.save(pedido)).thenReturn(pedido);
		when(itemPedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of());
		EntregaAceitaDados dados = new EntregaAceitaDados("1", "88", "31", "Joao", "moto", 12);

		PedidoResponse response = pedidoService.aplicarEntregaAceita(dados);

		assertThat(response.status()).isEqualTo(PedidoStatus.ENTREGA_ACEITA);
		assertThat(response.entregadorId()).isEqualTo("31");
	}

	@Test
	void aplicarEntregaAceitaDeveLancarConflitoQuandoPedidoNaoEstaValidado() {
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		EntregaAceitaDados dados = new EntregaAceitaDados("1", "88", "31", "Joao", "moto", 12);

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.aplicarEntregaAceita(dados));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void aplicarPedidoRetiradoDeveTransicionarDeEntregaAceitaParaEmEntrega() {
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		pedido.transicionar(PedidoStatus.AGUARDANDO_VALIDACAO, PedidoStatus.VALIDADO);
		pedido.transicionar(PedidoStatus.VALIDADO, PedidoStatus.ENTREGA_ACEITA);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		when(pedidoRepository.save(pedido)).thenReturn(pedido);
		when(itemPedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of());
		PedidoRetiradoDados dados = new PedidoRetiradoDados("1", "88", "31", Instant.now(), 18, null, null);

		PedidoResponse response = pedidoService.aplicarPedidoRetirado(dados);

		assertThat(response.status()).isEqualTo(PedidoStatus.EM_ENTREGA);
	}

	@Test
	void aplicarPedidoRetiradoDeveLancarConflitoQuandoPedidoNaoEstaEntregaAceita() {
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		PedidoRetiradoDados dados = new PedidoRetiradoDados("1", "88", "31", Instant.now(), 18, null, null);

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.aplicarPedidoRetirado(dados));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	private Pedido pedidoComEntregaAceita() {
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		pedido.transicionar(PedidoStatus.AGUARDANDO_VALIDACAO, PedidoStatus.VALIDADO);
		pedido.atribuirEntregador("31", "88");
		pedido.transicionar(PedidoStatus.VALIDADO, PedidoStatus.ENTREGA_ACEITA);
		return pedido;
	}

	@Test
	void retirarDeveTransicionarParaEmEntregaEPublicarPedidoRetirado() {
		Pedido pedido = pedidoComEntregaAceita();
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		when(pedidoRepository.save(pedido)).thenReturn(pedido);
		when(itemPedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of());

		PedidoResponse response = pedidoService.retirar(1L, "31", pedido.getCodigoRetirada());

		assertThat(response.status()).isEqualTo(PedidoStatus.EM_ENTREGA);
		ArgumentCaptor<PedidoRetiradoEvent> captor = ArgumentCaptor.forClass(PedidoRetiradoEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		PedidoRetiradoDados dados = captor.getValue().getEvento().data();
		assertThat(captor.getValue().getEvento().eventType()).isEqualTo("pedido.retirado");
		assertThat(dados.entregaId()).isEqualTo("88");
		assertThat(dados.entregadorId()).isEqualTo("31");
		assertThat(dados.clienteId()).isEqualTo("1");
		assertThat(dados.lojaId()).isEqualTo("2");
	}

	@Test
	void retirarDeveLancarForbiddenQuandoPedidoEDeOutroEntregador() {
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoComEntregaAceita()));

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.retirar(1L, "99", "1234"));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		org.mockito.Mockito.verifyNoInteractions(eventPublisher);
	}

	@Test
	void retirarComCodigoErradoDeveLancarUnprocessableENaoAlterarNada() {
		Pedido pedido = pedidoComEntregaAceita();
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		String codigoErrado = pedido.getCodigoRetirada().equals("0000") ? "1111" : "0000";

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.retirar(1L, "31", codigoErrado));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
		assertThat(pedido.getStatus()).isEqualTo(PedidoStatus.ENTREGA_ACEITA);
		org.mockito.Mockito.verifyNoInteractions(eventPublisher);
	}

	@Test
	void retirarDeveLancarConflitoQuandoPedidoNaoEstaEntregaAceita() {
		Pedido pedido = pedidoComEntregaAceita();
		pedido.transicionar(PedidoStatus.ENTREGA_ACEITA, PedidoStatus.EM_ENTREGA);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.retirar(1L, "31", pedido.getCodigoRetirada()));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		org.mockito.Mockito.verifyNoInteractions(eventPublisher);
	}

	private Pedido pedidoEmEntrega() {
		Pedido pedido = pedidoComEntregaAceita();
		pedido.transicionar(PedidoStatus.ENTREGA_ACEITA, PedidoStatus.EM_ENTREGA);
		return pedido;
	}

	@Test
	void entregarComPinCorretoSemConfirmacaoDoClienteDeveEsperarOCliente() {
		Pedido pedido = pedidoEmEntrega();
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		when(pedidoRepository.save(pedido)).thenReturn(pedido);
		when(itemPedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of());

		PedidoResponse response = pedidoService.entregar(1L, "31", pedido.getPin());

		assertThat(response.status()).isEqualTo(PedidoStatus.EM_ENTREGA);
		assertThat(response.entregaConfirmadaPeloEntregador()).isTrue();
		assertThat(response.recebimentoConfirmadoPeloCliente()).isFalse();
		org.mockito.Mockito.verifyNoInteractions(entregadorClient, eventPublisher);
	}

	@Test
	void confirmarRecebimentoSemConfirmacaoDoEntregadorDeveEsperarOEntregador() {
		Pedido pedido = pedidoEmEntrega();
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		when(pedidoRepository.save(pedido)).thenReturn(pedido);
		when(itemPedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of());

		PedidoResponse response = pedidoService.confirmarRecebimento(1L, 1L);

		assertThat(response.status()).isEqualTo(PedidoStatus.EM_ENTREGA);
		assertThat(response.recebimentoConfirmadoPeloCliente()).isTrue();
		org.mockito.Mockito.verifyNoInteractions(entregadorClient, eventPublisher);
	}

	@Test
	void confirmarRecebimentoDeOutroClienteDeveLancarNotFound() {
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoEmEntrega()));

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.confirmarRecebimento(99L, 1L));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void confirmarRecebimentoForaDeEmEntregaDeveLancarConflito() {
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoComEntregaAceita()));

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.confirmarRecebimento(1L, 1L));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void clienteConfirmaDepoisDoEntregadorDeveFinalizar() {
		Pedido pedido = pedidoEmEntrega();
		pedido.confirmarEntregaPeloEntregador();
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		when(pedidoRepository.save(pedido)).thenReturn(pedido);
		when(itemPedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of());

		PedidoResponse response = pedidoService.confirmarRecebimento(1L, 1L);

		assertThat(response.status()).isEqualTo(PedidoStatus.ENTREGUE);
		verify(entregadorClient).atualizarStatus("31", "DISPONIVEL");
		verify(eventPublisher).publishEvent(any(PedidoEntregueEvent.class));
	}

	@Test
	void entregarComPinCorretoDepoisDoClienteDeveFinalizarLiberarEntregadorEPublicarPedidoEntregue() {
		Pedido pedido = pedidoEmEntrega();
		pedido.confirmarRecebimentoPeloCliente();
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		when(pedidoRepository.save(pedido)).thenReturn(pedido);
		when(itemPedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of());

		PedidoResponse response = pedidoService.entregar(1L, "31", pedido.getPin());

		assertThat(response.status()).isEqualTo(PedidoStatus.ENTREGUE);
		verify(entregadorClient).atualizarStatus("31", "DISPONIVEL");
		ArgumentCaptor<PedidoEntregueEvent> captor = ArgumentCaptor.forClass(PedidoEntregueEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		PedidoEntregueDados dados = captor.getValue().getEvento().data();
		assertThat(captor.getValue().getEvento().eventType()).isEqualTo("pedido.entregue");
		assertThat(dados.entregaId()).isEqualTo("88");
		assertThat(dados.pinValidado()).isTrue();
		assertThat(dados.clienteId()).isEqualTo("1");
		assertThat(dados.lojaId()).isEqualTo("2");
	}

	@Test
	void entregarComPinErradoDeveLancarUnprocessableENaoAlterarNada() {
		Pedido pedido = pedidoEmEntrega();
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		String pinErrado = pedido.getPin().equals("0000") ? "1111" : "0000";

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.entregar(1L, "31", pinErrado));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
		assertThat(pedido.getStatus()).isEqualTo(PedidoStatus.EM_ENTREGA);
		org.mockito.Mockito.verifyNoInteractions(entregadorClient, eventPublisher);
	}

	@Test
	void entregarDeveLancarConflitoQuandoPedidoNaoEstaEmEntrega() {
		Pedido pedido = pedidoComEntregaAceita();
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.entregar(1L, "31", pedido.getPin()));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void entregarDeveLancarForbiddenQuandoPedidoEDeOutroEntregador() {
		Pedido pedido = pedidoEmEntrega();
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.entregar(1L, "99", pedido.getPin()));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void listarPorLojaDeveRetornarTodosOsPedidosDaLoja() {
		when(lojaService.buscarPorId(2L)).thenReturn(LOJA);
		Pedido pedido = pedidoComEntregaAceita();
		when(pedidoRepository.findByLojaId(2L)).thenReturn(List.of(pedido));
		when(itemPedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of());

		List<PedidoResponse> response = pedidoService.listarPorLoja(2L);

		assertThat(response).hasSize(1);
		assertThat(response.get(0).codigoRetirada()).matches("\\d{4}");
	}

	@Test
	void listarPorEntregadorDeveRetornarPedidosAtribuidos() {
		Pedido pedido = pedidoComEntregaAceita();
		when(pedidoRepository.findByEntregadorId("31")).thenReturn(List.of(pedido));
		when(itemPedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of());

		List<PedidoResponse> response = pedidoService.listarPorEntregador("31");

		assertThat(response).hasSize(1);
		assertThat(response.get(0).entregadorId()).isEqualTo("31");
	}

	@Test
	void aplicarPedidoEntregueDeveTransicionarDeEmEntregaParaEntregueELiberarEntregador() {
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		pedido.transicionar(PedidoStatus.AGUARDANDO_VALIDACAO, PedidoStatus.VALIDADO);
		pedido.atribuirEntregador("31", "88");
		pedido.transicionar(PedidoStatus.VALIDADO, PedidoStatus.ENTREGA_ACEITA);
		pedido.transicionar(PedidoStatus.ENTREGA_ACEITA, PedidoStatus.EM_ENTREGA);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		when(pedidoRepository.save(pedido)).thenReturn(pedido);
		when(itemPedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of());
		PedidoEntregueDados dados = new PedidoEntregueDados("1", "88", "31", Instant.now(), true, null, null);

		PedidoResponse response = pedidoService.aplicarPedidoEntregue(dados);

		assertThat(response.status()).isEqualTo(PedidoStatus.ENTREGUE);
		verify(entregadorClient).atualizarStatus("31", "DISPONIVEL");
	}

	@Test
	void aplicarPedidoEntregueNaoLiberaEntregadorQuandoPedidoNaoTemUmAtribuido() {
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		pedido.transicionar(PedidoStatus.AGUARDANDO_VALIDACAO, PedidoStatus.VALIDADO);
		pedido.transicionar(PedidoStatus.VALIDADO, PedidoStatus.ENTREGA_ACEITA);
		pedido.transicionar(PedidoStatus.ENTREGA_ACEITA, PedidoStatus.EM_ENTREGA);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		when(pedidoRepository.save(pedido)).thenReturn(pedido);
		when(itemPedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of());
		PedidoEntregueDados dados = new PedidoEntregueDados("1", "88", "31", Instant.now(), true, null, null);

		pedidoService.aplicarPedidoEntregue(dados);

		verify(entregadorClient, org.mockito.Mockito.never()).atualizarStatus(any(), any());
	}

	@Test
	void aplicarPedidoEntregueDeveLancarConflitoQuandoPedidoNaoEstaEmEntrega() {
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		PedidoEntregueDados dados = new PedidoEntregueDados("1", "88", "31", Instant.now(), true, null, null);

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.aplicarPedidoEntregue(dados));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void aplicarTransicaoDeveLancarBadRequestQuandoPedidoIdInvalido() {
		PedidoValidadoDados dados = new PedidoValidadoDados("abc", "2", new EnderecoEntregaRequest("Rua A, 1", -23.56, -46.65),
				new EnderecoEntregaRequest("Rua B, 2", -23.5, -46.6), new BigDecimal("8.00"), 20);

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.aplicarPedidoValidado(dados));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	private AceitarPedidoRequest aceitarPedidoRequestPadrao() {
		return new AceitarPedidoRequest(new EnderecoEntregaRequest("Rua A, 1", -23.56, -46.65), new BigDecimal("8.00"), 20);
	}

	@Test
	void aceitarDeveBaixarEstoqueTransicionarEPublicarPedidoValidado() {
		when(lojaService.buscarPorId(2L)).thenReturn(LOJA);
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		when(pedidoRepository.save(pedido)).thenReturn(pedido);
		ItemPedido item = new ItemPedido(1L, 9L, 2, new BigDecimal("15.90"));
		when(itemPedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of(item));

		PedidoResponse response = pedidoService.aceitar(2L, 1L, aceitarPedidoRequestPadrao());

		assertThat(response.status()).isEqualTo(PedidoStatus.VALIDADO);
		assertThat(response.entregadorId()).isNull();
		assertThat(response.prazoEntrega()).isBetween(
				Instant.now().plus(Loja.TEMPO_MAX_ENTREGA_PADRAO_MIN - 1, ChronoUnit.MINUTES),
				Instant.now().plus(Loja.TEMPO_MAX_ENTREGA_PADRAO_MIN, ChronoUnit.MINUTES));
		verify(produtoService).baixarEstoque(anyMap());
		verify(eventPublisher).publishEvent(any(PedidoValidadoEvent.class));
	}

	@Test
	void aceitarComPrazoDeAceiteVencidoDeveLancarConflitoSemBaixarEstoque() {
		when(lojaService.buscarPorId(2L)).thenReturn(LOJA);
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		pedido.definirPrazoAceite(Instant.now().minusSeconds(1));
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.aceitar(2L, 1L, aceitarPedidoRequestPadrao()));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		org.mockito.Mockito.verifyNoInteractions(produtoService, eventPublisher);
	}

	@Test
	void cancelarPedidosComPrazoDeAceiteVencidoDeveCancelarOsVencidos() {
		Pedido vencido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		when(pedidoRepository.findByStatusAndPrazoAceiteBefore(any(PedidoStatus.class), any(Instant.class)))
				.thenReturn(List.of(vencido));

		pedidoService.cancelarPedidosComPrazoDeAceiteVencido();

		assertThat(vencido.getStatus()).isEqualTo(PedidoStatus.CANCELADO);
		verify(pedidoRepository).save(vencido);
	}

	@Test
	void aceitarDeveAtribuirEntregadorDisponivelETransicionarParaEntregaAceita() {
		when(lojaService.buscarPorId(2L)).thenReturn(LOJA);
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		when(pedidoRepository.save(pedido)).thenReturn(pedido);
		ItemPedido item = new ItemPedido(1L, 9L, 2, new BigDecimal("15.90"));
		when(itemPedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of(item));
		EntregadorDados disponivel = new EntregadorDados("31", "João", "MOTO", "DISPONIVEL");
		when(entregadorClient.buscarDisponivel()).thenReturn(Optional.of(disponivel));

		PedidoResponse response = pedidoService.aceitar(2L, 1L, aceitarPedidoRequestPadrao());

		assertThat(response.status()).isEqualTo(PedidoStatus.ENTREGA_ACEITA);
		assertThat(response.entregadorId()).isEqualTo("31");
		verify(entregadorClient).atualizarStatus("31", "EM_ENTREGA");
		verify(eventPublisher).publishEvent(any(EntregaAceitaEvent.class));
	}

	@Test
	void aceitarDeveLancarConflitoQuandoProdutoServiceAcusaEstoqueInsuficiente() {
		when(lojaService.buscarPorId(2L)).thenReturn(LOJA);
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		ItemPedido item = new ItemPedido(1L, 9L, 2, new BigDecimal("15.90"));
		when(itemPedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of(item));
		doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Estoque insuficiente para o produto Bolo"))
				.when(produtoService).baixarEstoque(anyMap());

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.aceitar(2L, 1L, aceitarPedidoRequestPadrao()));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(pedido.getStatus()).isEqualTo(PedidoStatus.AGUARDANDO_VALIDACAO);
	}

	@Test
	void aceitarDeveLancarNotFoundQuandoPedidoNaoPertenceALoja() {
		when(lojaService.buscarPorId(3L)).thenReturn(LOJA);
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.aceitar(3L, 1L, aceitarPedidoRequestPadrao()));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void aceitarDeveLancarConflitoQuandoPedidoNaoEstaAguardandoValidacao() {
		when(lojaService.buscarPorId(2L)).thenReturn(LOJA);
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		pedido.transicionar(PedidoStatus.AGUARDANDO_VALIDACAO, PedidoStatus.VALIDADO);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.aceitar(2L, 1L, aceitarPedidoRequestPadrao()));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void recusarDeveCancelarPedidoAguardandoValidacao() {
		when(lojaService.buscarPorId(2L)).thenReturn(LOJA);
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		when(pedidoRepository.save(pedido)).thenReturn(pedido);
		when(itemPedidoRepository.findByPedidoId(pedido.getId())).thenReturn(List.of());

		PedidoResponse response = pedidoService.recusar(2L, 1L);

		assertThat(response.status()).isEqualTo(PedidoStatus.CANCELADO);
	}

	@Test
	void recusarDeveLancarNotFoundQuandoPedidoNaoPertenceALoja() {
		when(lojaService.buscarPorId(3L)).thenReturn(LOJA);
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.recusar(3L, 1L));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void recusarDeveLancarConflitoQuandoPedidoNaoEstaAguardandoValidacao() {
		when(lojaService.buscarPorId(2L)).thenReturn(LOJA);
		Pedido pedido = new Pedido(1L, 2L, new BigDecimal("31.80"), "Rua B, 2", -23.5, -46.6);
		pedido.transicionar(PedidoStatus.AGUARDANDO_VALIDACAO, PedidoStatus.VALIDADO);
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

		ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class, () -> pedidoService.recusar(2L, 1L));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}
}
