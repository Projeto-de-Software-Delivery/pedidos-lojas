package br.insper.delivery.pedido.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import br.insper.delivery.pedido.dto.PedidoRetiradoDados;

@ExtendWith(MockitoExtension.class)
class PedidoRetiradoListenerTest {

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private AmqpTemplate amqpTemplate;

	@InjectMocks
	private PedidoRetiradoListener listener;

	private PedidoRetiradoEvent eventoDeTeste() {
		PedidoRetiradoDados dados = new PedidoRetiradoDados("1", "88", "31", Instant.now(), 20, "2", "3");
		return new PedidoRetiradoEvent(this, PedidoRetiradoEvento.de(dados));
	}

	@Test
	void aoRetirarPedidoPublicaNoExchangeComRoutingKeyDoEvento() {
		when(objectMapper.writeValueAsString(any())).thenReturn("{}");

		listener.aoRetirarPedido(eventoDeTeste());

		verify(amqpTemplate).convertAndSend("pedidos", "pedido.retirado", "{}");
	}

	@Test
	void aoRetirarPedidoNaoDeveLancarQuandoSerializacaoFalha() {
		when(objectMapper.writeValueAsString(any())).thenThrow(mock(JacksonException.class));

		assertThatCode(() -> listener.aoRetirarPedido(eventoDeTeste())).doesNotThrowAnyException();
	}

	@Test
	void aoRetirarPedidoNaoDeveLancarQuandoBrokerFalha() {
		when(objectMapper.writeValueAsString(any())).thenReturn("{}");
		doThrow(new AmqpException("broker fora")).when(amqpTemplate)
				.convertAndSend(eq("pedidos"), anyString(), any(Object.class));

		assertThatCode(() -> listener.aoRetirarPedido(eventoDeTeste())).doesNotThrowAnyException();
	}
}
