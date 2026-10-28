package br.insper.delivery.loja.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.insper.delivery.loja.dto.LojaRequest;
import br.insper.delivery.loja.dto.LojaResponse;
import br.insper.delivery.loja.service.LojaService;
import jakarta.validation.Valid;

/**
 * Controlador para gerenciar lojas.
 */
@RestController
@RequestMapping("/lojas")
public class LojaController {

	private final LojaService lojaService;

	public LojaController(LojaService lojaService) {
		this.lojaService = lojaService;
	}

	@PostMapping
	public ResponseEntity<LojaResponse> criar(@Valid @RequestBody LojaRequest request) {
		LojaResponse response = LojaResponse.from(lojaService.criar(request));
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/me")
	public ResponseEntity<LojaResponse> buscarMe() {
		return ResponseEntity.ok(LojaResponse.from(lojaService.buscarMe()));
	}

	@GetMapping
	public ResponseEntity<List<LojaResponse>> listarTodas() {
		List<LojaResponse> response = lojaService.listarTodas().stream().map(LojaResponse::from).toList();
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}")
	public ResponseEntity<LojaResponse> buscarPorId(@PathVariable Long id) {
		lojaService.validarAcesso(id);
		return ResponseEntity.ok(LojaResponse.from(lojaService.buscarPorId(id)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<LojaResponse> atualizar(@PathVariable Long id, @Valid @RequestBody LojaRequest request) {
		return ResponseEntity.ok(LojaResponse.from(lojaService.atualizar(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable Long id) {
		lojaService.deletar(id);
		return ResponseEntity.noContent().build();
	}
}
