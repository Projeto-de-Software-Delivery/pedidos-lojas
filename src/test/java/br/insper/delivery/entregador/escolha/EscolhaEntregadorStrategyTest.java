package br.insper.delivery.entregador.escolha;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import br.insper.delivery.entregador.EntregadorDados;

class EscolhaEntregadorStrategyTest {

	private static final EntregadorDados ANA_CARRO = new EntregadorDados("1", "Ana", "CARRO", "DISPONIVEL");
	private static final EntregadorDados JOAO_MOTO = new EntregadorDados("2", "João", "MOTO", "DISPONIVEL");
	private static final EntregadorDados BIA_BIKE = new EntregadorDados("3", "Bia", "BICICLETA", "DISPONIVEL");

	@Test
	void primeiroDisponivelEscolheOPrimeiroDaLista() {
		EscolhaEntregadorStrategy estrategia = new PrimeiroDisponivelStrategy();

		assertThat(estrategia.nome()).isEqualTo("primeiro-disponivel");
		assertThat(estrategia.escolher(List.of(ANA_CARRO, JOAO_MOTO))).contains(ANA_CARRO);
		assertThat(estrategia.escolher(List.of())).isEmpty();
	}

	@Test
	void aleatorioEscolheOIndiceSorteado() {
		RandomGenerator sempreODoMeio = new RandomGenerator() {
			@Override
			public long nextLong() {
				return 0;
			}

			@Override
			public int nextInt(int bound) {
				return 1;
			}
		};
		EscolhaEntregadorStrategy estrategia = new AleatorioStrategy(sempreODoMeio);

		assertThat(estrategia.nome()).isEqualTo("aleatorio");
		assertThat(estrategia.escolher(List.of(ANA_CARRO, JOAO_MOTO, BIA_BIKE))).contains(JOAO_MOTO);
	}

	@Test
	void aleatorioSempreDevolveAlguemDaListaEVazioSemNinguem() {
		EscolhaEntregadorStrategy estrategia = new AleatorioStrategy();
		List<EntregadorDados> disponiveis = List.of(ANA_CARRO, JOAO_MOTO, BIA_BIKE);

		for (int i = 0; i < 20; i++) {
			assertThat(estrategia.escolher(disponiveis)).get().isIn(disponiveis);
		}
		assertThat(estrategia.escolher(List.of())).isEmpty();
	}

	@Test
	void preferirMotoEscolheMotoMesmoSeNaoForOPrimeiro() {
		EscolhaEntregadorStrategy estrategia = new VeiculoPreferidoStrategy();

		assertThat(estrategia.nome()).isEqualTo("preferir-moto");
		assertThat(estrategia.escolher(List.of(ANA_CARRO, JOAO_MOTO))).contains(JOAO_MOTO);
	}

	@Test
	void preferirMotoCaiParaOPrimeiroQuandoNaoHaMoto() {
		EscolhaEntregadorStrategy estrategia = new VeiculoPreferidoStrategy();

		assertThat(estrategia.escolher(List.of(BIA_BIKE, ANA_CARRO))).contains(BIA_BIKE);
		assertThat(estrategia.escolher(List.of())).isEmpty();
	}
}
