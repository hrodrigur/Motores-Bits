package es.unex.cum.mdai.motoresbits.data.model.entity;

import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "PEDIDOS")
public class Pedido implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long id;

    // Usuario
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pedido_usuario"))
    private Usuario usuario;

    @Column(name = "fec_pedido", nullable = false)
    private LocalDate fechaPedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    // IMPORTANTE: aquí SÍ queremos que borrar el pedido borre sus líneas
    @OneToMany(
            mappedBy = "pedido",
            cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE },
            orphanRemoval = true
    )
    private Set<DetallePedido> detalles = new LinkedHashSet<>();

    // ---- Métodos de dominio ----
    public DetallePedido addLinea(Producto producto, int cantidad, BigDecimal precio) {
        DetallePedido d = new DetallePedido();
        d.setCantidad(cantidad);
        d.setPrecio(precio);
        d.attach(this, producto);          // sincroniza PK compuesta
        this.detalles.add(d);              // único punto de inserción (evita duplicados)
        return d;
    }

    public void removeLinea(DetallePedido detalle) {
        if (detalle != null) {
            this.detalles.remove(detalle);
            detalle.attach(null, null);
        }
    }

    // ---- Getters / Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDate getFechaPedido() { return fechaPedido; }
    public void setFechaPedido(LocalDate fechaPedido) { this.fechaPedido = fechaPedido; }

    public EstadoPedido getEstado() { return estado; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public Set<DetallePedido> getDetalles() { return detalles; }
    public void setDetalles(Set<DetallePedido> detalles) { this.detalles = detalles; }
}
