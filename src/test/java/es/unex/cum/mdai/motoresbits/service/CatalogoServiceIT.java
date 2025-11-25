package es.unex.cum.mdai.motoresbits.service;

import es.unex.cum.mdai.motoresbits.data.model.entity.Categoria;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.model.entity.Resena;
import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;
import es.unex.cum.mdai.motoresbits.data.model.entity.Pedido;
import es.unex.cum.mdai.motoresbits.data.repository.CategoriaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.DetallePedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ResenaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;
import es.unex.cum.mdai.motoresbits.data.repository.PedidoRepository;
import es.unex.cum.mdai.motoresbits.service.exception.CategoriaNoEncontradaException;
import es.unex.cum.mdai.motoresbits.service.exception.ProductoNoEncontradoException;
import es.unex.cum.mdai.motoresbits.service.exception.ProductoConDependenciasException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogoServiceIT {

    @Autowired
    private CatalogoService catalogoService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private DetallePedidoRepository detallePedidoRepository;

    @Autowired
    private ResenaRepository resenaRepository;

    // ---------- helpers ----------

    private Categoria crearCategoria(String nombre) {
        return catalogoService.crearCategoria(nombre, "Descripción de " + nombre);
    }

    private Producto crearProductoEnCategoria(Categoria categoria,
                                              String referencia,
                                              BigDecimal precio,
                                              int stock) {
        return catalogoService.crearProducto(
                categoria.getId(),
                "Producto " + referencia,
                referencia,
                precio,
                stock
        );
    }

    // ---------- CATEGORÍAS: CREAR / OBTENER / LISTAR / EDITAR / ELIMINAR ----------

    @Test
    @DisplayName("crearCategoria debe guardar la categoría con nombre y descripción")
    void crearCategoria_ok() {
        // when
        Categoria cat = catalogoService.crearCategoria("Acción", "Juegos de acción y aventura");

        // then
        assertNotNull(cat.getId());
        assertEquals("Acción", cat.getNombre());
        assertEquals("Juegos de acción y aventura", cat.getDescripcion());

        assertTrue(categoriaRepository.findById(cat.getId()).isPresent());
    }

    @Test
    @DisplayName("obtenerCategoria debe devolver la categoría existente")
    void obtenerCategoria_existente() {
        // given
        Categoria creada = crearCategoria("RPG");

        // when
        Categoria recuperada = catalogoService.obtenerCategoria(creada.getId());

        // then
        assertEquals(creada.getId(), recuperada.getId());
        assertEquals("RPG", recuperada.getNombre());
    }

    @Test
    @DisplayName("obtenerCategoria debe lanzar CategoriaNoEncontradaException si no existe")
    void obtenerCategoria_noExiste_lanzaExcepcion() {
        assertThrows(CategoriaNoEncontradaException.class,
                () -> catalogoService.obtenerCategoria(9999L));
    }

    @Test
    @DisplayName("listarCategorias debe devolver todas las categorías existentes")
    void listarCategorias_devuelveTodas() {
        // given
        Categoria c1 = crearCategoria("Plataformas");
        Categoria c2 = crearCategoria("Deportes");

        // when
        List<Categoria> categorias = catalogoService.listarCategorias();

        // then
        assertTrue(categorias.size() >= 2);
        assertTrue(categorias.stream().anyMatch(c -> c.getId().equals(c1.getId())));
        assertTrue(categorias.stream().anyMatch(c -> c.getId().equals(c2.getId())));
    }

    @Test
    @DisplayName("editarCategoria debe modificar nombre y descripción")
    void editarCategoria_modificaCampos() {
        // given
        Categoria cat = crearCategoria("Indie");
        Long id = cat.getId();

        // when
        Categoria editada = catalogoService.editarCategoria(
                id,
                "Indie Actualizado",
                "Descripción actualizada"
        );

        // then
        assertEquals(id, editada.getId());
        assertEquals("Indie Actualizado", editada.getNombre());
        assertEquals("Descripción actualizada", editada.getDescripcion());

        Categoria enBD = categoriaRepository.findById(id).orElseThrow();
        assertEquals("Indie Actualizado", enBD.getNombre());
        assertEquals("Descripción actualizada", enBD.getDescripcion());
    }

    @Test
    @DisplayName("editarCategoria debe lanzar CategoriaNoEncontradaException si la categoría no existe")
    void editarCategoria_noExiste_lanzaExcepcion() {
        assertThrows(CategoriaNoEncontradaException.class,
                () -> catalogoService.editarCategoria(12345L, "Nuevo", "Desc"));
    }

    @Test
    @DisplayName("eliminarCategoria sin productos asociados debe borrar la categoría")
    void eliminarCategoria_sinProductos_ok() {
        // given
        Categoria cat = crearCategoria("Puzzle");
        Long id = cat.getId();

        // when
        catalogoService.eliminarCategoria(id);

        // then
        assertFalse(categoriaRepository.findById(id).isPresent());
    }

    @Test
    @DisplayName("eliminarCategoria de categoría inexistente debe lanzar CategoriaNoEncontradaException")
    void eliminarCategoria_noExiste_lanzaExcepcion() {
        assertThrows(CategoriaNoEncontradaException.class,
                () -> catalogoService.eliminarCategoria(9999L));
    }

    // Si en tu implementación eliminas categoría con productos lanzando una excepción,
    // puedes añadir algo como esto (ajustando la excepción concreta):
    //
    // @Test
    // @DisplayName("eliminarCategoria con productos asociados debe fallar")
    // void eliminarCategoria_conProductos_lanzaExcepcion() {
    //     Categoria cat = crearCategoria("Estrategia");
    //     crearProductoEnCategoria(cat, "REF-EST-1", new BigDecimal("10.00"), 5);
    //
    //     assertThrows(CategoriaConDependenciasException.class,
    //             () -> catalogoService.eliminarCategoria(cat.getId()));
    // }

    // ---------- PRODUCTOS: CREAR / OBTENER / LISTAR / EDITAR / ELIMINAR ----------

    @Test
    @DisplayName("crearProducto debe crear un producto en la categoría indicada")
    void crearProducto_ok() {
        // given
        Categoria cat = crearCategoria("Shooter");

        // when
        Producto p = catalogoService.crearProducto(
                cat.getId(),
                "Doom",
                "REF-DOOM-1",
                new BigDecimal("59.99"),
                10
        );

        // then
        assertNotNull(p.getId());
        assertEquals("Doom", p.getNombre());
        assertEquals("REF-DOOM-1", p.getReferencia());
        assertEquals(new BigDecimal("59.99"), p.getPrecio());
        assertEquals(10, p.getStock());
        assertEquals(cat.getId(), p.getCategoria().getId());

        assertTrue(productoRepository.findById(p.getId()).isPresent());
    }

    @Test
    @DisplayName("crearProducto en categoría inexistente debe lanzar CategoriaNoEncontradaException")
    void crearProducto_categoriaNoExiste_lanzaExcepcion() {
        assertThrows(CategoriaNoEncontradaException.class,
                () -> catalogoService.crearProducto(
                        9999L,
                        "Juego Fantasma",
                        "REF-GHOST",
                        new BigDecimal("19.99"),
                        3
                ));
    }

    @Test
    @DisplayName("obtenerProducto debe devolver el producto existente")
    void obtenerProducto_existente() {
        // given
        Categoria cat = crearCategoria("Carreras");
        Producto p = crearProductoEnCategoria(cat, "REF-RACE-1", new BigDecimal("39.99"), 4);

        // when
        Producto recuperado = catalogoService.obtenerProducto(p.getId());

        // then
        assertEquals(p.getId(), recuperado.getId());
        assertEquals("REF-RACE-1", recuperado.getReferencia());
        assertEquals(cat.getId(), recuperado.getCategoria().getId());
    }

    @Test
    @DisplayName("obtenerProducto debe lanzar ProductoNoEncontradoException si no existe")
    void obtenerProducto_noExiste_lanzaExcepcion() {
        assertThrows(ProductoNoEncontradoException.class,
                () -> catalogoService.obtenerProducto(9999L));
    }

    @Test
    @DisplayName("listarProductos debe devolver todos los productos")
    void listarProductos_devuelveTodos() {
        // given
        Categoria cat = crearCategoria("Simulación");
        Producto p1 = crearProductoEnCategoria(cat, "REF-SIM-1", new BigDecimal("10.00"), 5);
        Producto p2 = crearProductoEnCategoria(cat, "REF-SIM-2", new BigDecimal("20.00"), 3);

        // when
        List<Producto> productos = catalogoService.listarProductos();

        // then
        assertTrue(productos.size() >= 2);
        assertTrue(productos.stream().anyMatch(p -> p.getId().equals(p1.getId())));
        assertTrue(productos.stream().anyMatch(p -> p.getId().equals(p2.getId())));
    }

    @Test
    @DisplayName("listarPorCategoria debe devolver solo los productos de esa categoría")
    void listarPorCategoria_devuelveSoloDeEsaCategoria() {
        // given
        Categoria cat1 = crearCategoria("Lucha");
        Categoria cat2 = crearCategoria("Musical");

        Producto p1 = crearProductoEnCategoria(cat1, "REF-FIGHT-1", new BigDecimal("15.00"), 2);
        crearProductoEnCategoria(cat2, "REF-MUSIC-1", new BigDecimal("25.00"), 1);

        // when
        List<Producto> productosLucha = catalogoService.listarPorCategoria(cat1.getId());

        // then
        assertFalse(productosLucha.isEmpty());
        assertTrue(productosLucha.stream().allMatch(p -> p.getCategoria().getId().equals(cat1.getId())));
        assertTrue(productosLucha.stream().anyMatch(p -> p.getId().equals(p1.getId())));
    }

    @Test
    @DisplayName("listarPorCategoria en categoría inexistente debería devolver lista vacía o lanzar excepción según tu diseño")
    void listarPorCategoria_categoriaNoExiste_comportamientoEsperado() {
        // Aquí hay dos posibles diseños razonables:
        // 1) Devolver lista vacía.
        // 2) Lanzar CategoriaNoEncontradaException.
        //
        // Aceptamos ambos comportamientos: si el servicio lanza la excepción
        // la prueba pasa; si devuelve una lista, comprobamos que está vacía.
        try {
            List<Producto> productos = catalogoService.listarPorCategoria(9999L);
            assertTrue(productos.isEmpty(), "Si no hay categoría, la lista debe estar vacía");
        } catch (CategoriaNoEncontradaException ex) {
            // comportamiento alternativo válido
        }
    }

    @Test
    @DisplayName("editarProducto debe actualizar los datos y la categoría")
    void editarProducto_modificaCamposYCategoria() {
        // given
        Categoria cat1 = crearCategoria("Clásicos");
        Categoria cat2 = crearCategoria("Moderno");

        Producto p = crearProductoEnCategoria(cat1, "REF-CLAS-1", new BigDecimal("5.00"), 1);
        Long idProducto = p.getId();

        // when
        Producto editado = catalogoService.editarProducto(
                idProducto,
                cat2.getId(),
                "Juego Moderno",
                new BigDecimal("12.50"),
                7
        );

        // then
        assertEquals(idProducto, editado.getId());
        assertEquals("Juego Moderno", editado.getNombre());
        assertEquals(new BigDecimal("12.50"), editado.getPrecio());
        assertEquals(7, editado.getStock());
        assertEquals(cat2.getId(), editado.getCategoria().getId());

        Producto enBD = productoRepository.findById(idProducto).orElseThrow();
        assertEquals("Juego Moderno", enBD.getNombre());
        assertEquals(cat2.getId(), enBD.getCategoria().getId());
    }

    @Test
    @DisplayName("editarProducto de producto inexistente debe lanzar ProductoNoEncontradoException")
    void editarProducto_productoNoExiste_lanzaExcepcion() {
        Categoria cat = crearCategoria("Arcade");

        assertThrows(ProductoNoEncontradoException.class,
                () -> catalogoService.editarProducto(
                        9999L,
                        cat.getId(),
                        "No existe",
                        new BigDecimal("9.99"),
                        1
                ));
    }

    @Test
    @DisplayName("editarProducto con categoría inexistente debe lanzar CategoriaNoEncontradaException")
    void editarProducto_categoriaNoExiste_lanzaExcepcion() {
        Categoria cat = crearCategoria("Sandbox");
        Producto p = crearProductoEnCategoria(cat, "REF-SB-1", new BigDecimal("30.00"), 2);

        assertThrows(CategoriaNoEncontradaException.class,
                () -> catalogoService.editarProducto(
                        p.getId(),
                        9999L,
                        "Juego Sandbox",
                        new BigDecimal("30.00"),
                        2
                ));
    }

    @Test
    @DisplayName("eliminarProducto sin dependencias debe borrar el producto")
    void eliminarProducto_sinDependencias_ok() {
        // given
        Categoria cat = crearCategoria("VR");
        Producto p = crearProductoEnCategoria(cat, "REF-VR-1", new BigDecimal("49.99"), 3);
        Long id = p.getId();

        // sanity
        assertTrue(productoRepository.findById(id).isPresent());
        assertFalse(detallePedidoRepository.existsByProducto_Id(id));
        assertTrue(resenaRepository.findByProductoId(id).isEmpty());

        // when
        catalogoService.eliminarProducto(id);

        // then
        assertFalse(productoRepository.findById(id).isPresent());
    }

    @Test
    @DisplayName("eliminarProducto de producto inexistente debe lanzar ProductoNoEncontradoException")
    void eliminarProducto_noExiste_lanzaExcepcion() {
        assertThrows(ProductoNoEncontradoException.class,
                () -> catalogoService.eliminarProducto(9999L));
    }

    // NOTA: para probar ProductoConDependenciasException haría falta crear un DetallePedido
    // o una Resena asociada al producto. Depende mucho de cómo sean tus entidades Pedido,
    // DetallePedido y Resena, así que ese test lo dejaría para cuando tengamos esos modelos
    // delante.
    //
    // Ejemplo de esquema (NO COMPILA tal cual, ajústalo a tu modelo real):
    //
    // @Test
    // @DisplayName("eliminarProducto con detalles de pedido o reseñas debe lanzar ProductoConDependenciasException")
    // void eliminarProducto_conDependencias_lanzaExcepcion() {
    //     Categoria cat = crearCategoria("Otros");
    //     Producto p = crearProductoEnCategoria(cat, "REF-DEP-1", new BigDecimal("10.00"), 1);
    //
    //     // crear aquí un DetallePedido o Resena que apunte a p y guardarlo en su repo
    //
    //     assertThrows(ProductoConDependenciasException.class,
    //             () -> catalogoService.eliminarProducto(p.getId()));
    // }

    // ------------------- Tests adicionales sobre borrado con dependencias -------------------

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    /**
     * Test que verifica que eliminarProducto lanza ProductoConDependenciasException
     * cuando existe al menos una reseña asociada al producto.
     * Explicación: el servicio debe impedir borrar un producto si hay reseñas
     * para evitar inconsistencias (la entidad Resena apunta por FK a Producto).
     * El test crea un usuario (necesario para la reseña), una categoría y un producto,
     * persiste una reseña y luego comprueba que el intento de borrado por parte
     * del servicio lanza la excepción de negocio adecuada.
     */
    @Test
    @DisplayName("eliminarProducto debe lanzar ProductoConDependenciasException si hay reseñas")
    void eliminarProducto_conResenas_lanzaExcepcion() {
        // given: usuario, categoría y producto
        Usuario u = new Usuario();
        u.setNombre("Tester");
        u.setEmail("tester@example.com");
        u.setContrasena("pass");
        usuarioRepository.save(u);

        Categoria cat = crearCategoria("Cat-Resena");
        Producto p = crearProductoEnCategoria(cat, "REF-TEST-R", new BigDecimal("1.00"), 1);

        // and: una reseña asociada al producto
        Resena r = new Resena();
        r.setUsuario(u);
        r.setProducto(p);
        r.setPuntuacion(5);
        r.setComentario("Muy bueno");
        resenaRepository.saveAndFlush(r);

        // when/then: intentar eliminar debe lanzar la excepción de negocio
        assertThrows(ProductoConDependenciasException.class,
                () -> catalogoService.eliminarProducto(p.getId()));
    }

    /**
     * Test que verifica que eliminarProducto lanza ProductoConDependenciasException
     * cuando existe al menos una línea de pedido asociada al producto.
     * Explicación: si hay detalles de pedidos referenciando el producto, el
     * servicio debe impedir su eliminación y lanzar ProductoConDependenciasException.
     * Para crear la línea de pedido usamos directamente la entidad Pedido y
     * persistimos la relación; luego comprobamos la excepción.
     */
    @Test
    @DisplayName("eliminarProducto debe lanzar ProductoConDependenciasException si hay detalle de pedido")
    void eliminarProducto_conDetallePedido_lanzaExcepcion() {
        // given
        Usuario u = new Usuario();
        u.setNombre("Cliente");
        u.setEmail("cliente@example.com");
        u.setContrasena("pwd");
        usuarioRepository.save(u);

        Categoria cat = crearCategoria("Cat-Detalle");
        Producto p = crearProductoEnCategoria(cat, "REF-TEST-D", new BigDecimal("2.00"), 5);

        // and: crear un pedido con una línea que apunte al producto
        Pedido ped = new Pedido();
        ped.setUsuario(u);
        ped.setFechaPedido(java.time.LocalDate.now());
        ped.setEstado(es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido.PENDIENTE);
        ped.setTotal(new BigDecimal("2.00"));
        ped.addLinea(p, 1, p.getPrecio());
        pedidoRepository.saveAndFlush(ped);

        // precondition sanity: existe el detalle
        var found = detallePedidoRepository.findByPedido_IdAndProducto_Id(ped.getId(), p.getId());
        assertTrue(found.isPresent());

        // when/then
        assertThrows(ProductoConDependenciasException.class,
                () -> catalogoService.eliminarProducto(p.getId()));
    }

    @Test
    @DisplayName("crearProducto con referencia duplicada debe lanzar DataIntegrityViolationException")
    void crearProducto_referenciaDuplicada_lanzaExcepcion() {
        Categoria cat = crearCategoria("DupRefCat");

        // crear primer producto
        Producto p1 = crearProductoEnCategoria(cat, "REF-DUP-1", new BigDecimal("10.00"), 5);
        assertNotNull(p1.getId());

        // intentar crear segundo producto con misma referencia
        assertThrows(DataIntegrityViolationException.class, () -> {
            // dependiendo de la implementación, la excepción puede lanzarse por el repositorio
            catalogoService.crearProducto(cat.getId(), "Otro nombre", "REF-DUP-1", new BigDecimal("12.00"), 3);
        });
    }

}
