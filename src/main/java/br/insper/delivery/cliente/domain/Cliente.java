package br.insper.delivery.cliente.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Entidade que representa um cliente.
 */
@Entity
@Table(indexes = @Index(name = "idx_cliente_auth0_sub", columnList = "auth0_sub"))
public class Cliente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nome;

	private String email;

	private String telefone;

	@Column(name = "auth0_sub", length = 128, unique = true)
	private String auth0Sub;

	protected Cliente() {
	}

	public Cliente(String nome, String email, String telefone) {
		this(nome, email, telefone, null);
	}

	public Cliente(String nome, String email, String telefone, String auth0Sub) {
		this.nome = nome;
		this.email = email;
		this.telefone = telefone;
		this.auth0Sub = auth0Sub;
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getEmail() {
		return email;
	}

	public String getTelefone() {
		return telefone;
	}

	public String getAuth0Sub() {
		return auth0Sub;
	}

	public void setAuth0Sub(String auth0Sub) {
		this.auth0Sub = auth0Sub;
	}

	public void atualizar(String nome, String email, String telefone) {
		this.nome = nome;
		this.email = email;
		this.telefone = telefone;
	}

}
