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
import es.unex.cum.mdai.motoresbits.service.exception.ReferenciaProductoDuplicadaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

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
                stock,
                null
        );
    }

    private String emailUnico(String prefijo) {
        return prefijo + "_" + UUID.randomUUID() + "@example.com";
    }

    // ---------- CATEGORÍAS ----------

    @Test
    @DisplayName("crearCategoria debe guardar la categoría con nombre y descripción")
    void crearCategoria_ok() {
        Categoria cat = catalogoService.crearCategoria("Acción", "Juegos de acción y aventura");

        assertNotNull(cat.getId());
        assertEquals("Acción", cat.getNombre());
        assertEquals("Juegos de acción y aventura", cat.getDescripcion());
        assertTrue(categoriaRepository.findById(cat.getId()).isPresent());
    }

    @Test
    @DisplayName("obtenerCategoria debe devolver la categoría existente")
    void obtenerCategoria_existente() {
        Categoria creada = crearCategoria("RPG");
        Categoria recuperada = catalogoService.obtenerCategoria(creada.getId());

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
        Categoria c1 = crearCategoria("Plataformas");
        Categoria c2 = crearCategoria("Deportes");

        List<Categoria> categorias = catalogoService.listarCategorias();

        assertTrue(categorias.size() >= 2);
        assertTrue(categorias.stream().anyMatch(c -> c.getId().equals(c1.getId())));
        assertTrue(categorias.stream().anyMatch(c -> c.getId().equals(c2.getId())));
    }

    @Test
    @DisplayName("editarCategoria debe modificar nombre y descripción")
    void editarCategoria_modificaCampos() {
        Categoria cat = crearCategoria("Indie");
        Long id = cat.getId();

        Categoria editada = catalogoService.editarCategoria(
                id,
                "Indie Actualizado",
                "Descripción actualizada"
        );

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
        Categoria cat = crearCategoria("Puzzle");
        Long id = cat.getId();

        catalogoService.eliminarCategoria(id);

        assertFalse(categoriaRepository.findById(id).isPresent());
    }

    @Test
    @DisplayName("eliminarCategoria de categoría inexistente: puede lanzar o no según implementación")
    void eliminarCategoria_noExiste_comportamientoFlexible() {
        try {
            catalogoService.eliminarCategoria(9999L);
        } catch (CategoriaNoEncontradaException ex) {
            // también válido
        }
    }

    @Test
    @DisplayName("eliminarCategoria con productos: puede lanzar o borrar en cascada según implementación")
    void eliminarCategoria_conProductos_comportamientoFlexible() {
        Categoria cat = crearCategoria("ConProd");
        Producto p = crearProductoEnCategoria(cat, "REF-CONP-1", new BigDecimal("10.00"), 2);

        try {
            catalogoService.eliminarCategoria(cat.getId());
            // Si NO lanza, asumimos que borró en cascada:
            assertFalse(categoriaRepository.findById(cat.getId()).isPresent());
            assertFalse(productoRepository.findById(p.getId()).isPresent());
        } catch (es.unex.cum.mdai.motoresbits.service.exception.CategoriaConProductosException ex) {
            // también válido: tu servicio puede bloquear el borrado
        }
    }

    // ---------- PRODUCTOS ----------

    @Test
    @DisplayName("crearProducto debe crear un producto en la categoría indicada")
    void crearProducto_ok() {
        Categoria cat = crearCategoria("Shooter");

        Producto p = catalogoService.crearProducto(
                cat.getId(),
                "Doom",
                "REF-DOOM-1",
                new BigDecimal("59.99"),
                10,
                null
        );

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
                        3,
                        null
                ));
    }

    @Test
    @DisplayName("obtenerProducto debe devolver el producto existente")
    void obtenerProducto_existente() {
        Categoria cat = crearCategoria("Carreras");
        Producto p = crearProductoEnCategoria(cat, "REF-RACE-1", new BigDecimal("39.99"), 4);

        Producto recuperado = catalogoService.obtenerProducto(p.getId());

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
        Categoria cat = crearCategoria("Simulación");
        Producto p1 = crearProductoEnCategoria(cat, "REF-SIM-1", new BigDecimal("10.00"), 5);
        Producto p2 = crearProductoEnCategoria(cat, "REF-SIM-2", new BigDecimal("20.00"), 3);

        List<Producto> productos = catalogoService.listarProductos();

        assertTrue(productos.size() >= 2);
        assertTrue(productos.stream().anyMatch(p -> p.getId().equals(p1.getId())));
        assertTrue(productos.stream().anyMatch(p -> p.getId().equals(p2.getId())));
    }

    @Test
    @DisplayName("listarPorCategoria debe devolver solo los productos de esa categoría")
    void listarPorCategoria_devuelveSoloDeEsaCategoria() {
        Categoria cat1 = crearCategoria("Lucha");
        Categoria cat2 = crearCategoria("Musical");

        Producto p1 = crearProductoEnCategoria(cat1, "REF-FIGHT-1", new BigDecimal("15.00"), 2);
        crearProductoEnCategoria(cat2, "REF-MUSIC-1", new BigDecimal("25.00"), 1);

        List<Producto> productosLucha = catalogoService.listarPorCategoria(cat1.getId());

        assertFalse(productosLucha.isEmpty());
        assertTrue(productosLucha.stream().allMatch(p -> p.getCategoria().getId().equals(cat1.getId())));
        assertTrue(productosLucha.stream().anyMatch(p -> p.getId().equals(p1.getId())));
    }

    @Test
    @DisplayName("listarPorCategoria en categoría inexistente: lista vacía o excepción según diseño")
    void listarPorCategoria_categoriaNoExiste_comportamientoEsperado() {
        try {
            List<Producto> productos = catalogoService.listarPorCategoria(9999L);
            assertTrue(productos.isEmpty(), "Si no hay categoría, la lista debe estar vacía");
        } catch (CategoriaNoEncontradaException ex) {
            // válido
        }
    }

    @Test
    @DisplayName("editarProducto debe actualizar los datos y la categoría")
    void editarProducto_modificaCamposYCategoria() {
        Categoria cat1 = crearCategoria("Clásicos");
        Categoria cat2 = crearCategoria("Moderno");

        Producto p = crearProductoEnCategoria(cat1, "REF-CLAS-1", new BigDecimal("5.00"), 1);
        Long idProducto = p.getId();

        Producto editado = catalogoService.editarProducto(
                idProducto,
                cat2.getId(),
                "Juego Moderno",
                new BigDecimal("12.50"),
                7,
                null
        );

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
                        1,
                        null
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
                        2,
                        null
                ));
    }

    @Test
    @DisplayName("eliminarProducto sin dependencias debe borrar el producto")
    void eliminarProducto_sinDependencias_ok() {
        Categoria cat = crearCategoria("VR");
        Producto p = crearProductoEnCategoria(cat, "REF-VR-1", new BigDecimal("49.99"), 3);
        Long id = p.getId();

        assertTrue(productoRepository.findById(id).isPresent());
        assertFalse(detallePedidoRepository.existsByProducto_Id(id));
        assertTrue(resenaRepository.findByProductoId(id).isEmpty());

        catalogoService.eliminarProducto(id);

        assertFalse(productoRepository.findById(id).isPresent());
    }

    @Test
    @DisplayName("eliminarProducto inexistente: puede lanzar o no según implementación")
    void eliminarProducto_noExiste_comportamientoFlexible() {
        try {
            catalogoService.eliminarProducto(9999L);
        } catch (ProductoNoEncontradoException ex) {
            // también válido
        }
    }

    @Test
    @DisplayName("eliminarProducto con reseñas: puede lanzar o borrar en cascada según implementación")
    void eliminarProducto_conResenas_comportamientoFlexible() {
        Usuario u = new Usuario();
        u.setNombre("Tester");
        u.setEmail(emailUnico("tester"));
        u.setContrasena("pass");
        usuarioRepository.save(u);

        Categoria cat = crearCategoria("Cat-Resena");
        Producto p = crearProductoEnCategoria(cat, "REF-TEST-R", new BigDecimal("1.00"), 1);

        Resena r = new Resena();
        r.setUsuario(u);
        r.setProducto(p);
        r.setPuntuacion(5);
        r.setComentario("Muy bueno");
        resenaRepository.saveAndFlush(r);

        try {
            catalogoService.eliminarProducto(p.getId());
            // si no lanza, debe haberse borrado el producto (y normalmente también las reseñas)
            assertFalse(productoRepository.findById(p.getId()).isPresent());
        } catch (ProductoConDependenciasException ex) {
            // válido si tu impl bloquea el borrado
        }
    }

    @Test
    @DisplayName("eliminarProducto con detalle de pedido: puede lanzar o borrar según implementación")
    void eliminarProducto_conDetallePedido_comportamientoFlexible() {
        Usuario u = new Usuario();
        u.setNombre("Cliente");
        u.setEmail(emailUnico("cliente"));
        u.setContrasena("pwd");
        usuarioRepository.save(u);

        Categoria cat = crearCategoria("Cat-Detalle");
        Producto p = crearProductoEnCategoria(cat, "REF-TEST-D", new BigDecimal("2.00"), 5);

        Pedido ped = new Pedido();
        ped.setUsuario(u);
        ped.setFechaPedido(java.time.LocalDate.now());
        ped.setEstado(es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido.PENDIENTE);
        ped.setTotal(new BigDecimal("2.00"));
        ped.addLinea(p, 1, p.getPrecio());
        pedidoRepository.saveAndFlush(ped);

        assertTrue(detallePedidoRepository.findByPedido_IdAndProducto_Id(ped.getId(), p.getId()).isPresent());

        try {
            catalogoService.eliminarProducto(p.getId());
            assertFalse(productoRepository.findById(p.getId()).isPresent());
        } catch (ProductoConDependenciasException ex) {
            // válido
        }
    }

    @Test
    @DisplayName("crearProducto con referencia duplicada debe lanzar ReferenciaProductoDuplicadaException")
    void crearProducto_referenciaDuplicada_lanzaExcepcion() {
        Categoria cat = crearCategoria("DupRefCat");

        Producto p1 = crearProductoEnCategoria(cat, "REF-DUP-1", new BigDecimal("10.00"), 5);
        assertNotNull(p1.getId());

        assertThrows(ReferenciaProductoDuplicadaException.class, () -> {
            catalogoService.crearProducto(
                    cat.getId(),
                    "Otro nombre",
                    "REF-DUP-1",
                    new BigDecimal("12.00"),
                    3,
                    null
            );
        });
    }

    @Test
    @DisplayName("eliminarProducto con reseña asociada: puede lanzar o borrar en cascada según implementación")
    void eliminarProducto_conResena_comportamientoFlexible() {
        Categoria cat = crearCategoria("CatRes");
        Producto p = crearProductoEnCategoria(cat, "REF-RES-1", new BigDecimal("5.00"), 10);

        Usuario u = new Usuario();
        u.setNombre("UsuarioRes");
        u.setEmail(emailUnico("res"));
        u.setContrasena("pw");
        usuarioRepository.save(u);

        Resena r = new Resena();
        r.setUsuario(u);
        r.setProducto(p);
        r.setPuntuacion(4);
        r.setComentario("buena");
        resenaRepository.saveAndFlush(r);

        try {
            catalogoService.eliminarProducto(p.getId());
            assertFalse(productoRepository.findById(p.getId()).isPresent());
        } catch (ProductoConDependenciasException ex) {
            // válido
        }
    }
}
