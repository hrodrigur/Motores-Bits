package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import es.unex.cum.mdai.motoresbits.data.model.entity.Categoria;
import es.unex.cum.mdai.motoresbits.data.model.entity.Pedido;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.model.entity.Resena;
import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;
import es.unex.cum.mdai.motoresbits.data.repository.CategoriaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.PedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ResenaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;
import es.unex.cum.mdai.motoresbits.support.BaseJpaTest;
import es.unex.cum.mdai.motoresbits.support.TestDataFactory;

/**
 * Conjunto de pruebas relacionadas con los casos de uso del rol ADMIN.
 *
 * Estas pruebas ejercitan las operaciones CRUD y flujos típicos que un
 * administrador necesita: gestión de categorías, productos, pedidos y
 * moderación de reseñas. Los tests usan la factory de test para crear
 * datos y validan tanto persistencia como reglas de negocio básicas.
 */
class AdminUseCasesTest extends BaseJpaTest {

    @Autowired TestDataFactory f;
    @Autowired CategoriaRepository categoriaRepo;
    @Autowired ProductoRepository productoRepo;
    @Autowired PedidoRepository pedidoRepo;
    @Autowired UsuarioRepository usuarioRepo;
    @Autowired ResenaRepository resenaRepo;

    // Prueba: flujo CRUD básico sobre Categoría. Verifica creación, lectura,
    // actualización y eliminación cuando no existen dependencias (productos).
    @Test
    void categoria_crud_basico() {
        // crear
        var cat = new Categoria();
        cat.setNombre("Frenado");
        cat.setDescripcion("Sistema de frenos");
        cat = categoriaRepo.saveAndFlush(cat);

        // leer/listar
        assertThat(categoriaRepo.findAll()).extracting(Categoria::getNombre).contains("Frenado");

        // actualizar
        cat.setNombre("Frenos");
        categoriaRepo.saveAndFlush(cat);
        var reloaded = categoriaRepo.findById(cat.getId()).orElseThrow();
        assertThat(reloaded.getNombre()).isEqualTo("Frenos");

        // eliminar (no hay productos colgando)
        categoriaRepo.delete(reloaded);
        categoriaRepo.flush();
        assertThat(categoriaRepo.findById(cat.getId())).isEmpty();
    }

    // Prueba: gestión de productos. Cubre creación, actualización de precio/stock,
    // listado por categoría y eliminación. Asegura que los repositorios funcionan
    // según contrato y que los campos obligatorios se persistien correctamente.
    @Test
    void producto_crud_y_listado_por_categoria() {
        var cat = f.newCategoriaPersisted("Aceites");

        // crear
        var p = new Producto();
        p.setNombre("Aceite 5W30");
        p.setReferencia("OIL-5W30-ADMIN");
        p.setPrecio(new BigDecimal("29.90"));
        p.setStock(50);
        p.setCategoria(cat);
        p = productoRepo.saveAndFlush(p);

        // actualizar
        p.setPrecio(new BigDecimal("27.50"));
        p.setStock(60);
        productoRepo.saveAndFlush(p);

        var reloaded = productoRepo.findById(p.getId()).orElseThrow();
        assertThat(reloaded.getPrecio()).isEqualByComparingTo("27.50");
        assertThat(reloaded.getStock()).isEqualTo(60);

        // listar por categoría
        var lista = productoRepo.findByCategoriaId(cat.getId());
        assertThat(lista).extracting(Producto::getReferencia).contains("OIL-5W30-ADMIN");

        // eliminar
        productoRepo.delete(reloaded);
        productoRepo.flush();
        assertThat(productoRepo.findById(p.getId())).isEmpty();
    }

    // Prueba: recuperación de pedido con sus líneas y datos de producto (maestro-detalle).
    // Valida que el método con JOIN FETCH devuelve las entidades relacionadas y que
    // las líneas persisten por cascada cuando se añade al pedido.
    @Test
    void pedido_maestro_detalle_con_lineas_y_productos() {
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

        // líneas (por cascada, se persisten al flushear el pedido)
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

    // Prueba: transición de estados de un pedido. Verifica que el flujo PENDIENTE ->
    // PAGADO -> ENVIADO -> ENTREGADO se aplica correctamente y se persiste.
    @Test
    void pedido_actualizar_estado() {
        var u = f.newUsuarioPersisted();
        var pedido = new Pedido();
        pedido.setUsuario(u);
        pedido.setFechaPedido(LocalDate.now());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setTotal(new BigDecimal("0.00"));
        pedido = pedidoRepo.saveAndFlush(pedido);

        // PENDIENTE -> PAGADO
        pedido.setEstado(EstadoPedido.PAGADO);
        pedidoRepo.saveAndFlush(pedido);
        assertThat(pedidoRepo.findById(pedido.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.PAGADO);

        // PAGADO -> ENVIADO
        pedido.setEstado(EstadoPedido.ENVIADO);
        pedidoRepo.saveAndFlush(pedido);
        assertThat(pedidoRepo.findById(pedido.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.ENVIADO);

        // ENVIADO -> ENTREGADO
        pedido.setEstado(EstadoPedido.ENTREGADO);
        pedidoRepo.saveAndFlush(pedido);
        assertThat(pedidoRepo.findById(pedido.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.ENTREGADO);
    }

    // Prueba: moderación de reseñas. Cubre creación, edición y eliminación de una reseña.
    // Útil para verificar que el repositorio de reseñas respeta las restricciones y permite
    // operaciones básicas de moderador.
    @Test
    void moderar_resenas_editar_y_eliminar() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Filtros");
        var prod = f.newProductoPersisted(c, "FA", new BigDecimal("12.50"));

        // crear reseña válida
        var r = new Resena();
        r.setUsuario(u);
        r.setProducto(prod);
        r.setPuntuacion(4);
        r.setComentario("Buena");
        r = resenaRepo.saveAndFlush(r);

        // editar (puntuación y comentario)
        r.setPuntuacion(5);
        r.setComentario("Excelente");
        resenaRepo.saveAndFlush(r);

        var reloaded = resenaRepo.findById(r.getId()).orElseThrow();
        assertThat(reloaded.getPuntuacion()).isEqualTo(5);
        assertThat(reloaded.getComentario()).isEqualTo("Excelente");

        // eliminar
        resenaRepo.delete(reloaded);
        resenaRepo.flush();
        assertThat(resenaRepo.findById(r.getId())).isEmpty();
    }
}
