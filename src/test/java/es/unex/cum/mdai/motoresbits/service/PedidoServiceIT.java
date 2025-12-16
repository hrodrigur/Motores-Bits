package es.unex.cum.mdai.motoresbits.service;

import es.unex.cum.mdai.motoresbits.data.model.entity.*;
import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;
import es.unex.cum.mdai.motoresbits.data.model.enums.RolUsuario;
import es.unex.cum.mdai.motoresbits.data.repository.*;
import es.unex.cum.mdai.motoresbits.service.exception.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PedidoServiceIT {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private DetallePedidoRepository detallePedidoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    // ---------- helpers ----------

    private String emailUnico(String prefijo) {
        return prefijo + "_" + UUID.randomUUID() + "@example.com";
    }

    private Usuario crearUsuarioCliente(String email) {
        Usuario u = new Usuario();
        u.setNombre("Cliente " + email);
        u.setEmail(email);
        u.setContrasena("1234");
        u.setRol(RolUsuario.CLIENTE);
        // saldo por defecto null/0 en tu lógica, lo setean los tests cuando lo necesitan
        return usuarioRepository.save(u);
    }

    private Producto crearProducto(String ref, BigDecimal precio) {
        Categoria cat = new Categoria();
        cat.setNombre("Cat-" + ref);
        cat.setDescripcion("Categoría para " + ref);
        categoriaRepository.save(cat);

        Producto p = new Producto();
        p.setCategoria(cat);
        p.setNombre("Producto " + ref);
        p.setReferencia(ref);
        p.setPrecio(precio);
        p.setStock(100);
        return productoRepository.save(p);
    }

    // ---------- CREAR / OBTENER ----------

    @Test
    @DisplayName("crearPedido debe crear un pedido CREADO con total 0 para un usuario existente")
    void crearPedido_usuarioExistente() {
        Usuario u = crearUsuarioCliente(emailUnico("pedido_user"));

        Pedido pedido = pedidoService.crearPedido(u.getId());

        assertNotNull(pedido.getId());
        assertEquals(u.getId(), pedido.getUsuario().getId());
        assertEquals(EstadoPedido.CREADO, pedido.getEstado());
        assertEquals(BigDecimal.ZERO, pedido.getTotal());
        assertEquals(LocalDate.now(), pedido.getFechaPedido());
    }

    @Test
    @DisplayName("crearPedido debe lanzar UsuarioNoEncontradoException si el usuario no existe")
    void crearPedido_usuarioNoExiste_lanzaExcepcion() {
        assertThrows(UsuarioNoEncontradoException.class,
                () -> pedidoService.crearPedido(9999L));
    }

    @Test
    @DisplayName("obtenerPedido debe devolver el pedido con sus líneas y productos")
    void obtenerPedido_conLineasYProductos() {
        Usuario u = crearUsuarioCliente(emailUnico("pedido_lineas"));
        Producto p = crearProducto("REF-PED-1", new BigDecimal("10.00"));

        Pedido pedido = new Pedido();
        pedido.setUsuario(u);
        pedido.setFechaPedido(LocalDate.now());
        pedido.setEstado(EstadoPedido.CREADO);
        pedido.setTotal(BigDecimal.ZERO);
        pedido.addLinea(p, 2, p.getPrecio());
        pedido = pedidoRepository.save(pedido);

        Pedido recuperado = pedidoService.obtenerPedido(pedido.getId());

        assertEquals(pedido.getId(), recuperado.getId());
        assertNotNull(recuperado.getDetalles());
        assertEquals(1, recuperado.getDetalles().size());

        DetallePedido detalle = recuperado.getDetalles().iterator().next();
        assertEquals(p.getId(), detalle.getProducto().getId());
        assertEquals(2, detalle.getCantidad());
    }

    @Test
    @DisplayName("obtenerPedido debe lanzar PedidoNoEncontradoException si no existe")
    void obtenerPedido_noExiste_lanzaExcepcion() {
        assertThrows(PedidoNoEncontradoException.class,
                () -> pedidoService.obtenerPedido(9999L));
    }

    // ---------- AGREGAR / CAMBIAR / ELIMINAR LÍNEAS (sin cambios) ----------

    @Test
    @DisplayName("agregarLinea debe crear una línea nueva y actualizar el total")
    void agregarLinea_creaLineaYActualizaTotal() {
        Usuario u = crearUsuarioCliente(emailUnico("linea_nueva"));
        Producto p = crearProducto("REF-LIN-1", new BigDecimal("15.50"));
        Pedido pedido = pedidoService.crearPedido(u.getId());

        Pedido actualizado = pedidoService.agregarLinea(pedido.getId(), p.getId(), 3);

        assertEquals(1, actualizado.getDetalles().size());
        DetallePedido det = actualizado.getDetalles().iterator().next();
        assertEquals(3, det.getCantidad());
        assertEquals(new BigDecimal("15.50"), det.getPrecio());
        assertEquals(new BigDecimal("46.50"), actualizado.getTotal());
    }

    @Test
    @DisplayName("agregarLinea sobre misma línea debe sumar cantidades y recalcular total")
    void agregarLinea_mismaLinea_sumaCantidad() {
        Usuario u = crearUsuarioCliente(emailUnico("linea_sumar"));
        Producto p = crearProducto("REF-LIN-2", new BigDecimal("20.00"));
        Pedido pedido = pedidoService.crearPedido(u.getId());

        pedidoService.agregarLinea(pedido.getId(), p.getId(), 1);

        Pedido actualizado = pedidoService.agregarLinea(pedido.getId(), p.getId(), 2);

        assertEquals(1, actualizado.getDetalles().size());
        DetallePedido det = actualizado.getDetalles().iterator().next();
        assertEquals(3, det.getCantidad());
        assertEquals(new BigDecimal("60.00"), actualizado.getTotal());
    }

    @Test
    @DisplayName("agregarLinea con cantidad <= 0 debe lanzar IllegalArgumentException")
    void agregarLinea_cantidadNoValida() {
        Usuario u = crearUsuarioCliente(emailUnico("linea_invalida"));
        Producto p = crearProducto("REF-LIN-3", new BigDecimal("5.00"));
        Pedido pedido = pedidoService.crearPedido(u.getId());

        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.agregarLinea(pedido.getId(), p.getId(), 0));
        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.agregarLinea(pedido.getId(), p.getId(), -1));
    }

    @Test
    @DisplayName("cambiarCantidadLinea debe modificar la cantidad y el total")
    void cambiarCantidadLinea_modificaCantidadYTotal() {
        Usuario u = crearUsuarioCliente(emailUnico("linea_cambiar"));
        Producto p = crearProducto("REF-LIN-4", new BigDecimal("10.00"));
        Pedido pedido = pedidoService.crearPedido(u.getId());
        pedidoService.agregarLinea(pedido.getId(), p.getId(), 2);

        Pedido actualizado = pedidoService.cambiarCantidadLinea(pedido.getId(), p.getId(), 5);

        DetallePedido det = actualizado.getDetalles().iterator().next();
        assertEquals(5, det.getCantidad());
        assertEquals(new BigDecimal("50.00"), actualizado.getTotal());
    }

    @Test
    @DisplayName("cambiarCantidadLinea a 0 debe eliminar la línea y poner el total a 0")
    void cambiarCantidadLinea_aCero_eliminaLinea() {
        Usuario u = crearUsuarioCliente(emailUnico("linea_cero"));
        Producto p = crearProducto("REF-LIN-5", new BigDecimal("7.00"));
        Pedido pedido = pedidoService.crearPedido(u.getId());
        pedidoService.agregarLinea(pedido.getId(), p.getId(), 2);

        Pedido actualizado = pedidoService.cambiarCantidadLinea(pedido.getId(), p.getId(), 0);

        assertTrue(actualizado.getDetalles() == null || actualizado.getDetalles().isEmpty());
        assertEquals(BigDecimal.ZERO, actualizado.getTotal());
    }

    @Test
    @DisplayName("cambiarCantidadLinea con cantidad negativa debe lanzar IllegalArgumentException")
    void cambiarCantidadLinea_cantidadNegativa() {
        Usuario u = crearUsuarioCliente(emailUnico("linea_negativa"));
        Producto p = crearProducto("REF-LIN-6", new BigDecimal("8.00"));
        Pedido pedido = pedidoService.crearPedido(u.getId());
        pedidoService.agregarLinea(pedido.getId(), p.getId(), 1);

        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.cambiarCantidadLinea(pedido.getId(), p.getId(), -3));
    }

    @Test
    @DisplayName("cambiarCantidadLinea en línea inexistente debe lanzar LineaPedidoNoEncontradaException")
    void cambiarCantidadLinea_lineaNoExiste() {
        Usuario u = crearUsuarioCliente(emailUnico("linea_noexiste"));
        Producto p = crearProducto("REF-LIN-7", new BigDecimal("9.00"));
        Pedido pedido = pedidoService.crearPedido(u.getId());

        assertThrows(LineaPedidoNoEncontradaException.class,
                () -> pedidoService.cambiarCantidadLinea(pedido.getId(), p.getId(), 1));
    }

    @Test
    @DisplayName("eliminarLinea debe quitar la línea del pedido y actualizar el total")
    void eliminarLinea_ok() {
        Usuario u = crearUsuarioCliente(emailUnico("linea_eliminar"));
        Producto p = crearProducto("REF-LIN-8", new BigDecimal("11.00"));
        Pedido pedido = pedidoService.crearPedido(u.getId());
        pedidoService.agregarLinea(pedido.getId(), p.getId(), 2);

        Pedido actualizado = pedidoService.eliminarLinea(pedido.getId(), p.getId());

        assertTrue(actualizado.getDetalles() == null || actualizado.getDetalles().isEmpty());
        assertEquals(BigDecimal.ZERO, actualizado.getTotal());
    }

    @Test
    @DisplayName("eliminarLinea de línea inexistente debe lanzar LineaPedidoNoEncontradaException")
    void eliminarLinea_lineaNoExiste() {
        Usuario u = crearUsuarioCliente(emailUnico("linea_noexiste2"));
        Producto p = crearProducto("REF-LIN-9", new BigDecimal("3.00"));
        Pedido pedido = pedidoService.crearPedido(u.getId());

        assertThrows(LineaPedidoNoEncontradaException.class,
                () -> pedidoService.eliminarLinea(pedido.getId(), p.getId()));
    }

    // ---------- CAMBIAR ESTADO (ajustado a TU validador) ----------

    @Test
    @DisplayName("cambiarEstado debe permitir transiciones válidas (CREADO->PENDIENTE->PAGADO->ENVIADO->ENTREGADO)")
    void cambiarEstado_transicionesValidas() {
        Usuario u = crearUsuarioCliente(emailUnico("estado_valido"));
        Pedido pedido = pedidoService.crearPedido(u.getId());

        pedido = pedidoService.cambiarEstado(pedido.getId(), EstadoPedido.PENDIENTE);
        assertEquals(EstadoPedido.PENDIENTE, pedido.getEstado());

        pedido = pedidoService.cambiarEstado(pedido.getId(), EstadoPedido.PAGADO);
        assertEquals(EstadoPedido.PAGADO, pedido.getEstado());

        pedido = pedidoService.cambiarEstado(pedido.getId(), EstadoPedido.ENVIADO);
        assertEquals(EstadoPedido.ENVIADO, pedido.getEstado());

        pedido = pedidoService.cambiarEstado(pedido.getId(), EstadoPedido.ENTREGADO);
        assertEquals(EstadoPedido.ENTREGADO, pedido.getEstado());
    }

    @Test
    @DisplayName("cambiarEstado debe rechazar transiciones inválidas (ej. CREADO->ENVIADO, ENVIADO->CANCELADO)")
    void cambiarEstado_transicionesInvalidas() {
        Usuario u = crearUsuarioCliente(emailUnico("estado_invalido"));
        Pedido pedido = pedidoService.crearPedido(u.getId());

        Long id = pedido.getId();

        // ✅ según tu validador: CREADO -> ENVIADO es inválido
        assertThrows(EstadoPedidoInvalidoException.class,
                () -> pedidoService.cambiarEstado(id, EstadoPedido.ENVIADO));

        // Llevamos el pedido a ENVIADO de forma válida: CREADO->PENDIENTE->PAGADO->ENVIADO
        pedidoService.cambiarEstado(id, EstadoPedido.PENDIENTE);
        pedidoService.cambiarEstado(id, EstadoPedido.PAGADO);
        pedidoService.cambiarEstado(id, EstadoPedido.ENVIADO);

        // ✅ ENVIADO -> CANCELADO es inválido
        assertThrows(EstadoPedidoInvalidoException.class,
                () -> pedidoService.cambiarEstado(id, EstadoPedido.CANCELADO));
    }

    @Test
    @DisplayName("cambiarEstado desde ENTREGADO o CANCELADO no debe permitir cambios")
    void cambiarEstado_desdeFinalizado() {
        Usuario u = crearUsuarioCliente(emailUnico("estado_finalizado"));
        Pedido pedido = pedidoService.crearPedido(u.getId());

        Long id = pedido.getId();
        pedidoService.cambiarEstado(id, EstadoPedido.PENDIENTE);
        pedidoService.cambiarEstado(id, EstadoPedido.PAGADO);
        pedidoService.cambiarEstado(id, EstadoPedido.ENVIADO);
        pedidoService.cambiarEstado(id, EstadoPedido.ENTREGADO);

        assertThrows(EstadoPedidoInvalidoException.class,
                () -> pedidoService.cambiarEstado(id, EstadoPedido.CREADO));
    }

    // ---------- ELIMINAR PEDIDO ----------

    @Test
    @DisplayName("eliminarPedido debe borrar el pedido y sus líneas")
    void eliminarPedido_borraPedidoYLineas() {
        Usuario u = crearUsuarioCliente(emailUnico("pedido_eliminar"));
        Producto p = crearProducto("REF-PED-DEL", new BigDecimal("4.00"));
        Pedido pedido = pedidoService.crearPedido(u.getId());
        pedido = pedidoService.agregarLinea(pedido.getId(), p.getId(), 2);

        Long idPedido = pedido.getId();

        Pedido conDetalles = pedidoService.obtenerPedido(idPedido);
        Set<DetallePedido> detalles = conDetalles.getDetalles();
        assertNotNull(detalles);
        assertFalse(detalles.isEmpty());

        for (DetallePedido d : detalles) {
            assertTrue(detallePedidoRepository
                    .findByPedido_IdAndProducto_Id(idPedido, d.getProducto().getId())
                    .isPresent());
        }

        pedidoService.eliminarPedido(idPedido);

        assertFalse(pedidoRepository.findById(idPedido).isPresent());
        for (DetallePedido d : detalles) {
            assertFalse(detallePedidoRepository
                    .findByPedido_IdAndProducto_Id(idPedido, d.getProducto().getId())
                    .isPresent());
        }
    }

    @Test
    @DisplayName("eliminarPedido debe lanzar PedidoNoEncontradoException si no existe")
    void eliminarPedido_noExiste_lanzaExcepcion() {
        assertThrows(PedidoNoEncontradoException.class,
                () -> pedidoService.eliminarPedido(9999L));
    }

    // ---------- CHECKOUT / STOCK / SALDO (ajustado a TU lógica) ----------

    @Test
    @DisplayName("checkout debe descontar stock al confirmar y reponerlo al cancelar")
    void checkout_descuentaStock_y_reposicion_alCancelar() {
        Usuario u = crearUsuarioCliente(emailUnico("stock_user"));
        // ✅ necesario: confirmarPedido valida saldo antes que stock
        u.setSaldo(new BigDecimal("100.00"));
        usuarioRepository.saveAndFlush(u);

        Producto p = crearProducto("REF-STOCK-1", new BigDecimal("7.00"));
        p.setStock(2);
        productoRepository.saveAndFlush(p);

        Pedido pedido = pedidoService.crearPedido(u.getId());
        pedido = pedidoService.agregarLinea(pedido.getId(), p.getId(), 2);

        // ✅ según tu impl: confirmarPedido deja el pedido en PAGADO
        Pedido confirmado = pedidoService.confirmarPedido(pedido.getId());
        assertEquals(EstadoPedido.PAGADO, confirmado.getEstado());

        Producto afterConfirm = productoRepository.findById(p.getId()).orElseThrow();
        assertEquals(0, afterConfirm.getStock());

        // cancelar desde PAGADO => debe reponer stock
        Pedido cancelado = pedidoService.cambiarEstado(pedido.getId(), EstadoPedido.CANCELADO);
        assertEquals(EstadoPedido.CANCELADO, cancelado.getEstado());

        Producto afterCancel = productoRepository.findById(p.getId()).orElseThrow();
        assertEquals(2, afterCancel.getStock());
    }

    @Test
    @DisplayName("confirmarPedido debe lanzar StockInsuficienteException si no hay stock suficiente")
    void confirmarPedido_stockInsuficiente_lanzaExcepcion() {
        Usuario u = crearUsuarioCliente(emailUnico("stockfail_user"));
        // ✅ importante: que NO falle por saldo antes
        u.setSaldo(new BigDecimal("1000.00"));
        usuarioRepository.saveAndFlush(u);

        Producto p = crearProducto("REF-STOCK-FAIL", new BigDecimal("7.00"));
        p.setStock(1);
        productoRepository.saveAndFlush(p);

        Pedido pedido = pedidoService.crearPedido(u.getId());
        pedidoService.agregarLinea(pedido.getId(), p.getId(), 2);

        final Long pedidoId = pedido.getId();

        assertThrows(StockInsuficienteException.class,
                () -> pedidoService.confirmarPedido(pedidoId));
    }


    // ---------- PARAMETRIZADO CON MOCKS (arreglado) ----------

    private static Stream<Object[]> invalidTransitionsProvider() {
        // ✅ según tu validarTransicionEstado:
        // CREADO: inválido -> ENVIADO, ENTREGADO
        // PENDIENTE: inválido -> ENVIADO (y otros no permitidos)
        // PAGADO: inválido -> PENDIENTE
        // ENVIADO: inválido -> CANCELADO
        // ENTREGADO/CANCELADO: cualquier cambio inválido
        return Stream.of(
                new Object[]{EstadoPedido.CREADO, EstadoPedido.ENVIADO},
                new Object[]{EstadoPedido.CREADO, EstadoPedido.ENTREGADO},
                new Object[]{EstadoPedido.PENDIENTE, EstadoPedido.ENVIADO},
                new Object[]{EstadoPedido.PAGADO, EstadoPedido.PENDIENTE},
                new Object[]{EstadoPedido.ENVIADO, EstadoPedido.CANCELADO},
                new Object[]{EstadoPedido.ENTREGADO, EstadoPedido.CREADO},
                new Object[]{EstadoPedido.CANCELADO, EstadoPedido.PAGADO}
        );
    }

    @ParameterizedTest
    @MethodSource("invalidTransitionsProvider")
    @DisplayName("cambiarEstado debe lanzar EstadoPedidoInvalidoException para transiciones inválidas (unit dentro de integración)")
    void cambiarEstado_transicionesInvalidas_param(EstadoPedido actual, EstadoPedido nuevo) {
        var pedidoRepoMock = Mockito.mock(PedidoRepository.class);
        var usuarioRepoMock = Mockito.mock(UsuarioRepository.class);
        var productoRepoMock = Mockito.mock(ProductoRepository.class);
        var detalleRepoMock = Mockito.mock(DetallePedidoRepository.class);

        var service = new es.unex.cum.mdai.motoresbits.service.impl.PedidoServiceImpl(
                pedidoRepoMock, usuarioRepoMock, productoRepoMock, detalleRepoMock);

        Pedido p = new Pedido();
        p.setId(999L);
        p.setEstado(actual);

        // ✅ cambiarEstado usa findConLineasYProductos, no findById
        when(pedidoRepoMock.findConLineasYProductos(999L)).thenReturn(java.util.Optional.of(p));

        assertThrows(EstadoPedidoInvalidoException.class,
                () -> service.cambiarEstado(999L, nuevo));
    }

    // ---------- CONCURRENCIA (arreglado: saldo) ----------

    @Test
    @DisplayName("concurrencia_confirmarPedidos_noSobrevender")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrencia_confirmarPedidos_noSobrevender() throws InterruptedException {

        Categoria cat = new Categoria();
        cat.setNombre("CatConc");
        cat.setDescripcion("desc");
        categoriaRepository.save(cat);

        Producto prod = new Producto();
        prod.setCategoria(cat);
        prod.setNombre("ProdConc");
        prod.setReferencia("REF-CONC");
        prod.setPrecio(new BigDecimal("10.00"));
        prod.setStock(1);
        productoRepository.saveAndFlush(prod);

        Usuario u1 = crearUsuarioCliente(emailUnico("c1"));
        Usuario u2 = crearUsuarioCliente(emailUnico("c2"));
        // ✅ ambos con saldo suficiente (si no, success = 0)
        u1.setSaldo(new BigDecimal("100.00"));
        u2.setSaldo(new BigDecimal("100.00"));
        usuarioRepository.saveAndFlush(u1);
        usuarioRepository.saveAndFlush(u2);

        Pedido ped1 = pedidoService.crearPedido(u1.getId());
        Pedido ped2 = pedidoService.crearPedido(u2.getId());

        pedidoService.agregarLinea(ped1.getId(), prod.getId(), 1);
        pedidoService.agregarLinea(ped2.getId(), prod.getId(), 1);

        ExecutorService ex = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(2);
            AtomicInteger success = new AtomicInteger(0);

            ex.submit(() -> {
                try {
                    start.await();
                    try {
                        pedidoService.confirmarPedido(ped1.getId());
                        success.incrementAndGet();
                    } catch (StockInsuficienteException e) {
                        // esperado para uno
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });

            ex.submit(() -> {
                try {
                    start.await();
                    try {
                        pedidoService.confirmarPedido(ped2.getId());
                        success.incrementAndGet();
                    } catch (StockInsuficienteException e) {
                        // esperado para uno
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });

            start.countDown();
            boolean finished = done.await(5, TimeUnit.SECONDS);

            assertTrue(finished, "Las tareas deben terminar en tiempo");
            assertEquals(1, success.get(), "Solo un pedido debe confirmarse exitosamente");

            Producto after = productoRepository.findById(prod.getId()).orElseThrow();
            assertEquals(0, after.getStock(), "El stock final debe ser 0");
        } finally {
            ex.shutdownNow();
        }
    }
}
