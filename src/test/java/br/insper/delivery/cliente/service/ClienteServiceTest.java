package br.insper.delivery.cliente.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import br.insper.delivery.cliente.domain.Cliente;
import br.insper.delivery.cliente.dto.ClienteRequest;
import br.insper.delivery.cliente.repository.ClienteRepository;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

	@Mock
	private ClienteRepository clienteRepository;

	@InjectMocks
	private ClienteService clienteService;

	@Test
	void criarDevePersistirCliente() {
		ClienteRequest request = new ClienteRequest("Ana", "ana@email.com", "11999999999");
		Cliente salvo = new Cliente("Ana", "ana@email.com", "11999999999");
		when(clienteRepository.save(any(Cliente.class))).thenReturn(salvo);

		Cliente resultado = clienteService.criar(request);

		assertThat(resultado.getEmail()).isEqualTo("ana@email.com");
	}

	@Test
	void buscarPorIdDeveLancarQuandoNaoEncontrado() {
		when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

		org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> clienteService.buscarPorId(1L));
	}

	@Test
	void listarTodosDeveRetornarClientesDoRepositorio() {
		Cliente cliente = new Cliente("Ana", "ana@email.com", "11999999999");
		when(clienteRepository.findAll()).thenReturn(List.of(cliente));

		List<Cliente> resultado = clienteService.listarTodos();

		assertThat(resultado).hasSize(1);
	}

	@Test
	void atualizarDeveAlterarDadosDoClienteExistente() {
		Cliente cliente = new Cliente("Ana", "ana@email.com", "11999999999");
		ClienteRequest request = new ClienteRequest("Ana Souza", "ana.souza@email.com", "11988888888");
		when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
		when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

		Cliente resultado = clienteService.atualizar(1L, request);

		assertThat(resultado.getNome()).isEqualTo("Ana Souza");
		assertThat(resultado.getEmail()).isEqualTo("ana.souza@email.com");
	}

	@Test
	void atualizarDeveLancarQuandoNaoEncontrado() {
		ClienteRequest request = new ClienteRequest("Ana Souza", "ana.souza@email.com", "11988888888");
		when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

		org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> clienteService.atualizar(1L, request));
	}

	@Test
	void deletarDeveRemoverClienteExistente() {
		Cliente cliente = new Cliente("Ana", "ana@email.com", "11999999999");
		when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

		clienteService.deletar(1L);

		verify(clienteRepository).delete(cliente);
	}

	@Test
	void deletarDeveLancarQuandoNaoEncontrado() {
		when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

		org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> clienteService.deletar(1L));
	}

	@Test
	void criarComAuth0SubPersisteIdentificador() {
		ClienteRequest request = new ClienteRequest("Carlos", "carlos@email.com", "11988887777");
		Cliente salvo = new Cliente("Carlos", "carlos@email.com", "11988887777", "auth0|carlos123");
		when(clienteRepository.findByAuth0Sub("auth0|carlos123")).thenReturn(Optional.empty());
		when(clienteRepository.save(any(Cliente.class))).thenReturn(salvo);

		Cliente resultado = clienteService.criar(request, "auth0|carlos123");

		assertThat(resultado.getAuth0Sub()).isEqualTo("auth0|carlos123");
	}

	@Test
	void criarComAuth0SubJaExistenteLancaConflict() {
		ClienteRequest request = new ClienteRequest("Carlos", "carlos@email.com", "11988887777");
		Cliente existente = new Cliente("Outro", "outro@email.com", "11999999999", "auth0|carlos123");
		when(clienteRepository.findByAuth0Sub("auth0|carlos123")).thenReturn(Optional.of(existente));

		ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class,
				() -> clienteService.criar(request, "auth0|carlos123")
		);
		assertThat(ex.getStatusCode().value()).isEqualTo(409);
	}

	@Test
	void buscarMeComSubRetornaCliente() {
		Cliente cliente = new Cliente("Carlos", "carlos@email.com", "11988887777", "auth0|carlos123");
		when(clienteRepository.findByAuth0Sub("auth0|carlos123")).thenReturn(Optional.of(cliente));

		org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
				new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
						new br.insper.delivery.config.Auth0User("auth0|carlos123", "carlos@email.com"),
						"token"
				)
		);
		try {
			Cliente resultado = clienteService.buscarMe();
			assertThat(resultado.getNome()).isEqualTo("Carlos");
			assertThat(resultado.getAuth0Sub()).isEqualTo("auth0|carlos123");
		} finally {
			org.springframework.security.core.context.SecurityContextHolder.clearContext();
		}
	}

	@Test
	void buscarMeComEmailVinculaAuth0SubERetornaCliente() {
		Cliente cliente = new Cliente("Carlos", "carlos@email.com", "11988887777", null);
		when(clienteRepository.findByAuth0Sub("auth0|carlos123")).thenReturn(Optional.empty());
		when(clienteRepository.findByEmail("carlos@email.com")).thenReturn(Optional.of(cliente));
		when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

		org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
				new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
						new br.insper.delivery.config.Auth0User("auth0|carlos123", "carlos@email.com"),
						"token"
				)
		);
		try {
			Cliente resultado = clienteService.buscarMe();
			assertThat(resultado.getNome()).isEqualTo("Carlos");
			assertThat(resultado.getAuth0Sub()).isEqualTo("auth0|carlos123");
		} finally {
			org.springframework.security.core.context.SecurityContextHolder.clearContext();
		}
	}

	@Test
	void buscarMeNaoAutenticadoLancaUnauthorized() {
		org.springframework.security.core.context.SecurityContextHolder.clearContext();

		ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class,
				() -> clienteService.buscarMe()
		);
		assertThat(ex.getStatusCode().value()).isEqualTo(401);
	}

	@Test
	void buscarMeNaoCadastradoLancaNotFound() {
		when(clienteRepository.findByAuth0Sub("auth0|novo")).thenReturn(Optional.empty());
		when(clienteRepository.findByEmail("novo@email.com")).thenReturn(Optional.empty());

		org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
				new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
						new br.insper.delivery.config.Auth0User("auth0|novo", "novo@email.com"),
						"token"
				)
		);
		try {
			ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(
					ResponseStatusException.class,
					() -> clienteService.buscarMe()
			);
			assertThat(ex.getStatusCode().value()).isEqualTo(404);
		} finally {
			org.springframework.security.core.context.SecurityContextHolder.clearContext();
		}
	}

	@Test
	void validarAcessoPermiteAcessoParaMesmoUsuario() {
		Cliente cliente = new Cliente("Carlos", "carlos@email.com", "11988887777", "auth0|carlos123");
		when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

		org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
				new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
						new br.insper.delivery.config.Auth0User("auth0|carlos123", "carlos@email.com"),
						"token"
				)
		);
		try {
			org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> clienteService.validarAcesso(1L));
		} finally {
			org.springframework.security.core.context.SecurityContextHolder.clearContext();
		}
	}

	@Test
	void validarAcessoLancaForbiddenParaOutroUsuario() {
		Cliente cliente = new Cliente("Carlos", "carlos@email.com", "11988887777", "auth0|carlos123");
		when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

		org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
				new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
						new br.insper.delivery.config.Auth0User("auth0|outro", "outro@email.com"),
						"token"
				)
		);
		try {
			ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(
					ResponseStatusException.class,
					() -> clienteService.validarAcesso(1L)
			);
			assertThat(ex.getStatusCode().value()).isEqualTo(403);
		} finally {
			org.springframework.security.core.context.SecurityContextHolder.clearContext();
		}
	}

	@Test
	void validarAcessoPermiteAcessoQuandoClienteSemAuth0Sub() {
		Cliente cliente = new Cliente("Ana", "ana@email.com", "11999999999", null);
		when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

		org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> clienteService.validarAcesso(1L));
	}
}
