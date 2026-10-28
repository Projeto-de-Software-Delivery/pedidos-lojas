package br.insper.delivery.carrinho.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QuantidadeItemRequest(@NotNull @Positive Integer quantidade) {
}
