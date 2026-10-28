package br.insper.delivery.carrinho.domain;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Entidade que representa um item no carrinho de um cliente.
 */
@Entity
public class ItemCarrinho {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long clienteId;

	private Long produtoId;

	private Integer quantidade;

	private BigDecimal precoUnitario;

	protected ItemCarrinho() {
	}

	public ItemCarrinho(Long clienteId, Long produtoId, Integer quantidade, BigDecimal precoUnitario) {
		this.clienteId = clienteId;
		this.produtoId = produtoId;
		this.quantidade = quantidade;
		this.precoUnitario = precoUnitario;
	}

	public Long getId() {
		return id;
	}

	public Long getClienteId() {
		return clienteId;
	}

	public Long getProdutoId() {
		return produtoId;
	}

	public Integer getQuantidade() {
		return quantidade;
	}

	public BigDecimal getPrecoUnitario() {
		return precoUnitario;
	}

	public BigDecimal getSubtotal() {
		return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
	}

	/** Também atualiza o preço unitário pro preço atual do produto, não só soma a quantidade. */
	public void adicionarQuantidade(Integer quantidade, BigDecimal precoUnitario) {
		this.quantidade += quantidade;
		this.precoUnitario = precoUnitario;
	}

	public void alterarQuantidade(Integer quantidade, BigDecimal precoUnitario) {
		this.quantidade = quantidade;
		this.precoUnitario = precoUnitario;
	}

}
