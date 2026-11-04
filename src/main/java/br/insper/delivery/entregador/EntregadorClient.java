package br.insper.delivery.entregador;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente HTTP do serviço de entregador. O serviço só expõe CRUD + status (sem fila nem endpoint
 * de oferta de corrida), então a atribuição é feita por chamada síncrona: lista os DISPONIVEL
 * e marca o escolhido como EM_ENTREGA.
 */
@Service
public class EntregadorClient {

	private static final Logger log = LoggerFactory.getLogger(EntregadorClient.class);
	private static final String STATUS_DISPONIVEL = "DISPONIVEL";

	private final RestClient restClient;

	public EntregadorClient(@Value("${entregador.service.url}") String baseUrl) {
		this.restClient = RestClient.create(baseUrl);
	}

	/**
	 * Todos os entregadores com status DISPONIVEL; qual deles recebe a corrida é decidido pela
	 * {@code EscolhaEntregadorStrategy}. Best-effort: uma indisponibilidade do serviço de
	 * entregador devolve lista vazia em vez de falhar o pedido.
	 */
	public List<EntregadorDados> listarDisponiveis() {
		try {
			List<EntregadorDados> entregadores = restClient.get()
					.uri("/entregadores")
					.retrieve()
					.body(new ParameterizedTypeReference<List<EntregadorDados>>() {
					});
			return (entregadores == null ? List.<EntregadorDados>of() : entregadores).stream()
					.filter(entregador -> STATUS_DISPONIVEL.equals(entregador.status()))
					.toList();
		} catch (RestClientException e) {
			log.error("Falha ao consultar entregadores disponíveis", e);
			return List.of();
		}
	}

	/** Best-effort: se falhar, o entregador fica com status desatualizado no serviço dele. */
	public void atualizarStatus(String entregadorId, String status) {
		try {
			restClient.patch()
					.uri("/entregadores/{id}/status", entregadorId)
					.body(new EntregadorStatusUpdate(status))
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException e) {
			log.error("Falha ao atualizar status do entregador {} para {}", entregadorId, status, e);
		}
	}

	private record EntregadorStatusUpdate(String status) {
	}
}
