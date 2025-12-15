package es.unex.cum.mdai.motoresbits.data.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "PRODUCTOS", uniqueConstraints = {
        @UniqueConstraint(name = "uk_producto_referencia", columnNames = "referencia")
})
public class Producto implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long id;

    // Relación con categoría
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false,
            foreignKey = @ForeignKey(name = "fk_producto_categoria"))
    private Categoria categoria;

    @Column(nullable = false)
    @Size(max = 30)
    private String nombre;

    @Column(nullable = false, unique = true)
    @Size(max = 15)
    private String referencia;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Min(0)
    @Max(100)
    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "imagen", length = 120)
    private String imagen;

    @Column(name = "imagen_url", length = 1000)
    private String imagenUrl;
    @Version
    private Integer version;

    // IMPORTANTE: SIN REMOVE ni orphanRemoval para que NO se borren detalles al borrar el producto
    @OneToMany(
            mappedBy = "producto",
            cascade = { CascadeType.PERSIST, CascadeType.MERGE },
            orphanRemoval = false
    )
    private Set<DetallePedido> detalles = new LinkedHashSet<>();

    // Getters / Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Set<DetallePedido> getDetalles() { return detalles; }
    public void setDetalles(Set<DetallePedido> detalles) { this.detalles = detalles; }

    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
}
