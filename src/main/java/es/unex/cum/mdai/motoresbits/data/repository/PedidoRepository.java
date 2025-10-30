package es.unex.cum.mdai.motoresbits.data.repository;

import es.unex.cum.mdai.motoresbits.data.model.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByUsuarioId(Long usuarioId);
}
