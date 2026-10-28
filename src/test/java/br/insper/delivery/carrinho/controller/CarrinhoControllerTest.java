package br.insper.delivery.carrinho.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class CarrinhoControllerTest {

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
						.content("{\"nome\":\"Ana\",\"email\":\"carrinho.teste@email.com\","
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

	private Long criarProduto(String preco) throws Exception {
		Long lojaId = criarLoja();
		MvcResult result = mockMvc
				.perform(post("/produtos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lojaId\":" + lojaId + ",\"nome\":\"Bolo\",\"categoria\":\"Sobremesas\","
								+ "\"preco\":" + preco + ",\"estoque\":100,"
								+ "\"foto\":\"http://exemplo.com/foto.png\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		return extrairId(result);
	}

	@Test
	void buscarCarrinhoVazioRetorna200ComTotalZero() throws Exception {
		Long clienteId = criarCliente();

		mockMvc.perform(get("/clientes/" + clienteId + "/carrinho"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.itens").isEmpty())
				.andExpect(jsonPath("$.total").value(0));
	}

	@Test
	void buscarCarrinhoDeClienteInexistenteRetorna404() throws Exception {
		mockMvc.perform(get("/clientes/999999/carrinho")).andExpect(status().isNotFound());
	}

	@Test
	void adicionarItemComDadosValidosRetorna201ComTotalCalculado() throws Exception {
		Long clienteId = criarCliente();
		Long produtoId = criarProduto("10.00");

		mockMvc.perform(post("/clientes/" + clienteId + "/carrinho/itens")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"produtoId\":" + produtoId + ",\"quantidade\":3}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.itens.length()").value(1))
				.andExpect(jsonPath("$.total").value(30.0));
	}

	@Test
	void adicionarMesmoProdutoDuasVezesDeveSomarQuantidade() throws Exception {
		Long clienteId = criarCliente();
		Long produtoId = criarProduto("10.00");

		mockMvc.perform(post("/clientes/" + clienteId + "/carrinho/itens")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"produtoId\":" + produtoId + ",\"quantidade\":2}"))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/clientes/" + clienteId + "/carrinho/itens")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"produtoId\":" + produtoId + ",\"quantidade\":3}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.itens.length()").value(1))
				.andExpect(jsonPath("$.itens[0].quantidade").value(5))
				.andExpect(jsonPath("$.total").value(50.0));
	}

	@Test
	void adicionarItemParaClienteInexistenteRetorna404() throws Exception {
		Long produtoId = criarProduto("10.00");

		mockMvc.perform(post("/clientes/999999/carrinho/itens")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"produtoId\":" + produtoId + ",\"quantidade\":1}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void adicionarItemComProdutoInexistenteRetorna404() throws Exception {
		Long clienteId = criarCliente();

		mockMvc.perform(post("/clientes/" + clienteId + "/carrinho/itens")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"produtoId\":999999,\"quantidade\":1}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void adicionarItemComQuantidadeInvalidaRetorna400() throws Exception {
		Long clienteId = criarCliente();
		Long produtoId = criarProduto("10.00");

		mockMvc.perform(post("/clientes/" + clienteId + "/carrinho/itens")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"produtoId\":" + produtoId + ",\"quantidade\":0}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void removerItemExistenteRetorna200EAtualizaTotal() throws Exception {
		Long clienteId = criarCliente();
		Long produtoId = criarProduto("10.00");
		MvcResult adicionado = mockMvc
				.perform(post("/clientes/" + clienteId + "/carrinho/itens")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"produtoId\":" + produtoId + ",\"quantidade\":2}"))
				.andExpect(status().isCreated())
				.andReturn();
		String body = adicionado.getResponse().getContentAsString();
		Matcher matcher = Pattern.compile("\"itens\":\\[\\{\"id\":(\\d+)").matcher(body);
		matcher.find();
		Long itemId = Long.valueOf(matcher.group(1));

		mockMvc.perform(delete("/clientes/" + clienteId + "/carrinho/itens/" + itemId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.itens").isEmpty())
				.andExpect(jsonPath("$.total").value(0));
	}

	@Test
	void adicionarItemAcimaDoEstoqueRetorna422() throws Exception {
		Long clienteId = criarCliente();
		Long produtoId = criarProduto("10.00");

		mockMvc.perform(post("/clientes/" + clienteId + "/carrinho/itens")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"produtoId\":" + produtoId + ",\"quantidade\":101}"))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void alterarQuantidadeRetorna200EAtualizaTotal() throws Exception {
		Long clienteId = criarCliente();
		Long produtoId = criarProduto("10.00");
		MvcResult adicionado = mockMvc
				.perform(post("/clientes/" + clienteId + "/carrinho/itens")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"produtoId\":" + produtoId + ",\"quantidade\":2}"))
				.andExpect(status().isCreated())
				.andReturn();
		Matcher matcher = Pattern.compile("\"itens\":\\[\\{\"id\":(\\d+)")
				.matcher(adicionado.getResponse().getContentAsString());
		matcher.find();
		Long itemId = Long.valueOf(matcher.group(1));

		mockMvc.perform(patch("/clientes/" + clienteId + "/carrinho/itens/" + itemId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantidade\":5}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.itens[0].quantidade").value(5))
				.andExpect(jsonPath("$.total").value(50.0));
	}

	@Test
	void removerItemInexistenteRetorna404() throws Exception {
		Long clienteId = criarCliente();

		mockMvc.perform(delete("/clientes/" + clienteId + "/carrinho/itens/999999"))
				.andExpect(status().isNotFound());
	}
}
