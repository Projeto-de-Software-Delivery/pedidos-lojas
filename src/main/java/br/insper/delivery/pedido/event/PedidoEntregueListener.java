package br.insper.delivery.pedido.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import br.insper.delivery.config.RabbitMQConfig;

/**
 * Publica o evento pedido.entregue no RabbitMQ (exchange "pedidos", routing key "pedido.entregue"),
 * consumido pelo serviço de notificações.
 */
@Component
public class PedidoEntregueListener {

	private static final Logger log = LoggerFactory.getLogger(PedidoEntregueListener.class);

	private final ObjectMapper objectMapper;
	private final AmqpTemplate amqpTemplate;

	public PedidoEntregueListener(ObjectMapper objectMapper, AmqpTemplate amqpTemplate) {
		this.objectMapper = objectMapper;
		this.amqpTemplate = amqpTemplate;
	}

	@EventListener
	public void aoEntregarPedido(PedidoEntregueEvent event) {
		PedidoEntregueEvento evento = event.getEvento();
		try {
			String payload = objectMapper.writeValueAsString(evento);
			amqpTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_PEDIDOS, evento.eventType(), payload);
			log.info("Publicado no topico {}: {}", evento.eventType(), payload);
		} catch (JacksonException e) {
			log.error("Falha ao serializar evento pedido.entregue", e);
		} catch (AmqpException e) {
			// Publicacao e best-effort: a entrega ja foi persistida, nao falha a requisicao por
			// causa de uma indisponibilidade momentanea do broker.
			log.error("Falha ao publicar evento pedido.entregue no RabbitMQ", e);
		}
	}
}
