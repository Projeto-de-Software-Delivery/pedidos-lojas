package br.insper.delivery.loja.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.insper.delivery.config.Auth0User;
import br.insper.delivery.config.SecurityUtils;
import br.insper.delivery.loja.domain.Loja;
import br.insper.delivery.loja.dto.LojaRequest;
import br.insper.delivery.loja.repository.LojaRepository;

/**
 * Serviço para gerenciar lojas.
 */
@Service
public class LojaService {

	private static final Logger log = LoggerFactory.getLogger(LojaService.class);

	private final LojaRepository lojaRepository;

	public LojaService(LojaRepository lojaRepository) {
		this.lojaRepository = lojaRepository;
	}

	public Loja criar(LojaRequest request) {
		String auth0Sub = SecurityUtils.getCurrentUserSub().orElse(null);
		return criar(request, auth0Sub);
	}

	public Loja criar(LojaRequest request, String auth0Sub) {
		if (auth0Sub != null && lojaRepository.findByAuth0Sub(auth0Sub).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Loja já cadastrada para este usuário");
		}
		Loja loja = new Loja(request.nome(), request.cnpj(), request.endereco(), auth0Sub);
		Loja salva = lojaRepository.save(loja);
		log.info("Loja criada: id={}, cnpj={}, auth0Sub={}", salva.getId(), salva.getCnpj(), salva.getAuth0Sub());
		return salva;
	}

	public List<Loja> listarTodas() {
		return lojaRepository.findAll();
	}

	/**
	 * @throws ResponseStatusException 404 se a loja não existir.
	 */
	public Loja buscarPorId(Long id) {
		return lojaRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loja não encontrada"));
	}

	/**
	 * Busca a loja pertencente ao usuário logado no Auth0.
	 */
	public Loja buscarMe() {
		Auth0User user = SecurityUtils.getCurrentUser()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado"));

		if (user.sub() != null) {
			return lojaRepository.findByAuth0Sub(user.sub())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loja não encontrada"));
		}

		throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Loja não encontrada");
	}

	/**
	 * Valida se a loja pertence ao usuário autenticado (ou se é registro mock/legado).
	 */
	public void validarAcesso(Long id) {
		Loja loja = buscarPorId(id);
		String sub = SecurityUtils.getCurrentUserSub().orElse(null);
		if (loja.getAuth0Sub() != null && !loja.getAuth0Sub().equals(sub)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: recurso não pertence ao usuário");
		}
	}

	public Loja atualizar(Long id, LojaRequest request) {
		validarAcesso(id);
		Loja loja = buscarPorId(id);
		loja.atualizar(request.nome(), request.cnpj(), request.endereco());
		return lojaRepository.save(loja);
	}

	public void deletar(Long id) {
		validarAcesso(id);
		Loja loja = buscarPorId(id);
		lojaRepository.delete(loja);
	}
}
