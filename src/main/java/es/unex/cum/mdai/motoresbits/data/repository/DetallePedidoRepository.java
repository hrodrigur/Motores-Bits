package es.unex.cum.mdai.motoresbits.data.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import es.unex.cum.mdai.motoresbits.data.model.entity.DetallePedido;
import es.unex.cum.mdai.motoresbits.data.model.entity.DetallePedidoId;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, DetallePedidoId> {

    boolean existsByProducto_Id(Long productoId);
    Optional<DetallePedido> findByPedido_IdAndProducto_Id(Long pedidoId, Long productoId);
    @Modifying
    @Query("DELETE FROM DetallePedido d " +
            "WHERE d.pedido.id = :pedidoId AND d.producto.id = :productoId")
    void deleteByPedidoAndProducto(@Param("pedidoId") Long pedidoId,
                                   @Param("productoId") Long productoId);
}
