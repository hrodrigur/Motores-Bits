package es.unex.cum.mdai.motoresbits.data.model.entity;

import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PEDIDOS")
public class Pedido implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Relación con Usuario ---
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // --- Atributos del pedido ---
    @Column(name = "fec_pedido", nullable = false)
    private LocalDate fechaPedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    // --- Líneas del pedido ---
    @OneToMany(
            mappedBy = "pedido",
            cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE },
            orphanRemoval = true
    )
    private List<DetallePedido> detalles = new ArrayList<>();

    // ----------------------
    // Métodos de dominio
    // ----------------------
    /**
     * Crea y añade una línea al pedido. IMPORTANTE: lo normal es que este pedido
     * ya esté persistido (tenga id) antes de llamar a este método.
     */
    public DetallePedido addLinea(Producto producto, int cantidad, BigDecimal precio) {
        DetallePedido d = new DetallePedido();
        d.setCantidad(cantidad);
        d.setPrecio(precio);
        d.attach(this, producto); // sincroniza MapsId + añade a la colección si no estaba
        return d;
    }

    public void removeLinea(DetallePedido detalle) {
        if (detalle != null) {
            this.detalles.remove(detalle);
            detalle.attach(null, null);
        }
    }

    // ----------------------
    // Getters / Setters
    // ----------------------
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

    public List<DetallePedido> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePedido> detalles) { this.detalles = detalles; }
}
