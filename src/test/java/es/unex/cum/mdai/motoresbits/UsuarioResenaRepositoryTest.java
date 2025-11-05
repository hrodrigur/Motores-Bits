package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import es.unex.cum.mdai.motoresbits.data.model.entity.Categoria;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.model.entity.Resena;
import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;
import es.unex.cum.mdai.motoresbits.data.model.enums.RolUsuario;
import es.unex.cum.mdai.motoresbits.data.repository.CategoriaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ResenaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UsuarioResenaRepositoryTest {

    @Autowired UsuarioRepository usuarioRepository;
    @Autowired CategoriaRepository categoriaRepository;
    @Autowired ProductoRepository productoRepository;
    @Autowired ResenaRepository resenaRepository;

    @Test
    void crearUsuarioProductoYResena_y_buscarResenasPorProducto() {
        // Usuario
        var u = new Usuario();
        u.setNombre("Ana");
        u.setEmail("ana@test.com");
        u.setContrasena("x");
        try { u.setRol(RolUsuario.CLIENTE); } catch (Throwable ignored) {} // por si tu campo es String
        usuarioRepository.save(u);

        // Producto (con categoría)
        var c = new Categoria();
        c.setNombre("Frenos");
        c.setDescripcion("Piezas de freno");
        categoriaRepository.save(c);

        var p = new Producto();
        p.setNombre("Pastillas freno");
        p.setReferencia("PF-001");
        p.setPrecio(new BigDecimal("19.95"));
        p.setStock(50);
        p.setCategoria(c);
        productoRepository.save(p);

        // Reseña
        var r = new Resena();
        r.setUsuario(u);
        r.setProducto(p);
        r.setPuntuacion(5);
        r.setComentario("Muy buenas");
        try { r.setCreadaEn(LocalDateTime.now()); } catch (Throwable ignored) {}
        resenaRepository.save(r);

        // when
        var resenas = resenaRepository.findByProductoId(p.getId());

        // then
        assertThat(resenas).hasSize(1);
        assertThat(resenas.get(0).getUsuario().getEmail()).isEqualTo("ana@test.com");
    }
}
