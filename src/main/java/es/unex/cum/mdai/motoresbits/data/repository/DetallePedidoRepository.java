package es.unex.cum.mdai.motoresbits.data.repository;

import es.unex.cum.mdai.motoresbits.data.model.entity.DetallePedido;
import es.unex.cum.mdai.motoresbits.data.model.entity.DetallePedidoId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, DetallePedidoId> {
}
