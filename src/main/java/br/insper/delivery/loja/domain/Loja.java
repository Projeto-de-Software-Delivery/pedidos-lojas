package br.insper.delivery.loja.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Entidade que representa uma loja.
 */
@Entity
public class Loja {

	public static final int TEMPO_MAX_ACEITE_PADRAO_MIN = 10;
	public static final int TEMPO_MAX_ENTREGA_PADRAO_MIN = 60;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nome;

	private String cnpj;

	private String endereco;

	// prazos definidos pela loja; null (lojas antigas ou não informado) usa o padrão
	private Integer tempoMaxAceiteMin;

	private Integer tempoMaxEntregaMin;

	protected Loja() {
	}

	public Loja(String nome, String cnpj, String endereco) {
		this.nome = nome;
		this.cnpj = cnpj;
		this.endereco = endereco;
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getCnpj() {
		return cnpj;
	}

	public String getEndereco() {
		return endereco;
	}

	/** Minutos que a loja tem para aceitar um pedido novo antes de ele ser cancelado. */
	public int getTempoMaxAceiteMin() {
		return tempoMaxAceiteMin != null ? tempoMaxAceiteMin : TEMPO_MAX_ACEITE_PADRAO_MIN;
	}

	/** Minutos, contados da aceitação, para o pedido chegar ao cliente antes de aparecer como atrasado. */
	public int getTempoMaxEntregaMin() {
		return tempoMaxEntregaMin != null ? tempoMaxEntregaMin : TEMPO_MAX_ENTREGA_PADRAO_MIN;
	}

	public void atualizar(String nome, String cnpj, String endereco) {
		this.nome = nome;
		this.cnpj = cnpj;
		this.endereco = endereco;
	}

	/** null volta para o padrão. */
	public void definirPrazos(Integer tempoMaxAceiteMin, Integer tempoMaxEntregaMin) {
		this.tempoMaxAceiteMin = tempoMaxAceiteMin;
		this.tempoMaxEntregaMin = tempoMaxEntregaMin;
	}

}
