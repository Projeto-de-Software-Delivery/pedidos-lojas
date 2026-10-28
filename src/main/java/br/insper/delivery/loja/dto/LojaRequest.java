package br.insper.delivery.loja.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** Prazos em minutos são opcionais; sem eles a loja usa o padrão (ver Loja). */
public record LojaRequest(
		@NotBlank String nome,
		@NotBlank @Pattern(regexp = "\\d{14}") String cnpj,
		@NotBlank String endereco,
		@Positive Integer tempoMaxAceiteMin,
		@Positive Integer tempoMaxEntregaMin) {

	public LojaRequest(String nome, String cnpj, String endereco) {
		this(nome, cnpj, endereco, null, null);
	}
}
