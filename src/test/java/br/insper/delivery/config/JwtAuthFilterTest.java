package br.insper.delivery.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import br.insper.delivery.TestJwtHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class JwtAuthFilterTest {

	private JwtAuthFilter filter;

	@BeforeEach
	void setUp() {
		filter = new JwtAuthFilter();
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
	void parseTokenComTokenSimplificadoRetornaSub() {
		Auth0User user = filter.parseToken("auth0|999");

		assertNotNull(user);
		assertEquals("auth0|999", user.sub());
		assertNull(user.email());
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
}
