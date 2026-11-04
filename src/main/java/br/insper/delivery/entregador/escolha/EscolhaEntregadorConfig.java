package br.insper.delivery.entregador.escolha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Registra a estratégia de escolha de entregador como bean <b>Singleton</b>: o container cria uma
 * única instância (via {@link EscolhaEntregadorStrategyFactory}) na subida da aplicação e a
 * compartilha entre todas as requisições. As estratégias não guardam estado por requisição, então
 * a instância única é segura para uso concorrente.
 */
@Configuration
public class EscolhaEntregadorConfig {

	private static final Logger log = LoggerFactory.getLogger(EscolhaEntregadorConfig.class);

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
	public EscolhaEntregadorStrategy escolhaEntregadorStrategy(
			@Value("${entregador.escolha.estrategia:" + PrimeiroDisponivelStrategy.NOME + "}") String nome) {
		EscolhaEntregadorStrategy estrategia = EscolhaEntregadorStrategyFactory.criar(nome);
		log.info("Estratégia de escolha de entregador: {}", estrategia.nome());
		return estrategia;
	}
}
