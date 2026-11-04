package br.insper.delivery.produto.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.insper.delivery.loja.service.LojaService;
import br.insper.delivery.produto.domain.Produto;
import br.insper.delivery.produto.dto.ProdutoRequest;
import br.insper.delivery.produto.repository.ProdutoRepository;

/**
 * Serviço para gerenciar produtos do catálogo.
 */
@Service
public class ProdutoService {

	private static final Logger log = LoggerFactory.getLogger(ProdutoService.class);

	private final ProdutoRepository produtoRepository;
	private final LojaService lojaService;

	public ProdutoService(ProdutoRepository produtoRepository, LojaService lojaService) {
		this.produtoRepository = produtoRepository;
		this.lojaService = lojaService;
	}

	/** Só a loja dona (ou loja legada sem dono) cadastra, altera e exclui seus produtos. */
	public Produto criar(ProdutoRequest request) {
		lojaService.validarAcesso(request.lojaId());
		lojaService.buscarPorId(request.lojaId());
		Produto produto = new Produto(request.lojaId(), request.nome(), request.categoria(), request.preco(),
				request.estoque(), request.foto());
		Produto salvo = produtoRepository.save(produto);
		log.info("Produto criado: id={}, nome={}", salvo.getId(), salvo.getNome());
		return salvo;
	}

	public List<Produto> listarTodos() {
		return produtoRepository.findAll();
	}

	public List<Produto> listarPorLoja(Long lojaId) {
		lojaService.buscarPorId(lojaId);
		return produtoRepository.findByLojaId(lojaId);
	}

	public Produto buscarPorId(Long id) {
		return produtoRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));
	}

	/**
	 * A loja dona do produto não pode ser alterada.
	 */
	public Produto atualizar(Long id, ProdutoRequest request) {
		Produto produto = buscarPorId(id);
		lojaService.validarAcesso(produto.getLojaId());
		if (!produto.getLojaId().equals(request.lojaId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Não é possível mover um produto para outra loja");
		}
		produto.atualizar(request.nome(), request.categoria(), request.preco(), request.estoque(), request.foto());
		return produtoRepository.save(produto);
	}

	public void deletar(Long id) {
		Produto produto = buscarPorId(id);
		lojaService.validarAcesso(produto.getLojaId());
		produtoRepository.delete(produto);
	}

	/**
	 * Tudo-ou-nada: se algum produto não tiver estoque suficiente, nenhum estoque é alterado.
	 */
	@Transactional
	public void baixarEstoque(Map<Long, Integer> quantidadesPorProduto) {
		record Baixa(Produto produto, Integer quantidade) {
		}

		List<Baixa> baixas = quantidadesPorProduto.entrySet().stream()
				.map(entrada -> new Baixa(buscarPorId(entrada.getKey()), entrada.getValue()))
				.toList();

		for (Baixa baixa : baixas) {
			if (baixa.produto().getEstoque() < baixa.quantidade()) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"Estoque insuficiente para o produto " + baixa.produto().getNome());
			}
		}

		for (Baixa baixa : baixas) {
			baixa.produto().baixarEstoque(baixa.quantidade());
			produtoRepository.save(baixa.produto());
		}
	}
}
