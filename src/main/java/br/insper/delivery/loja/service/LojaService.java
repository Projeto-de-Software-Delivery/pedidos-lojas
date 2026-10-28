package br.insper.delivery.loja.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
		Loja loja = new Loja(request.nome(), request.cnpj(), request.endereco());
		loja.definirPrazos(request.tempoMaxAceiteMin(), request.tempoMaxEntregaMin());
		Loja salva = lojaRepository.save(loja);
		log.info("Loja criada: id={}, cnpj={}", salva.getId(), salva.getCnpj());
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

	public Loja atualizar(Long id, LojaRequest request) {
		Loja loja = buscarPorId(id);
		loja.atualizar(request.nome(), request.cnpj(), request.endereco());
		loja.definirPrazos(request.tempoMaxAceiteMin(), request.tempoMaxEntregaMin());
		return lojaRepository.save(loja);
	}

	public void deletar(Long id) {
		Loja loja = buscarPorId(id);
		lojaRepository.delete(loja);
	}
}
