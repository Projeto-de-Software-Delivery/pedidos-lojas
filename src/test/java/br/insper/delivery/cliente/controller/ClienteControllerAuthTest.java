package br.insper.delivery.cliente.controller;

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
class ClienteControllerAuthTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void getMeSemTokenRetorna401() throws Exception {
		mockMvc.perform(get("/clientes/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void getMeComTokenQuandoNaoCadastradoRetorna404() throws Exception {
		String token = TestJwtHelper.createToken("auth0|novo_cliente_" + System.currentTimeMillis(), "novo@email.com");

		mockMvc.perform(get("/clientes/me")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void postClienteComTokenSalvaAuth0SubEEPodeSerConsultadoEmMe() throws Exception {
		String sub = "auth0|cliente_" + System.currentTimeMillis();
		String email = "cliente_" + System.currentTimeMillis() + "@teste.com";
		String token = TestJwtHelper.createToken(sub, email);

		mockMvc.perform(post("/clientes")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.format("{\"nome\":\"Maria\",\"email\":\"%s\",\"telefone\":\"11977776666\"}", email)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nome").value("Maria"))
				.andExpect(jsonPath("$.email").value(email));

		mockMvc.perform(get("/clientes/me")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Maria"))
				.andExpect(jsonPath("$.email").value(email));
	}

	@Test
	void getClientePorIdComMesmoUsuarioRetorna200EComOutroRetorna403() throws Exception {
		String subDono = "auth0|dono_" + System.currentTimeMillis();
		String emailDono = "dono_" + System.currentTimeMillis() + "@teste.com";
		String tokenDono = TestJwtHelper.createToken(subDono, emailDono);

		MvcResult result = mockMvc.perform(post("/clientes")
				.header("Authorization", "Bearer " + tokenDono)
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.format("{\"nome\":\"Dono\",\"email\":\"%s\",\"telefone\":\"11977770000\"}", emailDono)))
				.andExpect(status().isCreated())
				.andReturn();

		String body = result.getResponse().getContentAsString();
		Matcher matcher = Pattern.compile("\"id\":(\\d+)").matcher(body);
		matcher.find();
		Long clienteId = Long.valueOf(matcher.group(1));

		// Dono acessa com sucesso
		mockMvc.perform(get("/clientes/" + clienteId)
				.header("Authorization", "Bearer " + tokenDono))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Dono"));

		// Outro usuário recebe 403 Forbidden
		String subOutro = "auth0|intruso_" + System.currentTimeMillis();
		String tokenOutro = TestJwtHelper.createToken(subOutro, "intruso@teste.com");

		mockMvc.perform(get("/clientes/" + clienteId)
				.header("Authorization", "Bearer " + tokenOutro))
				.andExpect(status().isForbidden());

		mockMvc.perform(put("/clientes/" + clienteId)
				.header("Authorization", "Bearer " + tokenOutro)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Hack\",\"email\":\"hack@email.com\",\"telefone\":\"11999990000\"}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(delete("/clientes/" + clienteId)
				.header("Authorization", "Bearer " + tokenOutro))
				.andExpect(status().isForbidden());
	}
}
