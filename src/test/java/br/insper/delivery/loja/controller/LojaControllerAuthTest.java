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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import br.insper.delivery.TestJwtHelper;

@SpringBootTest
@AutoConfigureMockMvc
class LojaControllerAuthTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void getMeSemTokenRetorna401() throws Exception {
		mockMvc.perform(get("/lojas/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void getMeComTokenQuandoNaoCadastradoRetorna404() throws Exception {
		String token = TestJwtHelper.createToken("auth0|nova_loja_" + System.currentTimeMillis(), "loja@email.com");

		mockMvc.perform(get("/lojas/me")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void postLojaComTokenSalvaAuth0SubEEPodeSerConsultadaEmMe() throws Exception {
		String sub = "auth0|loja_" + System.currentTimeMillis();
		String token = TestJwtHelper.createToken(sub, "loja@teste.com");

		mockMvc.perform(post("/lojas")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Hamburgueria\",\"cnpj\":\"11222333000144\",\"endereco\":\"Av Paulista, 1000\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nome").value("Hamburgueria"))
				.andExpect(jsonPath("$.cnpj").value("11222333000144"));

		mockMvc.perform(get("/lojas/me")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Hamburgueria"))
				.andExpect(jsonPath("$.cnpj").value("11222333000144"));
	}

	@Test
	void getLojaPorIdComMesmoUsuarioRetorna200EComOutroRetorna403() throws Exception {
		String subDono = "auth0|lojista_" + System.currentTimeMillis();
		String tokenDono = TestJwtHelper.createToken(subDono, "lojista@teste.com");

		MvcResult result = mockMvc.perform(post("/lojas")
				.header("Authorization", "Bearer " + tokenDono)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Sorveteria\",\"cnpj\":\"55666777000188\",\"endereco\":\"Rua Augusta, 50\"}"))
				.andExpect(status().isCreated())
				.andReturn();

		String body = result.getResponse().getContentAsString();
		Matcher matcher = Pattern.compile("\"id\":(\\d+)").matcher(body);
		matcher.find();
		Long lojaId = Long.valueOf(matcher.group(1));

		// Dono acessa com sucesso
		mockMvc.perform(get("/lojas/" + lojaId)
				.header("Authorization", "Bearer " + tokenDono))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Sorveteria"));

		// Outro usuário recebe 403 Forbidden
		String subOutro = "auth0|outro_lojista_" + System.currentTimeMillis();
		String tokenOutro = TestJwtHelper.createToken(subOutro, "outro_lojista@teste.com");

		mockMvc.perform(get("/lojas/" + lojaId)
				.header("Authorization", "Bearer " + tokenOutro))
				.andExpect(status().isForbidden());

		mockMvc.perform(put("/lojas/" + lojaId)
				.header("Authorization", "Bearer " + tokenOutro)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Hack\",\"cnpj\":\"55666777000188\",\"endereco\":\"Rua Invadida, 1\"}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(delete("/lojas/" + lojaId)
				.header("Authorization", "Bearer " + tokenOutro))
				.andExpect(status().isForbidden());
	}
}
