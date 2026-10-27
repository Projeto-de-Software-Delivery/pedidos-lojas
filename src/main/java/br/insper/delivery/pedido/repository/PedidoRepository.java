package br.insper.delivery.pedido.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.insper.delivery.pedido.domain.Pedido;
import br.insper.delivery.pedido.domain.PedidoStatus;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

	List<Pedido> findByClienteId(Long clienteId);

	List<Pedido> findByLojaIdAndStatus(Long lojaId, PedidoStatus status);

	List<Pedido> findByEntregadorId(String entregadorId);
}
