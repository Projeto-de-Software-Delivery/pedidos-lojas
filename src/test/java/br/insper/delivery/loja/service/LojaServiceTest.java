package br.insper.delivery.loja.service;

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

import br.insper.delivery.loja.domain.Loja;
import br.insper.delivery.loja.dto.LojaRequest;
import br.insper.delivery.loja.repository.LojaRepository;

@ExtendWith(MockitoExtension.class)
class LojaServiceTest {

	@Mock
	private LojaRepository lojaRepository;

	@InjectMocks
	private LojaService lojaService;

	@Test
	void criarDevePersistirLoja() {
		LojaRequest request = new LojaRequest("Padaria", "12345678000199", "Rua A, 1");
		Loja salva = new Loja("Padaria", "12345678000199", "Rua A, 1");
		when(lojaRepository.save(any(Loja.class))).thenReturn(salva);

		Loja resultado = lojaService.criar(request);

		assertThat(resultado.getCnpj()).isEqualTo("12345678000199");
	}

	@Test
	void buscarPorIdDeveLancarQuandoNaoEncontrado() {
		when(lojaRepository.findById(1L)).thenReturn(Optional.empty());

		org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> lojaService.buscarPorId(1L));
	}

	@Test
	void listarTodasDeveRetornarLojasDoRepositorio() {
		Loja loja = new Loja("Padaria", "12345678000199", "Rua A, 1");
		when(lojaRepository.findAll()).thenReturn(List.of(loja));

		List<Loja> resultado = lojaService.listarTodas();

		assertThat(resultado).hasSize(1);
	}

	@Test
	void atualizarDeveAlterarDadosDaLojaExistente() {
		Loja loja = new Loja("Padaria", "12345678000199", "Rua A, 1");
		LojaRequest request = new LojaRequest("Padaria Nova", "98765432000188", "Rua B, 2");
		when(lojaRepository.findById(1L)).thenReturn(Optional.of(loja));
		when(lojaRepository.save(any(Loja.class))).thenReturn(loja);

		Loja resultado = lojaService.atualizar(1L, request);

		assertThat(resultado.getNome()).isEqualTo("Padaria Nova");
		assertThat(resultado.getCnpj()).isEqualTo("98765432000188");
	}

	@Test
	void atualizarDeveLancarQuandoNaoEncontrado() {
		LojaRequest request = new LojaRequest("Padaria Nova", "98765432000188", "Rua B, 2");
		when(lojaRepository.findById(1L)).thenReturn(Optional.empty());

		org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> lojaService.atualizar(1L, request));
	}

	@Test
	void deletarDeveRemoverLojaExistente() {
		Loja loja = new Loja("Padaria", "12345678000199", "Rua A, 1");
		when(lojaRepository.findById(1L)).thenReturn(Optional.of(loja));

		lojaService.deletar(1L);

		verify(lojaRepository).delete(loja);
	}

	@Test
	void deletarDeveLancarQuandoNaoEncontrado() {
		when(lojaRepository.findById(1L)).thenReturn(Optional.empty());

		org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
				() -> lojaService.deletar(1L));
	}

	@Test
	void criarComAuth0SubPersisteIdentificador() {
		LojaRequest request = new LojaRequest("Pizzaria", "12345678000199", "Rua C, 3");
		Loja salva = new Loja("Pizzaria", "12345678000199", "Rua C, 3", "auth0|loja123");
		when(lojaRepository.findByAuth0Sub("auth0|loja123")).thenReturn(Optional.empty());
		when(lojaRepository.save(any(Loja.class))).thenReturn(salva);

		Loja resultado = lojaService.criar(request, "auth0|loja123");

		assertThat(resultado.getAuth0Sub()).isEqualTo("auth0|loja123");
	}

	@Test
	void criarComAuth0SubJaExistenteLancaConflict() {
		LojaRequest request = new LojaRequest("Pizzaria", "12345678000199", "Rua C, 3");
		Loja existente = new Loja("Outra", "99999999000199", "Rua D, 4", "auth0|loja123");
		when(lojaRepository.findByAuth0Sub("auth0|loja123")).thenReturn(Optional.of(existente));

		ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class,
				() -> lojaService.criar(request, "auth0|loja123")
		);
		assertThat(ex.getStatusCode().value()).isEqualTo(409);
	}

	@Test
	void buscarMeComSubRetornaLoja() {
		Loja loja = new Loja("Pizzaria", "12345678000199", "Rua C, 3", "auth0|loja123");
		when(lojaRepository.findByAuth0Sub("auth0|loja123")).thenReturn(Optional.of(loja));

		org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
				new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
						new br.insper.delivery.config.Auth0User("auth0|loja123", "loja@email.com"),
						"token"
				)
		);
		try {
			Loja resultado = lojaService.buscarMe();
			assertThat(resultado.getNome()).isEqualTo("Pizzaria");
			assertThat(resultado.getAuth0Sub()).isEqualTo("auth0|loja123");
		} finally {
			org.springframework.security.core.context.SecurityContextHolder.clearContext();
		}
	}

	@Test
	void buscarMeNaoAutenticadoLancaUnauthorized() {
		org.springframework.security.core.context.SecurityContextHolder.clearContext();

		ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(
				ResponseStatusException.class,
				() -> lojaService.buscarMe()
		);
		assertThat(ex.getStatusCode().value()).isEqualTo(401);
	}

	@Test
	void buscarMeNaoCadastradoLancaNotFound() {
		when(lojaRepository.findByAuth0Sub("auth0|loja_nova")).thenReturn(Optional.empty());

		org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
				new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
						new br.insper.delivery.config.Auth0User("auth0|loja_nova", "loja@email.com"),
						"token"
				)
		);
		try {
			ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(
					ResponseStatusException.class,
					() -> lojaService.buscarMe()
			);
			assertThat(ex.getStatusCode().value()).isEqualTo(404);
		} finally {
			org.springframework.security.core.context.SecurityContextHolder.clearContext();
		}
	}

	@Test
	void validarAcessoPermiteAcessoParaMesmoUsuario() {
		Loja loja = new Loja("Pizzaria", "12345678000199", "Rua C, 3", "auth0|loja123");
		when(lojaRepository.findById(1L)).thenReturn(Optional.of(loja));

		org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
				new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
						new br.insper.delivery.config.Auth0User("auth0|loja123", "loja@email.com"),
						"token"
				)
		);
		try {
			org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> lojaService.validarAcesso(1L));
		} finally {
			org.springframework.security.core.context.SecurityContextHolder.clearContext();
		}
	}

	@Test
	void validarAcessoLancaForbiddenParaOutroUsuario() {
		Loja loja = new Loja("Pizzaria", "12345678000199", "Rua C, 3", "auth0|loja123");
		when(lojaRepository.findById(1L)).thenReturn(Optional.of(loja));

		org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
				new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
						new br.insper.delivery.config.Auth0User("auth0|outro", "outro@email.com"),
						"token"
				)
		);
		try {
			ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(
					ResponseStatusException.class,
					() -> lojaService.validarAcesso(1L)
			);
			assertThat(ex.getStatusCode().value()).isEqualTo(403);
		} finally {
			org.springframework.security.core.context.SecurityContextHolder.clearContext();
		}
	}

	@Test
	void validarAcessoPermiteAcessoQuandoLojaSemAuth0Sub() {
		Loja loja = new Loja("Padaria", "12345678000199", "Rua A, 1", null);
		when(lojaRepository.findById(1L)).thenReturn(Optional.of(loja));

		org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> lojaService.validarAcesso(1L));
	}
}
