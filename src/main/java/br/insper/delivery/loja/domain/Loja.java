package br.insper.delivery.loja.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Entidade que representa uma loja.
 */
@Entity
@Table(indexes = @Index(name = "idx_loja_auth0_sub", columnList = "auth0_sub"))
public class Loja {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nome;

	private String cnpj;

	private String endereco;

	@Column(name = "auth0_sub", length = 128, unique = true)
	private String auth0Sub;

	protected Loja() {
	}

	public Loja(String nome, String cnpj, String endereco) {
		this(nome, cnpj, endereco, null);
	}

	public Loja(String nome, String cnpj, String endereco, String auth0Sub) {
		this.nome = nome;
		this.cnpj = cnpj;
		this.endereco = endereco;
		this.auth0Sub = auth0Sub;
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

	public String getAuth0Sub() {
		return auth0Sub;
	}

	public void setAuth0Sub(String auth0Sub) {
		this.auth0Sub = auth0Sub;
	}

	public void atualizar(String nome, String cnpj, String endereco) {
		this.nome = nome;
		this.cnpj = cnpj;
		this.endereco = endereco;
	}

}
