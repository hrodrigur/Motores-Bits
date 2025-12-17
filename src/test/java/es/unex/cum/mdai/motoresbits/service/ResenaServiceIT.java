package es.unex.cum.mdai.motoresbits.service;

import es.unex.cum.mdai.motoresbits.data.model.entity.Categoria;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.model.entity.Resena;
import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;
import es.unex.cum.mdai.motoresbits.data.repository.CategoriaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ResenaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// Pruebas de integración para ResenaService: crear, editar, listar y eliminar reseñas.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ResenaServiceIT {

    @Autowired
    private ResenaService resenaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ResenaRepository resenaRepository;

    private Usuario crearUsuario(String email) {
        Usuario u = new Usuario();
        u.setNombre("Usu " + email);
        u.setEmail(email);
        u.setContrasena("pwd");
        return usuarioRepository.save(u);
    }

    private Categoria crearCategoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setDescripcion("desc");
        return categoriaRepository.save(c);
    }

    private Producto crearProducto(Categoria c, String ref) {
        Producto p = new Producto();
        p.setCategoria(c);
        p.setNombre("Prod " + ref);
        p.setReferencia(ref);
        p.setPrecio(new BigDecimal("10.00"));
        p.setStock(10);
        return productoRepository.save(p);
    }

    @Test
    @DisplayName("crearResena valida y listar por producto")
    void crearResena_valida_y_listar() {
        Usuario u = crearUsuario("r1@example.com");
        Categoria c = crearCategoria("CatR1");
        Producto p = crearProducto(c, "REF-R1");

        Resena r = resenaService.crearResena(u.getId(), p.getId(), 5, "Muy bueno");

        assertNotNull(r.getId());
        List<Resena> list = resenaService.listarPorProducto(p.getId());
        assertEquals(1, list.size());
        assertEquals(5, list.get(0).getPuntuacion());
    }

    @Test
    @DisplayName("crearResena con puntuacion invalida lanza IllegalArgumentException")
    void crearResena_puntuacionInvalida() {
        Usuario u = crearUsuario("r2@example.com");
        Categoria c = crearCategoria("CatR2");
        Producto p = crearProducto(c, "REF-R2");

        assertThrows(IllegalArgumentException.class,
                () -> resenaService.crearResena(u.getId(), p.getId(), 6, "X"));

        assertThrows(IllegalArgumentException.class,
                () -> resenaService.crearResena(u.getId(), p.getId(), 0, "X"));
    }

    @Test
    @DisplayName("media de puntuaciones por producto")
    void mediaPuntuacion_porProducto() {
        Usuario u1 = crearUsuario("r3a@example.com");
        Usuario u2 = crearUsuario("r3b@example.com");
        Categoria c = crearCategoria("CatR3");
        Producto p = crearProducto(c, "REF-R3");

        resenaService.crearResena(u1.getId(), p.getId(), 5, "A");
        resenaService.crearResena(u2.getId(), p.getId(), 3, "B");

        Optional<Double> avg = resenaService.mediaPuntuacionPorProducto(p.getId());
        assertTrue(avg.isPresent());
        assertEquals(4.0, avg.get());
    }

    @Test
    @DisplayName("editarResena debe modificar puntuacion y comentario")
    void editarResena_modificaCampos() {
        Usuario u = crearUsuario("edit@example.com");
        Categoria c = crearCategoria("CatEdit");
        Producto p = crearProducto(c, "REF-EDIT");

        Resena r = resenaService.crearResena(u.getId(), p.getId(), 4, "Bien");

        Resena editada = resenaService.editarResena(r.getId(), 2, "Regular");
        assertEquals(2, editada.getPuntuacion());
        assertEquals("Regular", editada.getComentario());

        Resena desdeRepo = resenaRepository.findById(r.getId()).orElseThrow();
        assertEquals(2, desdeRepo.getPuntuacion());
        assertEquals("Regular", desdeRepo.getComentario());
    }

    @Test
    @DisplayName("editarResena con puntuacion invalida lanza IllegalArgumentException")
    void editarResena_puntuacionInvalida() {
        Usuario u = crearUsuario("editinv@example.com");
        Categoria c = crearCategoria("CatEditInv");
        Producto p = crearProducto(c, "REF-EDIT-INV");

        Resena r = resenaService.crearResena(u.getId(), p.getId(), 3, "Ok");

        assertThrows(IllegalArgumentException.class,
                () -> resenaService.editarResena(r.getId(), 0, "X"));
        assertThrows(IllegalArgumentException.class,
                () -> resenaService.editarResena(r.getId(), 6, "X"));
    }

    @Test
    @DisplayName("eliminarResena debe borrar la reseña y no aparecer en listados")
    void eliminarResena_borraYnoAparece() {
        Usuario u = crearUsuario("del@example.com");
        Categoria c = crearCategoria("CatDel");
        Producto p = crearProducto(c, "REF-DEL");

        Resena r = resenaService.crearResena(u.getId(), p.getId(), 5, "Top");
        assertTrue(resenaRepository.findById(r.getId()).isPresent());

        resenaService.eliminarResena(r.getId());

        assertFalse(resenaRepository.findById(r.getId()).isPresent());
        assertTrue(resenaService.listarPorProducto(p.getId()).isEmpty());
    }

    @Test
    @DisplayName("eliminarResena inexistente lanza IllegalArgumentException")
    void eliminarResena_noExiste_lanza() {
        assertThrows(IllegalArgumentException.class,
                () -> resenaService.eliminarResena(9999L));
    }

    @Test
    @DisplayName("listarPorProducto en producto sin reseñas devuelve lista vacía")
    void listarPorProducto_sinResenas_devuelveVacio() {
        Categoria c = crearCategoria("CatEmpty");
        Producto p = crearProducto(c, "REF-EMPTY");

        List<Resena> list = resenaService.listarPorProducto(p.getId());
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("crearResena con usuario o producto inexistente lanza IllegalArgumentException")
    void crearResena_entidadesNoExisten() {
        Usuario u = crearUsuario("exists@example.com");
        Categoria c = crearCategoria("CatX");
        Producto p = crearProducto(c, "REF-X");

        assertThrows(IllegalArgumentException.class,
                () -> resenaService.crearResena(9999L, p.getId(), 4, "X"));

        assertThrows(IllegalArgumentException.class,
                () -> resenaService.crearResena(u.getId(), 9999L, 4, "X"));
    }

    @Test
    @DisplayName("media de puntuaciones para producto sin reseñas devuelve Optional.empty")
    void mediaPuntuacion_sinResenas_devuelveEmpty() {
        Categoria c = crearCategoria("CatMediaEmpty");
        Producto p = crearProducto(c, "REF-MEDIA-EMPTY");

        Optional<Double> avg = resenaService.mediaPuntuacionPorProducto(p.getId());
        assertTrue(avg.isEmpty(), "Si no hay reseñas, la media debe venir vacía");
    }

}
