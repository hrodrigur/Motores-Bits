package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;
import es.unex.cum.mdai.motoresbits.data.repository.DetallePedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.PedidoRepository;
import es.unex.cum.mdai.motoresbits.support.BaseJpaTest;
import es.unex.cum.mdai.motoresbits.support.TestDataFactory;

class PedidoDetalleRepositoryTest extends BaseJpaTest {

    @Autowired TestDataFactory f;
    @Autowired PedidoRepository pedidoRepository;
    @Autowired DetallePedidoRepository detallePedidoRepository;

    @Test
    void crearPedidoConLineas_y_consultarPorUsuario_y_porPedidoYProducto() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Filtros");
        var prod = f.newProductoPersisted(c, "FA", new BigDecimal("12.50"));

        var pedido = f.newPedidoPersisted(u, new BigDecimal("25.00"), EstadoPedido.PENDIENTE);
        f.addLineaPersisted(pedido, prod, 2, prod.getPrecio());

        var pedidosUsuario = pedidoRepository.findByUsuarioId(u.getId());
        var detalle = detallePedidoRepository.findByPedido_IdAndProducto_Id(
                pedido.getId(), prod.getId()
        );

        assertThat(pedidosUsuario).hasSize(1);
        assertThat(detalle).isPresent();
        assertThat(detalle.get().getCantidad()).isEqualTo(2);
        assertThat(detalle.get().getPrecio()).isEqualByComparingTo("12.50");
    }
}
