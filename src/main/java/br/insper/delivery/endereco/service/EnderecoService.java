package br.insper.delivery.endereco.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.insper.delivery.cliente.service.ClienteService;
import br.insper.delivery.endereco.domain.Endereco;
import br.insper.delivery.endereco.dto.EnderecoRequest;
import br.insper.delivery.endereco.repository.EnderecoRepository;

/**
 * Serviço para gerenciar endereços de clientes.
 */
@Service
public class EnderecoService {

	private static final Logger log = LoggerFactory.getLogger(EnderecoService.class);

	private final EnderecoRepository enderecoRepository;
	private final ClienteService clienteService;

	public EnderecoService(EnderecoRepository enderecoRepository, ClienteService clienteService) {
		this.enderecoRepository = enderecoRepository;
		this.clienteService = clienteService;
	}

	public Endereco criar(Long clienteId, EnderecoRequest request) {
		clienteService.validarAcesso(clienteId);
		clienteService.buscarPorId(clienteId);
		Endereco endereco = new Endereco(clienteId, request.cep(), request.logradouro(), request.numero(),
				request.complemento(), request.bairro(), request.cidade(), request.estado());
		Endereco salvo = enderecoRepository.save(endereco);
		log.info("Endereco criado: id={}, clienteId={}", salvo.getId(), salvo.getClienteId());
		return salvo;
	}

	public List<Endereco> listarPorCliente(Long clienteId) {
		clienteService.validarAcesso(clienteId);
		clienteService.buscarPorId(clienteId);
		return enderecoRepository.findByClienteId(clienteId);
	}

	/**
	 * 404 tanto se o endereço não existir quanto se existir mas for de outro cliente.
	 */
	public Endereco buscarPorId(Long clienteId, Long id) {
		clienteService.validarAcesso(clienteId);
		clienteService.buscarPorId(clienteId);
		Endereco endereco = enderecoRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Endereço não encontrado"));
		if (!endereco.getClienteId().equals(clienteId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Endereço não encontrado");
		}
		return endereco;
	}

	public Endereco atualizar(Long clienteId, Long id, EnderecoRequest request) {
		Endereco endereco = buscarPorId(clienteId, id);
		endereco.atualizar(request.cep(), request.logradouro(), request.numero(), request.complemento(),
				request.bairro(), request.cidade(), request.estado());
		return enderecoRepository.save(endereco);
	}

	public void deletar(Long clienteId, Long id) {
		Endereco endereco = buscarPorId(clienteId, id);
		enderecoRepository.delete(endereco);
	}
}
