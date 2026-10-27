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
 * Publica o evento pedido.retirado no RabbitMQ (exchange "pedidos", routing key "pedido.retirado"),
 * consumido pelo serviço de notificações.
 */
@Component
public class PedidoRetiradoListener {

	private static final Logger log = LoggerFactory.getLogger(PedidoRetiradoListener.class);

	private final ObjectMapper objectMapper;
	private final AmqpTemplate amqpTemplate;

	public PedidoRetiradoListener(ObjectMapper objectMapper, AmqpTemplate amqpTemplate) {
		this.objectMapper = objectMapper;
		this.amqpTemplate = amqpTemplate;
	}

	@EventListener
	public void aoRetirarPedido(PedidoRetiradoEvent event) {
		PedidoRetiradoEvento evento = event.getEvento();
		try {
			String payload = objectMapper.writeValueAsString(evento);
			amqpTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_PEDIDOS, evento.eventType(), payload);
			log.info("Publicado no topico {}: {}", evento.eventType(), payload);
		} catch (JacksonException e) {
			log.error("Falha ao serializar evento pedido.retirado", e);
		} catch (AmqpException e) {
			// Publicacao e best-effort: a retirada ja foi persistida, nao falha a requisicao por
			// causa de uma indisponibilidade momentanea do broker.
			log.error("Falha ao publicar evento pedido.retirado no RabbitMQ", e);
		}
	}
}
