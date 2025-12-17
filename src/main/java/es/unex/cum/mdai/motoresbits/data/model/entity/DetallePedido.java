package es.unex.cum.mdai.motoresbits.data.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

// Entidad DetallePedido: línea individual dentro de un pedido (PK compuesta).
@Entity
@Table(name = "DETALLES_PEDIDO")
public class DetallePedido implements Serializable {

    @EmbeddedId
    private DetallePedidoId id = new DetallePedidoId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("pedidoId")
    @JoinColumn(
            name = "id_pedido",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_detalle_pedido")
    )
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("productoId")
    @JoinColumn(
            name = "id_producto",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_detalle_producto")
    )
    private Producto producto;

    @Min(1)
    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    // Sincroniza relaciones y la PK compuesta. No añade a colecciones.
    public void attach(Pedido p, Producto pr) {
        this.pedido = p;
        this.producto = pr;
        if (p != null) this.id.setPedidoId(p.getId()); else this.id.setPedidoId(null);
        if (pr != null) this.id.setProductoId(pr.getId()); else this.id.setProductoId(null);
    }

    // equals/hashCode por id compuesta (para Set<> en Pedido)
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DetallePedido)) return false;
        return Objects.equals(this.id, ((DetallePedido)o).id);
    }
    @Override public int hashCode() { return Objects.hash(this.id); }

    // Getters / Setters
    public DetallePedidoId getId() { return id; }
    public void setId(DetallePedidoId id) { this.id = id; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
        this.id.setPedidoId(pedido != null ? pedido.getId() : null);
    }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) {
        this.producto = producto;
        this.id.setProductoId(producto != null ? producto.getId() : null);
    }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
}
