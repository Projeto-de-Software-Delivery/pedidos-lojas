package br.insper.delivery.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import br.insper.delivery.TestJwtHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class JwtAuthFilterTest {

	private JwtAuthFilter filter;

	@BeforeEach
	void setUp() {
		JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(TestJwtHelper.secretKey()).build();
		filter = new JwtAuthFilter(decoder);
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void parseTokenComJwtValidoExtraiSubEEmail() {
		String token = TestJwtHelper.createToken("auth0|123456", "usuario@teste.com");
		Auth0User user = filter.parseToken(token);

		assertNotNull(user);
		assertEquals("auth0|123456", user.sub());
		assertEquals("usuario@teste.com", user.email());
	}

	@Test
	void parseTokenComAssinaturaInvalidaRetornaNull() {
		// mesmo payload, mas assinado com uma chave diferente da que o decoder espera
		JwtDecoder decoderComOutraChave = NimbusJwtDecoder.withSecretKey(
				new SecretKeySpec("outra-chave-completamente-diferente-da-esperada".getBytes(), "HmacSHA256")).build();
		JwtAuthFilter filterComOutroDecoder = new JwtAuthFilter(decoderComOutraChave);

		String token = TestJwtHelper.createToken("auth0|forjado", "forjado@teste.com");

		assertNull(filterComOutroDecoder.parseToken(token));
	}

	@Test
	void parseTokenComTokenNaoJwtRetornaNull() {
		assertNull(filter.parseToken("auth0|999"));
		assertNull(filter.parseToken("nao-e-um-jwt"));
	}

	@Test
	void parseTokenComNullOuVazioRetornaNull() {
		assertNull(filter.parseToken(null));
		assertNull(filter.parseToken(""));
		assertNull(filter.parseToken("   "));
	}

	@Test
	void doFilterInternalAutenticaComBearerValido() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain chain = mock(FilterChain.class);

		String token = TestJwtHelper.createToken("auth0|user1", "user1@teste.com");
		when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

		filter.doFilterInternal(request, response, chain);

		verify(chain).doFilter(request, response);
		assertNotNull(SecurityContextHolder.getContext().getAuthentication());
		Auth0User principal = (Auth0User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		assertEquals("auth0|user1", principal.sub());
		assertEquals("user1@teste.com", principal.email());
	}

	@Test
	void doFilterInternalSemAuthorizationPassaDiretoSemAutenticar() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain chain = mock(FilterChain.class);

		when(request.getHeader("Authorization")).thenReturn(null);

		filter.doFilterInternal(request, response, chain);

		verify(chain).doFilter(request, response);
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	void doFilterInternalComTokenForjadoPassaDiretoSemAutenticar() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain chain = mock(FilterChain.class);

		when(request.getHeader("Authorization")).thenReturn("Bearer auth0|tentativa-de-bypass");

		filter.doFilterInternal(request, response, chain);

		verify(chain).doFilter(request, response);
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}
}
