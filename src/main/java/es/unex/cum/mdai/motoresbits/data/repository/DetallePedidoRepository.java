package es.unex.cum.mdai.motoresbits.data.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import es.unex.cum.mdai.motoresbits.data.model.entity.DetallePedido;
import es.unex.cum.mdai.motoresbits.data.model.entity.DetallePedidoId;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, DetallePedidoId> {

    boolean existsByProducto_Id(Long productoId);
    Optional<DetallePedido> findByPedido_IdAndProducto_Id(Long pedidoId, Long productoId);
}
