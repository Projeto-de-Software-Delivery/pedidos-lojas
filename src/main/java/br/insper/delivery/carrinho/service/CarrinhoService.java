package br.insper.delivery.carrinho.service;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.insper.delivery.carrinho.domain.ItemCarrinho;
import br.insper.delivery.carrinho.dto.CarrinhoResponse;
import br.insper.delivery.carrinho.dto.ItemCarrinhoRequest;
import br.insper.delivery.carrinho.dto.ItemCarrinhoResponse;
import br.insper.delivery.carrinho.repository.ItemCarrinhoRepository;
import br.insper.delivery.cliente.service.ClienteService;
import br.insper.delivery.produto.domain.Produto;
import br.insper.delivery.produto.service.ProdutoService;

/**
 * Serviço para gerenciar o carrinho de compras de um cliente.
 */
@Service
public class CarrinhoService {

	private static final Logger log = LoggerFactory.getLogger(CarrinhoService.class);

	private final ItemCarrinhoRepository itemCarrinhoRepository;
	private final ClienteService clienteService;
	private final ProdutoService produtoService;

	public CarrinhoService(ItemCarrinhoRepository itemCarrinhoRepository, ClienteService clienteService,
			ProdutoService produtoService) {
		this.itemCarrinhoRepository = itemCarrinhoRepository;
		this.clienteService = clienteService;
		this.produtoService = produtoService;
	}

	/**
	 * Se o produto já estiver no carrinho, soma a quantidade à existente e atualiza o preço
	 * unitário para o preço atual do produto. A quantidade total não pode passar do estoque (422).
	 */
	public CarrinhoResponse adicionarItem(Long clienteId, ItemCarrinhoRequest request) {
		clienteService.buscarPorId(clienteId);
		Produto produto = produtoService.buscarPorId(request.produtoId());

		ItemCarrinho item = itemCarrinhoRepository.findByClienteIdAndProdutoId(clienteId, request.produtoId())
				.map(existente -> {
					validarEstoque(produto, existente.getQuantidade() + request.quantidade());
					existente.adicionarQuantidade(request.quantidade(), produto.getPreco());
					return existente;
				})
				.orElseGet(() -> {
					validarEstoque(produto, request.quantidade());
					return new ItemCarrinho(clienteId, request.produtoId(), request.quantidade(), produto.getPreco());
				});

		ItemCarrinho salvo = itemCarrinhoRepository.save(item);
		log.info("Item adicionado ao carrinho: clienteId={}, produtoId={}, quantidade={}", salvo.getClienteId(),
				salvo.getProdutoId(), salvo.getQuantidade());
		return montarCarrinho(clienteId);
	}

	/** Troca a quantidade do item (não soma); não pode passar do estoque (422). */
	public CarrinhoResponse alterarQuantidade(Long clienteId, Long itemId, Integer quantidade) {
		clienteService.buscarPorId(clienteId);
		ItemCarrinho item = buscarItem(clienteId, itemId);
		Produto produto = produtoService.buscarPorId(item.getProdutoId());
		validarEstoque(produto, quantidade);

		item.alterarQuantidade(quantidade, produto.getPreco());
		itemCarrinhoRepository.save(item);
		log.info("Quantidade alterada no carrinho: clienteId={}, produtoId={}, quantidade={}", clienteId,
				item.getProdutoId(), quantidade);
		return montarCarrinho(clienteId);
	}

	public CarrinhoResponse removerItem(Long clienteId, Long itemId) {
		clienteService.buscarPorId(clienteId);
		ItemCarrinho item = buscarItem(clienteId, itemId);

		itemCarrinhoRepository.delete(item);
		log.info("Item removido do carrinho: clienteId={}, produtoId={}", item.getClienteId(), item.getProdutoId());
		return montarCarrinho(clienteId);
	}

	public CarrinhoResponse buscarCarrinho(Long clienteId) {
		clienteService.buscarPorId(clienteId);
		return montarCarrinho(clienteId);
	}

	private void validarEstoque(Produto produto, int quantidade) {
		if (quantidade > produto.getEstoque()) {
			throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
					"Só temos " + produto.getEstoque() + " unidade(s) de " + produto.getNome() + " em estoque");
		}
	}

	private ItemCarrinho buscarItem(Long clienteId, Long itemId) {
		ItemCarrinho item = itemCarrinhoRepository.findById(itemId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado"));
		if (!item.getClienteId().equals(clienteId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado");
		}
		return item;
	}

	private CarrinhoResponse montarCarrinho(Long clienteId) {
		List<ItemCarrinho> itens = itemCarrinhoRepository.findByClienteId(clienteId);
		List<ItemCarrinhoResponse> itensResponse = itens.stream().map(ItemCarrinhoResponse::from).toList();
		BigDecimal total = itens.stream().map(ItemCarrinho::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new CarrinhoResponse(clienteId, itensResponse, total);
	}
}
