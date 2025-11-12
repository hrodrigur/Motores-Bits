package es.unex.cum.mdai.motoresbits.domain.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;
import es.unex.cum.mdai.motoresbits.domain.exception.NotFoundException;
import es.unex.cum.mdai.motoresbits.domain.service.UsuarioService;

@Service
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepo;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepo) {
        this.usuarioRepo = usuarioRepo;
    }

    @Override
    public Usuario registrar(Usuario u) {
        return usuarioRepo.save(u);
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario buscarPorEmail(String email) {
        return usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado")); // tu repo lo expone. :contentReference[oaicite:3]{index=3}
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarioRepo.findAll();
    }

    @Override
    public void borrar(Long idUsuario) {
        if (!usuarioRepo.existsById(idUsuario))
            throw new NotFoundException("Usuario no encontrado");
        usuarioRepo.deleteById(idUsuario);
    }
}
