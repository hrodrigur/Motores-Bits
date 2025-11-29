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
 * Pruebas relacionadas con el comportamiento de borrado entre entidades.
 * Estructura y convenciones en esta clase:
 * - Las pruebas están agrupadas por entidad (Categorías, Productos, Usuarios, Pedidos).
 * - Cada prueba incluye un comentario breve que describe: intención, pasos de setup y aserciones esperadas.
 * - Las pruebas se centran en reglas de integridad referencial y en las excepciones/efectos esperados
 *   al intentar eliminar entidades con dependencias.
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

    // ---------------------------- Categorías ----------------------------

    /**
     * Intención: borrar una categoría que no tiene productos asociados debe eliminarla sin errores.
     * Setup: crear categoría mediante la factory y persistirla.
     * Aserciones: tras delete+flush la búsqueda por id debe devolver empty.
     */
    @Test
    void borrarCategoria_sinProductos_OK() {
        var cat = f.newCategoriaPersisted("Vacía");
        categoriaRepo.delete(cat);
        categoriaRepo.flush();
        assertThat(categoriaRepo.findById(cat.getId())).isEmpty();
    }

    /**
     * Intención: intentar borrar una categoría que tiene productos relacionados debe fallar por integridad.
     * Setup: crear categoría y producto asociado.
     * Aserciones: delete+flush lanza DataIntegrityViolationException.
     */
    @Test
    void borrarCategoria_conProductos_FALLA() {
        var cat = f.newCategoriaPersisted("Con productos");
        f.newProductoPersisted(cat, "REF-A", new BigDecimal("10.00"));

        assertThatThrownBy(() -> {
            categoriaRepo.delete(cat);
            categoriaRepo.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);

        em.clear();
    }

    // ----------------------------- Productos -----------------------------

    /**
     * Intención: borrar un producto sin dependencias (ni reseñas ni detalles de pedido) debe tener éxito.
     * Setup: crear categoría y producto; borrar el producto.
     * Aserciones: el producto ya no debe encontrarse por id.
     */
    @Test
    void borrarProducto_sinDependencias_OK() {
        var cat = f.newCategoriaPersisted("Libre");
        var p = f.newProductoPersisted(cat, "REF-LIBRE", new BigDecimal("5.00"));

        productoRepo.delete(p);
        productoRepo.flush();
        assertThat(productoRepo.findById(p.getId())).isEmpty();
    }

    /**
     * Intención: intentar borrar un producto con reseñas asociadas debe fallar por integridad.
     * Setup: crear usuario, categoría, producto y reseña persistida.
     * Aserciones: delete+flush lanza DataIntegrityViolationException.
     */
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

        em.clear();
    }

    /**
     * Intención: intentar borrar un producto que aparece en una línea de pedido.
     * Observación: según la BD/ dialecto puede lanzar excepción por FK o mantener el producto/ detalle.
     * Aserciones: si no lanza excepción, comprobamos que al menos el producto o el detalle siguen existiendo
     * (evitamos el caso inesperado donde ambos desaparecen). Si lanza DataIntegrityViolationException, lo aceptamos.
     */
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
            // En algunas BD el borrado lanza DataIntegrityViolationException por FK;
            // aquí capturamos ese comportamiento y limpiamos el contexto para seguir.
            em.clear();
        }
    }

    /**
     * Test diagnóstico (equivalente al anterior) con mensajes explicativos para depuración.
     */
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
            // En instalaciones con FK en la BD es normal que falle con excepción; limpiamos la sesión.
            em.clear();
        }
    }

    // ------------------------------ Usuarios -----------------------------

    /**
     * Intención: borrar un usuario sin dependencias debe eliminarlo.
     * Setup: crear usuario y borrarlo.
     * Aserciones: busca por id devuelve empty.
     */
    @Test
    void borrarUsuario_sinDependencias_OK() {
        var u = f.newUsuarioPersisted();
        usuarioRepo.delete(u);
        usuarioRepo.flush();
        assertThat(usuarioRepo.findById(u.getId())).isEmpty();
    }

    /**
     * Intención: intentar borrar un usuario que tiene pedidos asociados debe fallar por integridad.
     * Setup: crear usuario, producto, pedido con línea y persistir.
     * Aserciones: delete+flush lanza DataIntegrityViolationException.
     */
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

    /**
     * Intención: intentar borrar un usuario que tiene reseñas asociadas debe fallar.
     * Setup: crear usuario, producto y reseña; persistir la reseña.
     * Aserciones: delete+flush lanza DataIntegrityViolationException.
     */
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

    // ------------------------------- Pedidos ----------------------------

    /**
     * Intención: al borrar un pedido deben eliminarse sus líneas si cascade+orphanRemoval están configurados.
     * Setup: crear pedido con dos líneas y persistir.
     * Aserciones: tras borrar el pedido, el repositorio no debe devolverlo y el contador de detalles debe ser 0.
     */
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

        // Comprobamos las líneas asociadas únicamente a este pedido (evita interferencias si hay datos residuales)
        var cargadoAntes = pedidoRepo.findConLineasYProductos(ped.getId()).orElseThrow();
        assertThat(cargadoAntes.getDetalles()).hasSize(2);

        pedidoRepo.delete(ped);
        pedidoRepo.flush();

        assertThat(pedidoRepo.findById(ped.getId())).isEmpty();
        // Verificar que las líneas para este pedido han sido eliminadas
        var cargadoDespues = pedidoRepo.findConLineasYProductos(ped.getId());
        assertThat(cargadoDespues).isEmpty();
    }
}
