package br.insper.delivery.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import br.insper.delivery.TestJwtHelper;

/**
 * Autorização por dono do recurso: cliente e loja criados com um token Auth0 só podem ser
 * operados pelo mesmo usuário (sub); outro usuário (ou requisição sem token) recebe 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwtDecoderConfig.class)
class AutorizacaoPorDonoTest {

	@Autowired
	private MockMvc mockMvc;

	private String tokenCliente;
	private String tokenLoja;
	private String tokenIntruso;
	private Long clienteId;
	private Long lojaId;
	private Long produtoId;

	@BeforeEach
	void criarRecursosComDono() throws Exception {
		String sufixo = UUID.randomUUID().toString();
		tokenCliente = TestJwtHelper.createToken("auth0|cliente_" + sufixo, "cliente_" + sufixo + "@teste.com");
		tokenLoja = TestJwtHelper.createToken("auth0|loja_" + sufixo, "loja_" + sufixo + "@teste.com");
		tokenIntruso = TestJwtHelper.createToken("auth0|intruso_" + sufixo, "intruso_" + sufixo + "@teste.com");

		clienteId = extrairId(mockMvc.perform(comToken(post("/clientes"), tokenCliente)
				.content("{\"nome\":\"Ana\",\"email\":\"cliente_" + sufixo + "@teste.com\",\"telefone\":\"11999999999\"}"))
				.andExpect(status().isCreated()).andReturn());
		lojaId = extrairId(mockMvc.perform(comToken(post("/lojas"), tokenLoja)
				.content("{\"nome\":\"Padaria\",\"cnpj\":\"12345678000199\",\"endereco\":\"Rua A, 1\"}"))
				.andExpect(status().isCreated()).andReturn());
		produtoId = extrairId(mockMvc.perform(comToken(post("/produtos"), tokenLoja).content(produtoJson(100)))
				.andExpect(status().isCreated()).andReturn());
	}

	@Test
	void carrinhoSoDoProprioCliente() throws Exception {
		String item = "{\"produtoId\":" + produtoId + ",\"quantidade\":1}";

		mockMvc.perform(comToken(post("/clientes/" + clienteId + "/carrinho/itens"), tokenCliente).content(item))
				.andExpect(status().isCreated());
		mockMvc.perform(comToken(get("/clientes/" + clienteId + "/carrinho"), tokenCliente))
				.andExpect(status().isOk());

		mockMvc.perform(comToken(get("/clientes/" + clienteId + "/carrinho"), tokenIntruso))
				.andExpect(status().isForbidden());
		mockMvc.perform(comToken(post("/clientes/" + clienteId + "/carrinho/itens"), tokenIntruso).content(item))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/clientes/" + clienteId + "/carrinho"))
				.andExpect(status().isForbidden());
	}

	@Test
	void enderecosSoDoProprioCliente() throws Exception {
		String endereco = "{\"cep\":\"01310100\",\"logradouro\":\"Av Paulista\",\"numero\":\"1000\","
				+ "\"complemento\":\"\",\"bairro\":\"Bela Vista\",\"cidade\":\"São Paulo\",\"estado\":\"SP\"}";

		mockMvc.perform(comToken(post("/clientes/" + clienteId + "/enderecos"), tokenCliente).content(endereco))
				.andExpect(status().isCreated());
		mockMvc.perform(comToken(get("/clientes/" + clienteId + "/enderecos"), tokenIntruso))
				.andExpect(status().isForbidden());
		mockMvc.perform(comToken(post("/clientes/" + clienteId + "/enderecos"), tokenIntruso).content(endereco))
				.andExpect(status().isForbidden());
	}

	@Test
	void pedidosDoClienteELojaSoParaOsDonos() throws Exception {
		String pedido = "{\"lojaId\":" + lojaId + ",\"itens\":[{\"produtoId\":" + produtoId + ",\"quantidade\":1}],"
				+ "\"enderecoEntrega\":{\"rua\":\"Rua B, 2\",\"lat\":-23.5,\"lng\":-46.6}}";

		mockMvc.perform(comToken(post("/clientes/" + clienteId + "/pedidos"), tokenIntruso).content(pedido))
				.andExpect(status().isForbidden());
		Long pedidoId = extrairId(mockMvc.perform(comToken(post("/clientes/" + clienteId + "/pedidos"), tokenCliente)
				.content(pedido)).andExpect(status().isCreated()).andReturn());

		mockMvc.perform(comToken(get("/clientes/" + clienteId + "/pedidos"), tokenIntruso))
				.andExpect(status().isForbidden());
		mockMvc.perform(comToken(get("/lojas/" + lojaId + "/pedidos"), tokenIntruso))
				.andExpect(status().isForbidden());
		mockMvc.perform(comToken(get("/lojas/" + lojaId + "/pedidos/pendentes"), tokenCliente))
				.andExpect(status().isForbidden());
		mockMvc.perform(comToken(post("/lojas/" + lojaId + "/pedidos/" + pedidoId + "/recusar"), tokenIntruso))
				.andExpect(status().isForbidden());
		mockMvc.perform(comToken(post("/clientes/" + clienteId + "/pedidos/" + pedidoId + "/confirmar-recebimento"),
				tokenIntruso)).andExpect(status().isForbidden());

		mockMvc.perform(comToken(get("/lojas/" + lojaId + "/pedidos/pendentes"), tokenLoja))
				.andExpect(status().isOk());
		mockMvc.perform(comToken(post("/lojas/" + lojaId + "/pedidos/" + pedidoId + "/recusar"), tokenLoja))
				.andExpect(status().isOk());
	}

	@Test
	void produtosSoAlteradosPelaLojaDona() throws Exception {
		mockMvc.perform(comToken(post("/produtos"), tokenIntruso).content(produtoJson(5)))
				.andExpect(status().isForbidden());
		mockMvc.perform(comToken(put("/produtos/" + produtoId), tokenIntruso).content(produtoJson(0)))
				.andExpect(status().isForbidden());
		mockMvc.perform(comToken(delete("/produtos/" + produtoId), tokenIntruso))
				.andExpect(status().isForbidden());

		// leitura do catálogo continua aberta a qualquer usuário
		mockMvc.perform(comToken(get("/produtos?lojaId=" + lojaId), tokenIntruso))
				.andExpect(status().isOk());

		mockMvc.perform(comToken(put("/produtos/" + produtoId), tokenLoja).content(produtoJson(50)))
				.andExpect(status().isOk());
		mockMvc.perform(comToken(delete("/produtos/" + produtoId), tokenLoja))
				.andExpect(status().isNoContent());
	}

	private String produtoJson(int estoque) {
		return "{\"lojaId\":" + lojaId + ",\"nome\":\"Bolo\",\"categoria\":\"Sobremesas\","
				+ "\"preco\":15.90,\"estoque\":" + estoque + ",\"foto\":\"http://exemplo.com/foto.png\"}";
	}

	private static MockHttpServletRequestBuilder comToken(MockHttpServletRequestBuilder request, String token) {
		return request.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
	}

	private static Long extrairId(MvcResult result) throws Exception {
		Matcher matcher = Pattern.compile("\"id\":(\\d+)").matcher(result.getResponse().getContentAsString());
		matcher.find();
		return Long.valueOf(matcher.group(1));
	}
}
