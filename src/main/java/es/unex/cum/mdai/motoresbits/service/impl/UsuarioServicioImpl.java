package es.unex.cum.mdai.motoresbits.service.impl;

import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;
import es.unex.cum.mdai.motoresbits.data.model.enums.RolUsuario;
import es.unex.cum.mdai.motoresbits.data.repository.PedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ResenaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;
import es.unex.cum.mdai.motoresbits.service.UsuarioService;
import es.unex.cum.mdai.motoresbits.service.exception.CredencialesInvalidasException;
import es.unex.cum.mdai.motoresbits.service.exception.EmailYaRegistradoException;
import es.unex.cum.mdai.motoresbits.service.exception.UsuarioNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

// Implementación del servicio de usuarios: registro, login, búsqueda y ajuste de saldo.
@Service
@Transactional
public class UsuarioServicioImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final ResenaRepository resenaRepository;

    public UsuarioServicioImpl(UsuarioRepository usuarioRepository,
                              PedidoRepository pedidoRepository,
                              ResenaRepository resenaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.resenaRepository = resenaRepository;
    }

    @Override
    public Usuario registrarCliente(String nombre, String email, String contrasena) {
        // Normalizamos email (ejemplo sencillo)
        String emailNormalizado = email == null ? null : email.trim().toLowerCase();

        // 1) Comprobar si el email ya existe
        if (usuarioRepository.findByEmail(emailNormalizado).isPresent()) {
            throw new EmailYaRegistradoException(emailNormalizado);
        }

        // 2) Crear entidad
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(emailNormalizado);
        u.setContrasena(contrasena); // más adelante: cifrar con PasswordEncoder
        u.setRol(RolUsuario.CLIENTE);

        // 3) Persistir
        return usuarioRepository.save(u);
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario getById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        String emailNormalizado = email.trim().toLowerCase();
        return usuarioRepository.findByEmail(emailNormalizado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario login(String email, String contrasena) {
        String emailNormalizado = email == null ? null : email.trim().toLowerCase();

        var usuarioOpt = usuarioRepository.findByEmail(emailNormalizado);

        if (usuarioOpt.isEmpty()) {
            // No se diferencia entre "no existe" y "contraseña mal" por seguridad
            throw new CredencialesInvalidasException();
        }

        Usuario u = usuarioOpt.get();

        // Simulación: texto plano (como en tus tests)
        // Más adelante: usar PasswordEncoder.matches(raw, encoded)
        if (!u.getContrasena().equals(contrasena)) {
            throw new CredencialesInvalidasException();
        }

        return u;
    }

    @Override
    public void eliminarUsuario(Long id) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        // Primero eliminamos reseñas hechas por el usuario
        try {
            var resenas = resenaRepository.findByUsuarioId(id);
            if (resenas != null && !resenas.isEmpty()) {
                resenaRepository.deleteAll(resenas);
            }
        } catch (Exception ignore) {
            // En algunos contextos resenaRepository puede no estar inyectado aún; fallback a no-op
        }

        // Luego eliminamos los pedidos del usuario (y sus líneas por cascade)
        var pedidos = pedidoRepository.findByUsuarioId(id);
        if (pedidos != null && !pedidos.isEmpty()) {
            pedidoRepository.deleteAll(pedidos);
        }

        // Por último eliminamos el usuario
        usuarioRepository.delete(u);
    }

    @Override
    public Usuario actualizarPerfil(Long id, String direccion, String telefono) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        u.setDireccion(direccion);
        u.setTelefono(telefono);

        return usuarioRepository.save(u);
    }

    @Override
    @Transactional
    public Usuario ajustarSaldo(Long idUsuario, BigDecimal delta) {

        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("La cantidad debe ser distinta de 0");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        BigDecimal saldoActual = usuario.getSaldo() == null ? BigDecimal.ZERO : usuario.getSaldo();
        BigDecimal nuevoSaldo = saldoActual.add(delta);

        if (nuevoSaldo.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("No se puede dejar el saldo en negativo");
        }

        usuario.setSaldo(nuevoSaldo);
        return usuarioRepository.save(usuario);
    }

}
