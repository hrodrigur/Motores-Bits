package es.unex.cum.mdai.motoresbits.service;

import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

// Servicio de usuario: registro, login, búsqueda y ajuste de saldo.
public interface UsuarioService {

    Usuario registrarCliente(String nombre, String email, String contrasena);

    Usuario getById(Long id);

    Optional<Usuario> buscarPorEmail(String email);

    List<Usuario> listarTodos();

    Usuario login(String email, String contrasena);

    void eliminarUsuario(Long id);

    Usuario actualizarPerfil(Long id, String direccion, String telefono);

    Usuario ajustarSaldo(Long idUsuario, BigDecimal delta);

}
