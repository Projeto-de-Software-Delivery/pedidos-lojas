package br.insper.delivery.pedido.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class EventoConsumidorControllerTest {

	@Autowired
	private MockMvc mockMvc;

	private Long extrairId(MvcResult result) throws Exception {
		String body = result.getResponse().getContentAsString();
		Matcher matcher = Pattern.compile("\"id\":(\\d+)").matcher(body);
		matcher.find();
		return Long.valueOf(matcher.group(1));
	}

	private Long criarCliente() throws Exception {
		MvcResult result = mockMvc
				.perform(post("/clientes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\":\"Ana\",\"email\":\"evento.teste@email.com\","
								+ "\"telefone\":\"11999999999\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		return extrairId(result);
	}

	private Long criarLoja() throws Exception {
		MvcResult result = mockMvc
				.perform(post("/lojas")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\":\"Padaria\",\"cnpj\":\"12345678000199\",\"endereco\":\"Rua A, 1\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		return extrairId(result);
	}

	private Long criarProduto(Long lojaId) throws Exception {
		MvcResult result = mockMvc
				.perform(post("/produtos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lojaId\":" + lojaId + ",\"nome\":\"Bolo\",\"categoria\":\"Sobremesas\","
								+ "\"preco\":15.90,\"estoque\":100,\"foto\":\"http://exemplo.com/foto.png\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		return extrairId(result);
	}

	private Long criarPedido() throws Exception {
		Long clienteId = criarCliente();
		Long lojaId = criarLoja();
		Long produtoId = criarProduto(lojaId);
		MvcResult result = mockMvc
				.perform(post("/clientes/" + clienteId + "/pedidos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lojaId\":" + lojaId + ",\"itens\":[{\"produtoId\":" + produtoId
								+ ",\"quantidade\":2}],\"enderecoEntrega\":{\"rua\":\"Rua B, 2\",\"lat\":-23.5,"
								+ "\"lng\":-46.6}}"))
				.andExpect(status().isCreated())
				.andReturn();
		return extrairId(result);
	}

	private String envelope(String eventType, String data) {
		return "{\"eventId\":\"" + UUID.randomUUID() + "\",\"eventType\":\"" + eventType + "\",\"version\":1,"
				+ "\"occurredAt\":\"2026-09-30T14:00:00Z\",\"data\":" + data + "}";
	}

	private String pedidoValidadoData(Long pedidoId) {
		return "{\"pedidoId\":\"" + pedidoId + "\",\"lojaId\":\"7\","
				+ "\"enderecoRetirada\":{\"rua\":\"Rua A, 1\",\"lat\":-23.56,\"lng\":-46.65},"
				+ "\"enderecoEntrega\":{\"rua\":\"Rua B, 2\",\"lat\":-23.5,\"lng\":-46.6},"
				+ "\"valorFrete\":8.00,\"tempoPreparoMin\":20}";
	}

	private String entregaAceitaData(Long pedidoId) {
		return "{\"pedidoId\":\"" + pedidoId + "\",\"entregaId\":\"88\",\"entregadorId\":\"31\","
				+ "\"nomeEntregador\":\"Joao\",\"veiculo\":\"moto\",\"etaRetiradaMin\":12}";
	}

	private String pedidoRetiradoData(Long pedidoId) {
		return "{\"pedidoId\":\"" + pedidoId + "\",\"entregaId\":\"88\",\"entregadorId\":\"31\","
				+ "\"retiradoEm\":\"2026-09-30T14:25:00Z\",\"etaEntregaMin\":18}";
	}

	private String pedidoEntregueData(Long pedidoId) {
		return "{\"pedidoId\":\"" + pedidoId + "\",\"entregaId\":\"88\",\"entregadorId\":\"31\","
				+ "\"entregueEm\":\"2026-09-30T14:45:00Z\",\"pinValidado\":true}";
	}

	@Test
	void consomeOsQuatroEventosEAvancaAMaquinaDeEstadosNaOrdem() throws Exception {
		Long pedidoId = criarPedido();

		mockMvc.perform(post("/eventos/pedido-validado")
				.contentType(MediaType.APPLICATION_JSON)
				.content(envelope("pedido.validado", pedidoValidadoData(pedidoId))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("VALIDADO"));

		mockMvc.perform(post("/eventos/entrega-aceita")
				.contentType(MediaType.APPLICATION_JSON)
				.content(envelope("entrega.aceita", entregaAceitaData(pedidoId))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ENTREGA_ACEITA"));

		mockMvc.perform(post("/eventos/pedido-retirado")
				.contentType(MediaType.APPLICATION_JSON)
				.content(envelope("pedido.retirado", pedidoRetiradoData(pedidoId))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("EM_ENTREGA"));

		mockMvc.perform(post("/eventos/pedido-entregue")
				.contentType(MediaType.APPLICATION_JSON)
				.content(envelope("pedido.entregue", pedidoEntregueData(pedidoId))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ENTREGUE"));
	}

	@Test
	void pedidoValidadoForaDeOrdemRetorna409() throws Exception {
		Long pedidoId = criarPedido();

		mockMvc.perform(post("/eventos/entrega-aceita")
				.contentType(MediaType.APPLICATION_JSON)
				.content(envelope("entrega.aceita", entregaAceitaData(pedidoId))))
				.andExpect(status().isConflict());
	}

	@Test
	void pedidoValidadoParaPedidoInexistenteRetorna404() throws Exception {
		mockMvc.perform(post("/eventos/pedido-validado")
				.contentType(MediaType.APPLICATION_JSON)
				.content(envelope("pedido.validado", pedidoValidadoData(999999L))))
				.andExpect(status().isNotFound());
	}

	@Test
	void pedidoValidadoComCampoObrigatorioFaltandoRetorna400() throws Exception {
		Long pedidoId = criarPedido();
		String dataSemValorFrete = "{\"pedidoId\":\"" + pedidoId + "\",\"lojaId\":\"7\","
				+ "\"enderecoRetirada\":{\"rua\":\"Rua A, 1\",\"lat\":-23.56,\"lng\":-46.65},"
				+ "\"enderecoEntrega\":{\"rua\":\"Rua B, 2\",\"lat\":-23.5,\"lng\":-46.6},"
				+ "\"tempoPreparoMin\":20}";

		mockMvc.perform(post("/eventos/pedido-validado")
				.contentType(MediaType.APPLICATION_JSON)
				.content(envelope("pedido.validado", dataSemValorFrete)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void pedidoRetiradoDuploRetorna409NaSegundaVez() throws Exception {
		Long pedidoId = criarPedido();
		mockMvc.perform(post("/eventos/pedido-validado")
				.contentType(MediaType.APPLICATION_JSON)
				.content(envelope("pedido.validado", pedidoValidadoData(pedidoId))))
				.andExpect(status().isOk());
		mockMvc.perform(post("/eventos/entrega-aceita")
				.contentType(MediaType.APPLICATION_JSON)
				.content(envelope("entrega.aceita", entregaAceitaData(pedidoId))))
				.andExpect(status().isOk());
		mockMvc.perform(post("/eventos/pedido-retirado")
				.contentType(MediaType.APPLICATION_JSON)
				.content(envelope("pedido.retirado", pedidoRetiradoData(pedidoId))))
				.andExpect(status().isOk());

		mockMvc.perform(post("/eventos/pedido-retirado")
				.contentType(MediaType.APPLICATION_JSON)
				.content(envelope("pedido.retirado", pedidoRetiradoData(pedidoId))))
				.andExpect(status().isConflict());
	}

	@Test
	void appDoEntregadorListaACorridaERetiraOPedido() throws Exception {
		Long pedidoId = criarPedido();
		// id único: o banco H2 é compartilhado entre os testes da classe
		String entregadorId = UUID.randomUUID().toString();
		mockMvc.perform(post("/eventos/pedido-validado")
				.contentType(MediaType.APPLICATION_JSON)
				.content(envelope("pedido.validado", pedidoValidadoData(pedidoId))))
				.andExpect(status().isOk());
		mockMvc.perform(post("/eventos/entrega-aceita")
				.contentType(MediaType.APPLICATION_JSON)
				.content(envelope("entrega.aceita", "{\"pedidoId\":\"" + pedidoId + "\",\"entregaId\":\"88\","
						+ "\"entregadorId\":\"" + entregadorId + "\",\"nomeEntregador\":\"Joao\","
						+ "\"veiculo\":\"moto\",\"etaRetiradaMin\":12}")))
				.andExpect(status().isOk());

		MvcResult corridas = mockMvc.perform(get("/pedidos").param("entregadorId", entregadorId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(pedidoId))
				.andReturn();
		String corrida = corridas.getResponse().getContentAsString();
		String codigo = extrair(corrida, "\"codigoRetirada\":\"(\\d{4})\"");
		String codigoErrado = codigo.equals("0000") ? "1111" : "0000";
		String clienteId = extrair(corrida, "\"clienteId\":(\\d+)");

		mockMvc.perform(post("/pedidos/" + pedidoId + "/retirar")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"entregadorId\":\"outro\",\"codigoRetirada\":\"" + codigo + "\"}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/pedidos/" + pedidoId + "/retirar")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"entregadorId\":\"" + entregadorId + "\"}"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/pedidos/" + pedidoId + "/retirar")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"entregadorId\":\"" + entregadorId + "\",\"codigoRetirada\":\"" + codigoErrado + "\"}"))
				.andExpect(status().isUnprocessableContent());

		MvcResult retirada = mockMvc.perform(post("/pedidos/" + pedidoId + "/retirar")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"entregadorId\":\"" + entregadorId + "\",\"codigoRetirada\":\"" + codigo + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("EM_ENTREGA"))
				.andReturn();

		mockMvc.perform(post("/pedidos/" + pedidoId + "/retirar")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"entregadorId\":\"" + entregadorId + "\",\"codigoRetirada\":\"" + codigo + "\"}"))
				.andExpect(status().isConflict());

		String pin = extrair(retirada.getResponse().getContentAsString(), "\"pin\":\"(\\d{4})\"");
		String pinErrado = pin.equals("0000") ? "1111" : "0000";

		mockMvc.perform(post("/pedidos/" + pedidoId + "/entregar")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"entregadorId\":\"" + entregadorId + "\",\"pin\":\"12\"}"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/pedidos/" + pedidoId + "/entregar")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"entregadorId\":\"" + entregadorId + "\",\"pin\":\"" + pinErrado + "\"}"))
				.andExpect(status().isUnprocessableContent());

		mockMvc.perform(post("/pedidos/" + pedidoId + "/entregar")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"entregadorId\":\"" + entregadorId + "\",\"pin\":\"" + pin + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("EM_ENTREGA"))
				.andExpect(jsonPath("$.entregaConfirmadaPeloEntregador").value(true));

		mockMvc.perform(post("/clientes/999999/pedidos/" + pedidoId + "/confirmar-recebimento"))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/clientes/" + clienteId + "/pedidos/" + pedidoId + "/confirmar-recebimento"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ENTREGUE"))
				.andExpect(jsonPath("$.recebimentoConfirmadoPeloCliente").value(true));
	}

	private String extrair(String texto, String regex) {
		Matcher matcher = Pattern.compile(regex).matcher(texto);
		matcher.find();
		return matcher.group(1);
	}
}
