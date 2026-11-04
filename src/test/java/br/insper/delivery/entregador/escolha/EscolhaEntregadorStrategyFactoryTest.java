package br.insper.delivery.entregador.escolha;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EscolhaEntregadorStrategyFactoryTest {

	@Test
	void criaCadaEstrategiaPeloNome() {
		assertThat(EscolhaEntregadorStrategyFactory.criar("primeiro-disponivel"))
				.isInstanceOf(PrimeiroDisponivelStrategy.class);
		assertThat(EscolhaEntregadorStrategyFactory.criar("aleatorio")).isInstanceOf(AleatorioStrategy.class);
		assertThat(EscolhaEntregadorStrategyFactory.criar("preferir-moto"))
				.isInstanceOf(VeiculoPreferidoStrategy.class);
	}

	@Test
	void ignoraMaiusculasEEspacos() {
		assertThat(EscolhaEntregadorStrategyFactory.criar("  Aleatorio ")).isInstanceOf(AleatorioStrategy.class);
	}

	@Test
	void todoNomeSuportadoGeraUmaEstrategiaComEsseNome() {
		for (String nome : EscolhaEntregadorStrategyFactory.NOMES_SUPORTADOS) {
			assertThat(EscolhaEntregadorStrategyFactory.criar(nome).nome()).isEqualTo(nome);
		}
	}

	@Test
	void nomeDesconhecidoOuNuloLancaErroComAsOpcoes() {
		assertThatThrownBy(() -> EscolhaEntregadorStrategyFactory.criar("mais-perto"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("mais-perto")
				.hasMessageContaining("primeiro-disponivel");
		assertThatThrownBy(() -> EscolhaEntregadorStrategyFactory.criar(null))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
