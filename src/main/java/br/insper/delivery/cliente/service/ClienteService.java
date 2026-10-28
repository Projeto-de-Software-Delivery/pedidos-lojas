package br.insper.delivery.cliente.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.insper.delivery.cliente.domain.Cliente;
import br.insper.delivery.cliente.dto.ClienteRequest;
import br.insper.delivery.cliente.repository.ClienteRepository;
import br.insper.delivery.config.Auth0User;
import br.insper.delivery.config.SecurityUtils;

/**
 * Serviço para gerenciar clientes.
 */
@Service
public class ClienteService {

	private static final Logger log = LoggerFactory.getLogger(ClienteService.class);

	private final ClienteRepository clienteRepository;

	public ClienteService(ClienteRepository clienteRepository) {
		this.clienteRepository = clienteRepository;
	}

	public Cliente criar(ClienteRequest request) {
		String auth0Sub = SecurityUtils.getCurrentUserSub().orElse(null);
		return criar(request, auth0Sub);
	}

	public Cliente criar(ClienteRequest request, String auth0Sub) {
		if (auth0Sub != null && clienteRepository.findByAuth0Sub(auth0Sub).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Cliente já cadastrado para este usuário");
		}
		Cliente cliente = new Cliente(request.nome(), request.email(), request.telefone(), auth0Sub);
		Cliente salvo = clienteRepository.save(cliente);
		log.info("Cliente criado: id={}, email={}, auth0Sub={}", salvo.getId(), salvo.getEmail(), salvo.getAuth0Sub());
		return salvo;
	}

	public List<Cliente> listarTodos() {
		return clienteRepository.findAll();
	}

	/**
	 * @throws ResponseStatusException 404 se o cliente não existir.
	 */
	public Cliente buscarPorId(Long id) {
		return clienteRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));
	}

	/**
	 * Busca o cliente associado ao sub ou e-mail do usuário autenticado no Auth0.
	 */
	public Cliente buscarMe() {
		Auth0User user = SecurityUtils.getCurrentUser()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado"));

		if (user.sub() != null) {
			Optional<Cliente> porSub = clienteRepository.findByAuth0Sub(user.sub());
			if (porSub.isPresent()) {
				return porSub.get();
			}
		}

		if (user.email() != null) {
			Optional<Cliente> porEmail = clienteRepository.findByEmail(user.email());
			if (porEmail.isPresent()) {
				Cliente cliente = porEmail.get();
				if (cliente.getAuth0Sub() == null && user.sub() != null) {
					cliente.setAuth0Sub(user.sub());
					return clienteRepository.save(cliente);
				}
				return cliente;
			}
		}

		throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado");
	}

	/**
	 * Valida se o cliente pertence ao usuário autenticado (ou se é registro mock/legado).
	 */
	public void validarAcesso(Long id) {
		Cliente cliente = buscarPorId(id);
		String sub = SecurityUtils.getCurrentUserSub().orElse(null);
		if (cliente.getAuth0Sub() != null && !cliente.getAuth0Sub().equals(sub)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: recurso não pertence ao usuário");
		}
	}

	public Cliente atualizar(Long id, ClienteRequest request) {
		validarAcesso(id);
		Cliente cliente = buscarPorId(id);
		cliente.atualizar(request.nome(), request.email(), request.telefone());
		return clienteRepository.save(cliente);
	}

	public void deletar(Long id) {
		validarAcesso(id);
		Cliente cliente = buscarPorId(id);
		clienteRepository.delete(cliente);
	}
}
