package es.unex.cum.mdai.motoresbits.service;

import es.unex.cum.mdai.motoresbits.data.model.entity.Pedido;
import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;
import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;
import es.unex.cum.mdai.motoresbits.data.model.enums.RolUsuario;
import es.unex.cum.mdai.motoresbits.data.repository.PedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;
import es.unex.cum.mdai.motoresbits.service.exception.CredencialesInvalidasException;
import es.unex.cum.mdai.motoresbits.service.exception.EmailYaRegistradoException;
import es.unex.cum.mdai.motoresbits.service.exception.UsuarioNoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

// Pruebas de integración para UsuarioService: registro, login, eliminación y concurrencia.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UsuarioServiceIT {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Test
    @DisplayName("registrarCliente debe crear un nuevo usuario CLIENTE con email normalizado")
    void registrarCliente_debeCrearUsuarioCliente() {
        String nombre = "Pepe";
        String email = "  PEPE@MAIL.COM  ";
        String contrasena = "secreta";

        Usuario creado = usuarioService.registrarCliente(nombre, email, contrasena);

        assertNotNull(creado.getId());
        assertEquals(nombre, creado.getNombre());
        assertEquals("pepe@mail.com", creado.getEmail());
        assertEquals(contrasena, creado.getContrasena());
        assertEquals(RolUsuario.CLIENTE, creado.getRol());

        assertTrue(usuarioRepository.findById(creado.getId()).isPresent());
    }

    @Test
    @DisplayName("registrarCliente debe lanzar EmailYaRegistradoException si el email ya existe")
    void registrarCliente_debeFallarSiEmailDuplicado() {
        Usuario existente = new Usuario();
        existente.setNombre("Ana");
        existente.setEmail("ana@mail.com");
        existente.setContrasena("1234");
        existente.setRol(RolUsuario.CLIENTE);
        usuarioRepository.save(existente);

        assertThrows(EmailYaRegistradoException.class, () ->
                usuarioService.registrarCliente("Otra Ana", "ANA@mail.com", "abcd")
        );
    }

    @Test
    @DisplayName("getById debe devolver el usuario si existe")
    void getById_debeDevolverUsuario() {
        Usuario u = new Usuario();
        u.setNombre("Carlos");
        u.setEmail("carlos@mail.com");
        u.setContrasena("pass");
        u.setRol(RolUsuario.CLIENTE);
        u = usuarioRepository.save(u);

        Usuario encontrado = usuarioService.getById(u.getId());

        assertEquals(u.getId(), encontrado.getId());
        assertEquals("Carlos", encontrado.getNombre());
    }

    @Test
    @DisplayName("getById debe lanzar UsuarioNoEncontradoException si no existe")
    void getById_debeLanzarSiNoExiste() {
        assertThrows(UsuarioNoEncontradoException.class, () ->
                usuarioService.getById(9999L)
        );
    }

    @Test
    @DisplayName("login debe devolver el usuario si las credenciales son correctas")
    void login_debeFuncionarConCredencialesCorrectas() {
        Usuario u = new Usuario();
        u.setNombre("Laura");
        u.setEmail("laura@mail.com");
        u.setContrasena("secreta");
        u.setRol(RolUsuario.CLIENTE);
        usuarioRepository.save(u);

        Usuario logueado = usuarioService.login("  LAURA@mail.com ", "secreta");

        assertNotNull(logueado);
        assertEquals(u.getId(), logueado.getId());
    }

    @Test
    @DisplayName("login debe lanzar CredencialesInvalidasException si el email no existe")
    void login_debeFallarSiEmailNoExiste() {
        assertThrows(CredencialesInvalidasException.class, () ->
                usuarioService.login("noexiste@mail.com", "x")
        );
    }

    @Test
    @DisplayName("login debe lanzar CredencialesInvalidasException si la contraseña es incorrecta")
    void login_debeFallarSiContrasenaIncorrecta() {
        Usuario u = new Usuario();
        u.setNombre("Mario");
        u.setEmail("mario@mail.com");
        u.setContrasena("correcta");
        u.setRol(RolUsuario.CLIENTE);
        usuarioRepository.save(u);

        assertThrows(CredencialesInvalidasException.class, () ->
                usuarioService.login("mario@mail.com", "incorrecta")
        );
    }

    @Test
    @DisplayName("eliminarUsuario debe borrar el usuario y sus pedidos asociados")
    void eliminarUsuario_conPedidos_debeBorrarUsuarioYPedidos() {
        Usuario u = new Usuario();
        u.setNombre("ConPedidos");
        u.setEmail("conpedidos@mail.com");
        u.setContrasena("1234");
        u.setRol(RolUsuario.CLIENTE);
        u = usuarioRepository.save(u);

        Pedido p = new Pedido();
        p.setUsuario(u);
        p.setFechaPedido(LocalDate.now());
        p.setEstado(EstadoPedido.CREADO);
        p.setTotal(new BigDecimal("100.00"));
        pedidoRepository.save(p);

        Long idUsuario = u.getId();
        Long idPedido = p.getId();

        usuarioService.eliminarUsuario(idUsuario);

        assertFalse(usuarioRepository.findById(idUsuario).isPresent());
        assertFalse(pedidoRepository.findById(idPedido).isPresent());
    }

    @Test
    @DisplayName("registro concurrente con mismo email debe permitir solo uno")
    @SuppressWarnings("resource")
    void registrar_concurrente_unicidadEmail() throws InterruptedException {
        final String email = "concurrent@mail.com";
        int threads = 5;
        @SuppressWarnings("resource")
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);
            AtomicInteger success = new AtomicInteger(0);

            for (int i = 0; i < threads; i++) {
                ex.submit(() -> {
                    try {
                        start.await();
                        try {
                            usuarioService.registrarCliente("User", email, "pwd");
                            success.incrementAndGet();
                        } catch (Exception ignored) {
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            done.await();

            assertEquals(1, success.get());

            var found = usuarioRepository.findByEmail(email.toLowerCase().trim());
            assertTrue(found.isPresent());
        } finally {
            ex.shutdownNow();
        }
    }
}
