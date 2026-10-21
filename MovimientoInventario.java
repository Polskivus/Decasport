package erp.modelo;

import java.time.LocalDateTime;

/**
 * Movimiento de stock (tabla inventario): historico de entradas, salidas y
 * ajustes de cada producto.
 *
 * Enlaza con la venta o la compra que lo origina, nunca con las dos. Si no
 * tiene ninguna es un ajuste manual. La cantidad es positiva en entradas y
 * salidas; en un ajuste lleva signo (+ suma stock, - lo resta).
 */
public class MovimientoInventario implements Entidad {

	private int id; // PK AUTO_INCREMENT
	private int productoId; // FK -> Producto.id
	private TipoMovimiento tipo; // ENUM
	private int cantidad; // INT
	private LocalDateTime fecha; // DATETIME
	private Integer ventaId; // FK -> Venta.id (opcional)
	private Integer compraId; // FK -> Compra.id (opcional)

	public MovimientoInventario() {
	}

	public MovimientoInventario(int productoId, TipoMovimiento tipo, int cantidad, Integer ventaId,
			Integer compraId) {
		if (tipo == null) {
			throw new ErpException("El tipo de movimiento es obligatorio");
		}
		if (cantidad == 0) {
			throw new ErpException("La cantidad del movimiento no puede ser 0");
		}
		if (tipo != TipoMovimiento.AJUSTE && cantidad < 0) {
			throw new ErpException("En entradas y salidas la cantidad debe ser positiva");
		}
		// CHECK de la tabla: no puede tener venta y compra a la vez
		if (ventaId != null && compraId != null) {
			throw new ErpException("Un movimiento no puede tener venta y compra a la vez");
		}
		this.productoId = Validar.referencia(productoId, "producto");
		this.tipo = tipo;
		this.cantidad = cantidad;
		this.ventaId = ventaId;
		this.compraId = compraId;
		this.fecha = LocalDateTime.now();
	}

	/** Lo que cambia el stock: + en entradas, - en salidas, con signo en ajustes. */
	public int getEfectoEnStock() {
		return tipo == TipoMovimiento.SALIDA ? -cantidad : cantidad;
	}

	public boolean isAjusteManual() {
		return ventaId == null && compraId == null;
	}

	// ------------------------------------------------------------------
	// Getters y setters
	// ------------------------------------------------------------------
	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

	public int getProductoId() {
		return productoId;
	}

	public TipoMovimiento getTipo() {
		return tipo;
	}

	public int getCantidad() {
		return cantidad;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public Integer getVentaId() {
		return ventaId;
	}

	public Integer getCompraId() {
		return compraId;
	}
}
