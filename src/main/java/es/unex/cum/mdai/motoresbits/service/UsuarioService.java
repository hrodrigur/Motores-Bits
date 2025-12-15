package es.unex.cum.mdai.motoresbits.service;

import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface UsuarioService {

    /**
     * Registra un nuevo usuario con rol CLIENTE.
     * Lanza una excepción si el email ya está en uso.
     */
    Usuario registrarCliente(String nombre, String email, String contrasena);

    /**
     * Devuelve un usuario existente por id o lanza excepción si no existe.
     */
    Usuario getById(Long id);

    /**
     * Devuelve un usuario envuelto en Optional si existe el email.
     */
    Optional<Usuario> buscarPorEmail(String email);

    /**
     * Devuelve la lista completa de usuarios.
     */
    List<Usuario> listarTodos();

    /**
     * Caso de uso de login.
     * Por ahora compara contraseñas en texto plano.
     * Lanza excepción si email o contraseña no son válidos.
     */
    Usuario login(String email, String contrasena);

    /**
     * Elimina un usuario si no tiene pedidos asociados.
     * Si tiene pedidos, lanza una excepción de regla de negocio.
     */
    void eliminarUsuario(Long id);

    /**
     * Actualiza campos de perfil basicos del usuario.
     */
    Usuario actualizarPerfil(Long id, String direccion, String telefono);

    Usuario ajustarSaldo(Long idUsuario, BigDecimal delta);

}
