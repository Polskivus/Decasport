package objetos;

import java.time.LocalDateTime;

/**
 * Objeto que representa una fila de la tabla "inventario":
 * el histórico de movimientos de stock (trazabilidad).
 *
 * Nota: la cantidad guardada es siempre positiva; el sentido del movimiento
 * lo marca el tipo (ENTRADA / SALIDA / AJUSTE).
 */
public class Inventario {

    /** Tipos de movimiento admitidos por la tabla inventario. */
    public enum TipoMovimiento {
        ENTRADA, SALIDA, AJUSTE
    }

    private int id;
    private int productoId;
    private TipoMovimiento tipo;
    private int cantidad;
    private LocalDateTime fecha;
    private Integer ventaId;
    private Integer compraId;

    public Inventario() {
    }

    /** Constructor para dar de alta un movimiento nuevo (sin id ni fecha). */
    public Inventario(int productoId, TipoMovimiento tipo, int cantidad,
                      Integer ventaId, Integer compraId) {
        this.productoId = productoId;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.ventaId = ventaId;
        this.compraId = compraId;
    }

    /** Constructor completo, para reconstruir un movimiento leído de la base de datos. */
    public Inventario(int id, int productoId, TipoMovimiento tipo, int cantidad,
                      LocalDateTime fecha, Integer ventaId, Integer compraId) {
        this.id = id;
        this.productoId = productoId;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.fecha = fecha;
        this.ventaId = ventaId;
        this.compraId = compraId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProductoId() {
        return productoId;
    }

    public void setProductoId(int productoId) {
        this.productoId = productoId;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimiento tipo) {
        this.tipo = tipo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Integer getVentaId() {
        return ventaId;
    }

    public void setVentaId(Integer ventaId) {
        this.ventaId = ventaId;
    }

    public Integer getCompraId() {
        return compraId;
    }

    public void setCompraId(Integer compraId) {
        this.compraId = compraId;
    }

    /** true si el movimiento suma stock. */
    public boolean esEntrada() {
        return tipo == TipoMovimiento.ENTRADA;
    }

    /** true si el movimiento resta stock. */
    public boolean esSalida() {
        return tipo == TipoMovimiento.SALIDA;
    }

    /** true si el movimiento fija el stock a un valor concreto. */
    public boolean esAjuste() {
        return tipo == TipoMovimiento.AJUSTE;
    }

    @Override
    public String toString() {
        return "Inventario[id=" + id
                + ", productoId=" + productoId
                + ", tipo=" + tipo
                + ", cantidad=" + cantidad
                + ", fecha=" + fecha
                + ", ventaId=" + ventaId
                + ", compraId=" + compraId + "]";
    }
}
