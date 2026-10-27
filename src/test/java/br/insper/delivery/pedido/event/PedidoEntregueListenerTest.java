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

import br.insper.delivery.pedido.dto.PedidoEntregueDados;

@ExtendWith(MockitoExtension.class)
class PedidoEntregueListenerTest {

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private AmqpTemplate amqpTemplate;

	@InjectMocks
	private PedidoEntregueListener listener;

	private PedidoEntregueEvent eventoDeTeste() {
		PedidoEntregueDados dados = new PedidoEntregueDados("1", "88", "31", Instant.now(), true, "2", "3");
		return new PedidoEntregueEvent(this, PedidoEntregueEvento.de(dados));
	}

	@Test
	void aoEntregarPedidoPublicaNoExchangeComRoutingKeyDoEvento() {
		when(objectMapper.writeValueAsString(any())).thenReturn("{}");

		listener.aoEntregarPedido(eventoDeTeste());

		verify(amqpTemplate).convertAndSend("pedidos", "pedido.entregue", "{}");
	}

	@Test
	void aoEntregarPedidoNaoDeveLancarQuandoSerializacaoFalha() {
		when(objectMapper.writeValueAsString(any())).thenThrow(mock(JacksonException.class));

		assertThatCode(() -> listener.aoEntregarPedido(eventoDeTeste())).doesNotThrowAnyException();
	}

	@Test
	void aoEntregarPedidoNaoDeveLancarQuandoBrokerFalha() {
		when(objectMapper.writeValueAsString(any())).thenReturn("{}");
		doThrow(new AmqpException("broker fora")).when(amqpTemplate)
				.convertAndSend(eq("pedidos"), anyString(), any(Object.class));

		assertThatCode(() -> listener.aoEntregarPedido(eventoDeTeste())).doesNotThrowAnyException();
	}
}
