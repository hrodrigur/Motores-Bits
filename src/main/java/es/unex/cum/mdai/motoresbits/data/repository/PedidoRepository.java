package es.unex.cum.mdai.motoresbits.data.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.unex.cum.mdai.motoresbits.data.model.entity.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByUsuarioId(Long usuarioId);

    // Devuelve true si existe al menos un pedido para el usuario dado
    boolean existsByUsuarioId(Long usuarioId);

    @Query("""
           select distinct p
           from Pedido p
           left join fetch p.detalles d
           left join fetch d.producto pr
           where p.id = :id
           """)
    Optional<Pedido> findConLineasYProductos(@Param("id") Long id);
}
