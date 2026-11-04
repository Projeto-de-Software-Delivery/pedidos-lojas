package br.insper.delivery.entregador.escolha;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

class EscolhaEntregadorConfigTest {

	private AnnotationConfigApplicationContext contexto(Map<String, Object> propriedades) {
		AnnotationConfigApplicationContext contexto = new AnnotationConfigApplicationContext();
		contexto.getEnvironment().getPropertySources().addFirst(new MapPropertySource("teste", propriedades));
		contexto.register(EscolhaEntregadorConfig.class);
		contexto.refresh();
		return contexto;
	}

	@Test
	void semConfiguracaoUsaPrimeiroDisponivel() {
		try (AnnotationConfigApplicationContext contexto = contexto(Map.of())) {
			assertThat(contexto.getBean(EscolhaEntregadorStrategy.class))
					.isInstanceOf(PrimeiroDisponivelStrategy.class);
		}
	}

	@Test
	void usaAEstrategiaConfiguradaEDevolveSempreAMesmaInstancia() {
		try (AnnotationConfigApplicationContext contexto = contexto(
				Map.of("entregador.escolha.estrategia", "preferir-moto"))) {
			EscolhaEntregadorStrategy primeira = contexto.getBean(EscolhaEntregadorStrategy.class);
			EscolhaEntregadorStrategy segunda = contexto.getBean(EscolhaEntregadorStrategy.class);

			assertThat(primeira).isInstanceOf(VeiculoPreferidoStrategy.class);
			assertThat(primeira).isSameAs(segunda);
		}
	}
}
