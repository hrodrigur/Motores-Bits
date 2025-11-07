package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import es.unex.cum.mdai.motoresbits.data.model.entity.Pedido;
import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;
import es.unex.cum.mdai.motoresbits.data.repository.DetallePedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.PedidoRepository;
import es.unex.cum.mdai.motoresbits.support.BaseJpaTest;
import es.unex.cum.mdai.motoresbits.support.TestDataFactory;

/**
 * Pruebas centradas en el repositorio de `Pedido` y en el manejo de líneas (DetallePedido).
 *
 * Verifica consultas por usuario, operaciones JOIN FETCH y el comportamiento de orphanRemoval.
 */
class PedidosRepositoryTest extends BaseJpaTest {

    @Autowired TestDataFactory f;
    @Autowired PedidoRepository pedidoRepo;
    @Autowired DetallePedidoRepository detalleRepo;
    @Autowired PlatformTransactionManager txManager;

    // Crear un pedido con una línea y verificar consultas por usuario y por producto.
    @Test
    void crearPedidoConLinea_y_consultarPorUsuarioYProducto() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Filtros");
        var prod = f.newProductoPersisted(c, "FA", new BigDecimal("12.50"));

        var pedido = f.newPedidoPersisted(u, new BigDecimal("25.00"), EstadoPedido.PENDIENTE);
        pedido.addLinea(prod, 2, prod.getPrecio());
        pedidoRepo.saveAndFlush(pedido);

        var pedidosUsuario = pedidoRepo.findByUsuarioId(u.getId());
        assertThat(pedidosUsuario).hasSize(1);

        var detalle = detalleRepo.findByPedido_IdAndProducto_Id(pedido.getId(), prod.getId());
        assertThat(detalle).isPresent();
        assertThat(detalle.get().getCantidad()).isEqualTo(2);
        assertThat(detalle.get().getPrecio()).isEqualByComparingTo("12.50");
    }

    // Verificar método JOIN FETCH que recupera pedido junto con sus líneas y productos.
    @Test
    void joinFetch_pedidoConLineasYProductos() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Frenos");
        var pr1 = f.newProductoPersisted(c, "PF", new BigDecimal("19.95"));
        var pr2 = f.newProductoPersisted(c, "DF", new BigDecimal("59.90"));

        var pedido = new Pedido();
        pedido.setUsuario(u);
        pedido.setFechaPedido(LocalDate.now());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setTotal(new BigDecimal("79.85"));
        pedido = pedidoRepo.save(pedido);

        pedido.addLinea(pr1, 1, pr1.getPrecio());
        pedido.addLinea(pr2, 1, pr2.getPrecio());
        pedidoRepo.saveAndFlush(pedido);

        var cargado = pedidoRepo.findConLineasYProductos(pedido.getId()).orElseThrow();
        assertThat(cargado.getDetalles()).hasSize(2);
        assertThat(cargado.getDetalles()).allSatisfy(d -> {
            assertThat(d.getProducto()).isNotNull();
            assertThat(d.getPrecio()).isNotNull();
        });
    }

    // Probar orphanRemoval: quitar la línea del pedido debe eliminarla de la BD.
    @Test
    void orphanRemoval_quitarLineaBorraEnBD() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Aceites");
        var p1 = f.newProductoPersisted(c, "OIL", new BigDecimal("25.00"));

        var pedido = f.newPedidoPersisted(u, new BigDecimal("25.00"), EstadoPedido.PENDIENTE);
        var linea = pedido.addLinea(p1, 1, p1.getPrecio());
        pedidoRepo.saveAndFlush(pedido);
        assertThat(detalleRepo.count()).isEqualTo(1);

        pedido.removeLinea(linea);
        pedidoRepo.saveAndFlush(pedido);
        assertThat(detalleRepo.count()).isZero();
    }

    @Test
    void transaccion_rollback_alFallar_dentroDeTransaccionNueva() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("TestTX");
        var prod = f.newProductoPersisted(c, "TXP", new BigDecimal("5.00"));

        // Usamos TransactionTemplate para crear una transacción independiente que falla
        var tt = new TransactionTemplate(txManager);
        tt.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        try {
            tt.executeWithoutResult(status -> {
                var pedido = new Pedido();
                pedido.setUsuario(u);
                pedido.setFechaPedido(LocalDate.now());
                pedido.setEstado(EstadoPedido.PENDIENTE);
                pedido.setTotal(new BigDecimal("5.00"));
                pedido = pedidoRepo.saveAndFlush(pedido);
                pedido.addLinea(prod, 1, prod.getPrecio());
                pedidoRepo.saveAndFlush(pedido);

                // Forzamos fallo para que la transacción nueva haga rollback
                throw new RuntimeException("Forzar rollback de la transacción nueva");
            });
        } catch (RuntimeException ex) {
            // excepción esperada
        }

        // Tras la excepción, la operación en la transacción nueva debe haber sido rollbacked
        var all = pedidoRepo.findByUsuarioId(u.getId());
        assertThat(all).isEmpty();
    }

}
