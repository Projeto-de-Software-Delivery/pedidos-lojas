package br.insper.delivery.loja.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class LojaControllerTest {

	@Autowired
	private MockMvc mockMvc;

	private Long criarLoja() throws Exception {
		MvcResult result = mockMvc
				.perform(post("/lojas")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\":\"Padaria\",\"cnpj\":\"12345678000199\",\"endereco\":\"Rua A, 1\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String body = result.getResponse().getContentAsString();
		Matcher matcher = Pattern.compile("\"id\":(\\d+)").matcher(body);
		matcher.find();
		return Long.valueOf(matcher.group(1));
	}

	@Test
	void criarLojaComDadosValidosRetorna201() throws Exception {
		mockMvc.perform(post("/lojas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Padaria\",\"cnpj\":\"12345678000199\",\"endereco\":\"Rua A, 1\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	void criarLojaSemPrazosUsaOPadrao() throws Exception {
		mockMvc.perform(post("/lojas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Padaria\",\"cnpj\":\"12345678000199\",\"endereco\":\"Rua A, 1\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tempoMaxAceiteMin").value(10))
				.andExpect(jsonPath("$.tempoMaxEntregaMin").value(60));
	}

	@Test
	void criarLojaComPrazosDevolveOsPrazos() throws Exception {
		mockMvc.perform(post("/lojas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Padaria\",\"cnpj\":\"12345678000199\",\"endereco\":\"Rua A, 1\","
						+ "\"tempoMaxAceiteMin\":5,\"tempoMaxEntregaMin\":40}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tempoMaxAceiteMin").value(5))
				.andExpect(jsonPath("$.tempoMaxEntregaMin").value(40));
	}

	@Test
	void criarLojaComPrazoZeradoRetorna400() throws Exception {
		mockMvc.perform(post("/lojas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Padaria\",\"cnpj\":\"12345678000199\",\"endereco\":\"Rua A, 1\","
						+ "\"tempoMaxAceiteMin\":0}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void criarLojaComCnpjInvalidoRetorna400() throws Exception {
		mockMvc.perform(post("/lojas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Padaria\",\"cnpj\":\"123\",\"endereco\":\"Rua A, 1\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void listarTodasRetorna200() throws Exception {
		criarLoja();

		mockMvc.perform(get("/lojas")).andExpect(status().isOk());
	}

	@Test
	void buscarLojaInexistenteRetorna404() throws Exception {
		mockMvc.perform(get("/lojas/999999")).andExpect(status().isNotFound());
	}

	@Test
	void buscarLojaExistenteRetorna200() throws Exception {
		Long id = criarLoja();

		mockMvc.perform(get("/lojas/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cnpj").value("12345678000199"));
	}

	@Test
	void atualizarLojaExistenteRetorna200() throws Exception {
		Long id = criarLoja();

		mockMvc.perform(put("/lojas/" + id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Padaria Nova\",\"cnpj\":\"98765432000188\",\"endereco\":\"Rua B, 2\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Padaria Nova"));
	}

	@Test
	void atualizarLojaInexistenteRetorna404() throws Exception {
		mockMvc.perform(put("/lojas/999999")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Padaria Nova\",\"cnpj\":\"98765432000188\",\"endereco\":\"Rua B, 2\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void deletarLojaExistenteRetorna204() throws Exception {
		Long id = criarLoja();

		mockMvc.perform(delete("/lojas/" + id)).andExpect(status().isNoContent());
	}

	@Test
	void deletarLojaInexistenteRetorna404() throws Exception {
		mockMvc.perform(delete("/lojas/999999")).andExpect(status().isNotFound());
	}
}
