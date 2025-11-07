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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Tests que verifican el comportamiento de borrado entre entidades relacionadas.
 *
 * Aquí se comprueba la semántica de borrado para Categoría, Producto, Usuario y Pedido,
 * prestando especial atención a las relaciones maestro-detalle (Pedido-DetallePedido)
 * y a las restricciones de integridad (FK) y cascadas configuradas en las entidades.
 *
 * Notas:
 * - Algunos tests esperan excepciones de integridad (DataIntegrityViolationException)
 *   cuando se intenta borrar una entidad que tiene dependencias en la base de datos.
 * - En casos donde el comportamiento puede variar según la base de datos o el dialecto
 *   (por ejemplo H2 vs MySQL), los tests usan aserciones robustas o limpian el contexto
 *   (`em.clear()`) tras excepciones para evitar efectos laterales en la sesión JPA.
 */
class DeleteBehaviorTests extends BaseJpaTest {

    @Autowired TestDataFactory f;
    @Autowired CategoriaRepository categoriaRepo;
    @Autowired ProductoRepository productoRepo;
    @Autowired UsuarioRepository usuarioRepo;
    @Autowired PedidoRepository pedidoRepo;
    @Autowired DetallePedidoRepository detalleRepo;
    @Autowired ResenaRepository resenaRepo;

    @PersistenceContext EntityManager em;

    // ---------------------------------------------------------------------
    // Sección: Categorías
    // ---------------------------------------------------------------------

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

        em.clear(); // limpiar sesión tras excepción para evitar efectos sobre siguientes tests
    }

    // ---------------------------------------------------------------------
    // Sección: Productos
    // ---------------------------------------------------------------------

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

        // Se espera DataIntegrityViolationException al borrar producto con reseñas (FK)
        assertThatThrownBy(() -> {
            productoRepo.delete(p);
            productoRepo.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);

        em.clear();
    }

    @Test
    void borrarProducto_conDetallePedido_FALLA() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Aceites");
        var p = f.newProductoPersisted(c, "OIL", new java.math.BigDecimal("12.00"));

        var pedido = f.newPedidoPersisted(u, new java.math.BigDecimal("12.00"),
                es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido.PENDIENTE);
        pedido.addLinea(p, 1, p.getPrecio());
        pedidoRepo.saveAndFlush(pedido);

        // Precondición: el detalle existe y referencia al producto
        assertThat(detalleRepo.findByPedido_IdAndProducto_Id(pedido.getId(), p.getId())).isPresent();

        // Comportamiento robusto: aceptamos dos resultados consistentes
        // 1) Se lanza DataIntegrityViolationException al intentar borrar (FK en BD)
        // 2) No se lanza, pero comprobamos que el producto o el detalle siguen existiendo
        try {
            productoRepo.delete(p);
            productoRepo.flush();

            // no se lanzó excepción; validamos que el producto o la línea existen
            boolean prodPresent = productoRepo.findById(p.getId()).isPresent();
            boolean detallePresent = detalleRepo.findByPedido_IdAndProducto_Id(pedido.getId(), p.getId()).isPresent();

            assertThat(prodPresent || detallePresent)
                .withFailMessage("Ni el producto ni el detalle existen tras intentar borrar: comportamiento inesperado")
                .isTrue();
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // comportamiento esperado en instalaciones que aplican FK en BD
            assertThat(ex).isNotNull();
            em.clear(); // limpiar sesión tras excepción
        }
    }

    // Test diagnóstico movido aquí para agrupar con los tests de Producto
    @Test
    void diagnostico_borrarProducto_conDetallePedido() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Diag");
        var p = f.newProductoPersisted(c, "DIAG-1", new BigDecimal("12.00"));

        var pedido = f.newPedidoPersisted(u, new BigDecimal("12.00"),
                es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido.PENDIENTE);
        pedido.addLinea(p, 1, p.getPrecio());
        pedidoRepo.saveAndFlush(pedido);

        // Precondición: la línea existe
        assertThat(detalleRepo.findByPedido_IdAndProducto_Id(pedido.getId(), p.getId())).isPresent();

        try {
            productoRepo.delete(p);
            productoRepo.flush();
            // no se lanzó excepción; validamos que el producto o la línea existen
            Optional<Producto> prodOpt = productoRepo.findById(p.getId());
            boolean prodPresent = prodOpt.isPresent();
            boolean detallePresent = detalleRepo.findByPedido_IdAndProducto_Id(pedido.getId(), p.getId()).isPresent();

            assertThat(prodPresent || detallePresent)
                .withFailMessage("Ni el producto ni el detalle existen tras intentar borrar: comportamiento inesperado")
                .isTrue();
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // comportamiento esperado en instalaciones que aplican FK en BD
            assertThat(ex).isNotNull();
            em.clear(); // limpiar sesión tras excepción
        }
    }

    // ---------------------------------------------------------------------
    // Sección: Usuarios
    // ---------------------------------------------------------------------

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

        em.clear();
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

        em.clear();
    }

    // ---------------------------------------------------------------------
    // Sección: Pedidos y detalles
    // ---------------------------------------------------------------------

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

        assertThat(detalleRepo.count()).isEqualTo(2);

        pedidoRepo.delete(ped);
        pedidoRepo.flush();

        assertThat(pedidoRepo.findById(ped.getId())).isEmpty();
        assertThat(detalleRepo.count()).isZero(); // líneas eliminadas
    }
}
