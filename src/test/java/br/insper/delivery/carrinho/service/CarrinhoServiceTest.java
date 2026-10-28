package br.insper.delivery.carrinho.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import br.insper.delivery.carrinho.domain.ItemCarrinho;
import br.insper.delivery.carrinho.dto.CarrinhoResponse;
import br.insper.delivery.carrinho.dto.ItemCarrinhoRequest;
import br.insper.delivery.carrinho.repository.ItemCarrinhoRepository;
import br.insper.delivery.cliente.domain.Cliente;
import br.insper.delivery.cliente.service.ClienteService;
import br.insper.delivery.produto.domain.Produto;
import br.insper.delivery.produto.service.ProdutoService;

@ExtendWith(MockitoExtension.class)
class CarrinhoServiceTest {

	@Mock
	private ItemCarrinhoRepository itemCarrinhoRepository;

	@Mock
	private ClienteService clienteService;

	@Mock
	private ProdutoService produtoService;

	@InjectMocks
	private CarrinhoService carrinhoService;

	private static final Cliente CLIENTE = new Cliente("Ana", "ana@email.com", "11999999999");

	@Test
	void adicionarItemNovoDeveCriarItem() {
		when(clienteService.buscarPorId(1L)).thenReturn(CLIENTE);
		Produto produto = new Produto(7L, "Bolo", "Sobremesas", new BigDecimal("10.00"), 10, "foto.png");
		when(produtoService.buscarPorId(2L)).thenReturn(produto);
		when(itemCarrinhoRepository.findByClienteIdAndProdutoId(1L, 2L)).thenReturn(Optional.empty());
		ItemCarrinho salvo = new ItemCarrinho(1L, 2L, 3, new BigDecimal("10.00"));
		when(itemCarrinhoRepository.save(any(ItemCarrinho.class))).thenReturn(salvo);
		when(itemCarrinhoRepository.findByClienteId(1L)).thenReturn(List.of(salvo));

		CarrinhoResponse response = carrinhoService.adicionarItem(1L, new ItemCarrinhoRequest(2L, 3));

		assertThat(response.itens()).hasSize(1);
		assertThat(response.total()).isEqualByComparingTo("30.00");
	}

	@Test
	void adicionarItemExistenteDeveSomarQuantidade() {
		when(clienteService.buscarPorId(1L)).thenReturn(CLIENTE);
		Produto produto = new Produto(7L, "Bolo", "Sobremesas", new BigDecimal("10.00"), 10, "foto.png");
		when(produtoService.buscarPorId(2L)).thenReturn(produto);
		ItemCarrinho existente = new ItemCarrinho(1L, 2L, 2, new BigDecimal("10.00"));
		when(itemCarrinhoRepository.findByClienteIdAndProdutoId(1L, 2L)).thenReturn(Optional.of(existente));
		when(itemCarrinhoRepository.save(any(ItemCarrinho.class))).thenReturn(existente);
		when(itemCarrinhoRepository.findByClienteId(1L)).thenReturn(List.of(existente));

		CarrinhoResponse response = carrinhoService.adicionarItem(1L, new ItemCarrinhoRequest(2L, 3));

		assertThat(existente.getQuantidade()).isEqualTo(5);
		assertThat(response.total()).isEqualByComparingTo("50.00");
	}

	@Test
	void adicionarItemAcimaDoEstoqueDeveLancar422() {
		when(clienteService.buscarPorId(1L)).thenReturn(CLIENTE);
		Produto produto = new Produto(7L, "Bolo", "Sobremesas", new BigDecimal("10.00"), 2, "foto.png");
		when(produtoService.buscarPorId(2L)).thenReturn(produto);
		when(itemCarrinhoRepository.findByClienteIdAndProdutoId(1L, 2L)).thenReturn(Optional.empty());

		ResponseStatusException e = org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> carrinhoService.adicionarItem(1L, new ItemCarrinhoRequest(2L, 3)));

		assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
		verify(itemCarrinhoRepository, never()).save(any(ItemCarrinho.class));
	}

	@Test
	void adicionarItemExistenteQueSomadoPassaDoEstoqueDeveLancar422() {
		when(clienteService.buscarPorId(1L)).thenReturn(CLIENTE);
		Produto produto = new Produto(7L, "Bolo", "Sobremesas", new BigDecimal("10.00"), 4, "foto.png");
		when(produtoService.buscarPorId(2L)).thenReturn(produto);
		ItemCarrinho existente = new ItemCarrinho(1L, 2L, 3, new BigDecimal("10.00"));
		when(itemCarrinhoRepository.findByClienteIdAndProdutoId(1L, 2L)).thenReturn(Optional.of(existente));

		org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> carrinhoService.adicionarItem(1L, new ItemCarrinhoRequest(2L, 2)));

		assertThat(existente.getQuantidade()).isEqualTo(3);
	}

	@Test
	void alterarQuantidadeDeveTrocarQuantidadeEPreco() {
		when(clienteService.buscarPorId(1L)).thenReturn(CLIENTE);
		ItemCarrinho item = new ItemCarrinho(1L, 2L, 3, new BigDecimal("10.00"));
		when(itemCarrinhoRepository.findById(5L)).thenReturn(Optional.of(item));
		Produto produto = new Produto(7L, "Bolo", "Sobremesas", new BigDecimal("12.00"), 10, "foto.png");
		when(produtoService.buscarPorId(2L)).thenReturn(produto);
		when(itemCarrinhoRepository.findByClienteId(1L)).thenReturn(List.of(item));

		CarrinhoResponse response = carrinhoService.alterarQuantidade(1L, 5L, 1);

		assertThat(item.getQuantidade()).isEqualTo(1);
		assertThat(response.total()).isEqualByComparingTo("12.00");
		verify(itemCarrinhoRepository).save(item);
	}

	@Test
	void alterarQuantidadeAcimaDoEstoqueDeveLancar422() {
		when(clienteService.buscarPorId(1L)).thenReturn(CLIENTE);
		ItemCarrinho item = new ItemCarrinho(1L, 2L, 1, new BigDecimal("10.00"));
		when(itemCarrinhoRepository.findById(5L)).thenReturn(Optional.of(item));
		Produto produto = new Produto(7L, "Bolo", "Sobremesas", new BigDecimal("10.00"), 2, "foto.png");
		when(produtoService.buscarPorId(2L)).thenReturn(produto);

		ResponseStatusException e = org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> carrinhoService.alterarQuantidade(1L, 5L, 3));

		assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
		assertThat(item.getQuantidade()).isEqualTo(1);
	}

	@Test
	void adicionarItemDeveLancarQuandoClienteNaoEncontrado() {
		when(clienteService.buscarPorId(1L))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));

		org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> carrinhoService.adicionarItem(1L, new ItemCarrinhoRequest(2L, 1)));
	}

	@Test
	void adicionarItemDeveLancarQuandoProdutoNaoEncontrado() {
		when(clienteService.buscarPorId(1L)).thenReturn(CLIENTE);
		when(produtoService.buscarPorId(2L))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));

		org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> carrinhoService.adicionarItem(1L, new ItemCarrinhoRequest(2L, 1)));
	}

	@Test
	void buscarCarrinhoVazioDeveRetornarTotalZero() {
		when(clienteService.buscarPorId(1L)).thenReturn(CLIENTE);
		when(itemCarrinhoRepository.findByClienteId(1L)).thenReturn(List.of());

		CarrinhoResponse response = carrinhoService.buscarCarrinho(1L);

		assertThat(response.itens()).isEmpty();
		assertThat(response.total()).isEqualByComparingTo("0");
	}

	@Test
	void removerItemDeveExcluirItem() {
		when(clienteService.buscarPorId(1L)).thenReturn(CLIENTE);
		ItemCarrinho item = new ItemCarrinho(1L, 2L, 3, new BigDecimal("10.00"));
		when(itemCarrinhoRepository.findById(5L)).thenReturn(Optional.of(item));
		when(itemCarrinhoRepository.findByClienteId(1L)).thenReturn(List.of());

		CarrinhoResponse response = carrinhoService.removerItem(1L, 5L);

		verify(itemCarrinhoRepository).delete(item);
		assertThat(response.itens()).isEmpty();
	}

	@Test
	void removerItemDeveLancarQuandoItemNaoEncontrado() {
		when(clienteService.buscarPorId(1L)).thenReturn(CLIENTE);
		when(itemCarrinhoRepository.findById(5L)).thenReturn(Optional.empty());

		org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> carrinhoService.removerItem(1L, 5L));
	}

	@Test
	void removerItemDeveLancarQuandoItemPertenceAOutroCliente() {
		when(clienteService.buscarPorId(1L)).thenReturn(CLIENTE);
		ItemCarrinho itemDeOutroCliente = new ItemCarrinho(9L, 2L, 3, new BigDecimal("10.00"));
		when(itemCarrinhoRepository.findById(5L)).thenReturn(Optional.of(itemDeOutroCliente));

		org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> carrinhoService.removerItem(1L, 5L));
	}
}
