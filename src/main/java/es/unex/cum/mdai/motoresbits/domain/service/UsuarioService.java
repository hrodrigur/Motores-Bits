package es.unex.cum.mdai.motoresbits.domain.service;

import java.util.List;
import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;

public interface UsuarioService {
    Usuario registrar(Usuario u);
    Usuario buscarPorEmail(String email);
    List<Usuario> listar();
    void borrar(Long idUsuario);
}
