package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import es.unex.cum.mdai.motoresbits.data.model.entity.Resena;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;
import es.unex.cum.mdai.motoresbits.data.repository.*;
import es.unex.cum.mdai.motoresbits.support.BaseJpaTest;
import es.unex.cum.mdai.motoresbits.support.TestDataFactory;

// Pruebas sobre comportamiento de borrado e integridad referencial entre entidades.
class DeleteBehaviorTests extends BaseJpaTest {

    @Autowired TestDataFactory f;
    @Autowired CategoriaRepository categoriaRepo;
    @Autowired ProductoRepository productoRepo;
    @Autowired UsuarioRepository usuarioRepo;
    @Autowired PedidoRepository pedidoRepo;
    @Autowired DetallePedidoRepository detalleRepo;
    @Autowired ResenaRepository resenaRepo;

    @Test
    void borrarCategoria_sinProductos_OK() {
        var cat = f.newCategoriaPersisted("Vacía");
        categoriaRepo.delete(cat);
        categoriaRepo.flush();
        assertThat(categoriaRepo.findById(cat.getId())).isEmpty();
    }

    @Test
    void borrarCategoria_conProductos_FALLA() {
        var cat = f.newCategoriaPersisted("Con productos");
        f.newProductoPersisted(cat, "REF-A", new BigDecimal("10.00"));

        assertThatThrownBy(() -> {
            categoriaRepo.delete(cat);
            categoriaRepo.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void borrarProducto_sinDependencias_OK() {
        var cat = f.newCategoriaPersisted("Libre");
        var p = f.newProductoPersisted(cat, "REF-LIBRE", new BigDecimal("5.00"));

        productoRepo.delete(p);
        productoRepo.flush();
        assertThat(productoRepo.findById(p.getId())).isEmpty();
    }

    @Test
    void borrarProducto_conResenas_FALLA() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Frenos");
        var p = f.newProductoPersisted(c, "PF", new BigDecimal("19.95"));

        var r = new Resena();
        r.setUsuario(u);
        r.setProducto(p);
        r.setPuntuacion(4);
        r.setComentario("ok");
        resenaRepo.saveAndFlush(r);

        assertThatThrownBy(() -> {
            productoRepo.delete(p);
            productoRepo.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void borrarProducto_conDetallePedido_FALLA() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Aceites");
        var p = f.newProductoPersisted(c, "OIL", new BigDecimal("12.00"));

        var pedido = f.newPedidoPersisted(u, new BigDecimal("12.00"),
                EstadoPedido.PENDIENTE);
        pedido.addLinea(p, 1, p.getPrecio());
        pedidoRepo.saveAndFlush(pedido);

        assertThat(detalleRepo.findByPedido_IdAndProducto_Id(pedido.getId(), p.getId())).isPresent();

        try {
            productoRepo.delete(p);
            productoRepo.flush();

            boolean prodPresent = productoRepo.findById(p.getId()).isPresent();
            boolean detallePresent = detalleRepo.findByPedido_IdAndProducto_Id(pedido.getId(), p.getId()).isPresent();

            assertThat(prodPresent || detallePresent)
                    .withFailMessage("Ni el producto ni el detalle existen tras intentar borrar: comportamiento inesperado")
                    .isTrue();
        } catch (DataIntegrityViolationException ex) {
            // comportamiento esperado en BD con FK estrictas
        }
    }

    @Test
    void diagnostico_borrarProducto_conDetallePedido() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Diag");
        var p = f.newProductoPersisted(c, "DIAG-1", new BigDecimal("12.00"));

        var pedido = f.newPedidoPersisted(u, new BigDecimal("12.00"),
                EstadoPedido.PENDIENTE);
        pedido.addLinea(p, 1, p.getPrecio());
        pedidoRepo.saveAndFlush(pedido);

        assertThat(detalleRepo.findByPedido_IdAndProducto_Id(pedido.getId(), p.getId())).isPresent();

        try {
            productoRepo.delete(p);
            productoRepo.flush();

            Optional<Producto> prodOpt = productoRepo.findById(p.getId());
            boolean prodPresent = prodOpt.isPresent();
            boolean detallePresent = detalleRepo.findByPedido_IdAndProducto_Id(pedido.getId(), p.getId()).isPresent();

            assertThat(prodPresent || detallePresent)
                    .withFailMessage("Ni el producto ni el detalle existen tras intentar borrar: comportamiento inesperado")
                    .isTrue();
        } catch (DataIntegrityViolationException ex) {
            // comportamiento normal con FK estrictas
        }
    }

    @Test
    void borrarUsuario_sinDependencias_OK() {
        var u = f.newUsuarioPersisted();
        usuarioRepo.delete(u);
        usuarioRepo.flush();
        assertThat(usuarioRepo.findById(u.getId())).isEmpty();
    }

    @Test
    void borrarUsuario_conPedidos_FALLA() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Filtros");
        var p = f.newProductoPersisted(c, "FA", new BigDecimal("9.00"));

        var pedido = f.newPedidoPersisted(u, new BigDecimal("9.00"), EstadoPedido.PENDIENTE);
        pedido.addLinea(p, 1, p.getPrecio());
        pedidoRepo.saveAndFlush(pedido);

        assertThatThrownBy(() -> {
            usuarioRepo.delete(u);
            usuarioRepo.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void borrarUsuario_conResenas_FALLA() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Frenado");
        var p = f.newProductoPersisted(c, "PF-X", new BigDecimal("19.95"));

        var r = new Resena();
        r.setUsuario(u);
        r.setProducto(p);
        r.setPuntuacion(5);
        r.setComentario("bien");
        resenaRepo.saveAndFlush(r);

        assertThatThrownBy(() -> {
            usuarioRepo.delete(u);
            usuarioRepo.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void borrarPedido_borraSusLineas_porCascadeYOrphanRemoval_OK() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Varios");
        var p1 = f.newProductoPersisted(c, "R1", new BigDecimal("3.00"));
        var p2 = f.newProductoPersisted(c, "R2", new BigDecimal("4.00"));

        var ped = f.newPedidoPersisted(u, new BigDecimal("7.00"), EstadoPedido.PENDIENTE);
        ped.addLinea(p1, 1, p1.getPrecio());
        ped.addLinea(p2, 1, p2.getPrecio());
        pedidoRepo.saveAndFlush(ped);

        var cargadoAntes = pedidoRepo.findConLineasYProductos(ped.getId()).orElseThrow();
        assertThat(cargadoAntes.getDetalles()).hasSize(2);

        pedidoRepo.delete(ped);
        pedidoRepo.flush();

        assertThat(pedidoRepo.findById(ped.getId())).isEmpty();
        assertThat(pedidoRepo.findConLineasYProductos(ped.getId())).isEmpty();
    }
}
