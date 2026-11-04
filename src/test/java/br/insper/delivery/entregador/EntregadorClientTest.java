package br.insper.delivery.entregador;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class EntregadorClientTest {

	private HttpServer servicoFalso;
	private EntregadorClient client;

	@AfterEach
	void derrubarServicoFalso() {
		if (servicoFalso != null) {
			servicoFalso.stop(0);
		}
	}

	private void subirServicoFalso(String corpoListaEntregadores) throws IOException {
		servicoFalso = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		servicoFalso.createContext("/entregadores", exchange -> {
			byte[] corpo = corpoListaEntregadores.getBytes();
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, corpo.length);
			exchange.getResponseBody().write(corpo);
			exchange.close();
		});
		servicoFalso.start();
		int porta = servicoFalso.getAddress().getPort();
		client = new EntregadorClient("http://localhost:" + porta);
	}

	@Test
	void listarDisponiveisRetornaSoOsComStatusDisponivel() throws IOException {
		subirServicoFalso("[{\"id\":\"1\",\"nome\":\"Ana\",\"veiculo\":\"CARRO\",\"status\":\"EM_ENTREGA\"},"
				+ "{\"id\":\"2\",\"nome\":\"João\",\"veiculo\":\"MOTO\",\"status\":\"DISPONIVEL\"}]");

		List<EntregadorDados> resultado = client.listarDisponiveis();

		assertThat(resultado).hasSize(1);
		assertThat(resultado.get(0).id()).isEqualTo("2");
	}

	@Test
	void listarDisponiveisRetornaVazioQuandoNenhumDisponivel() throws IOException {
		subirServicoFalso("[{\"id\":\"1\",\"nome\":\"Ana\",\"veiculo\":\"CARRO\",\"status\":\"EM_ENTREGA\"}]");

		assertThat(client.listarDisponiveis()).isEmpty();
	}

	@Test
	void listarDisponiveisRetornaVazioQuandoServicoIndisponivel() {
		client = new EntregadorClient("http://localhost:1");

		assertThat(client.listarDisponiveis()).isEmpty();
	}

	@Test
	void atualizarStatusNaoLancaQuandoServicoIndisponivel() {
		client = new EntregadorClient("http://localhost:1");

		client.atualizarStatus("1", "EM_ENTREGA");
	}

	@Test
	void atualizarStatusEnviaPatchComONovoStatus() throws IOException {
		servicoFalso = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		AtomicReference<String> metodoRecebido = new AtomicReference<>();
		AtomicReference<String> corpoRecebido = new AtomicReference<>();
		servicoFalso.createContext("/entregadores/2/status", exchange -> {
			metodoRecebido.set(exchange.getRequestMethod());
			corpoRecebido.set(new String(exchange.getRequestBody().readAllBytes()));
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});
		servicoFalso.start();
		client = new EntregadorClient("http://localhost:" + servicoFalso.getAddress().getPort());

		client.atualizarStatus("2", "EM_ENTREGA");

		assertThat(metodoRecebido.get()).isEqualTo("PATCH");
		assertThat(corpoRecebido.get()).contains("EM_ENTREGA");
	}
}
