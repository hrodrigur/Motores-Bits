package es.unex.cum.mdai.motoresbits.service.impl;

import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;
import es.unex.cum.mdai.motoresbits.data.model.enums.RolUsuario;
import es.unex.cum.mdai.motoresbits.data.repository.PedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;
import es.unex.cum.mdai.motoresbits.service.UsuarioService;
import es.unex.cum.mdai.motoresbits.service.exception.CredencialesInvalidasException;
import es.unex.cum.mdai.motoresbits.service.exception.EmailYaRegistradoException;
import es.unex.cum.mdai.motoresbits.service.exception.UsuarioConPedidosException;
import es.unex.cum.mdai.motoresbits.service.exception.UsuarioNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository,
                              PedidoRepository pedidoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
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

        // Regla de negocio: no eliminar si tiene pedidos
        boolean tienePedidos = !pedidoRepository.findByUsuarioId(id).isEmpty();
        if (tienePedidos) {
            throw new UsuarioConPedidosException(id);
        }

        usuarioRepository.delete(u);
    }
}
