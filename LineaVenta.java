package modelo;

import java.math.BigDecimal;

/**
 * Linea de una venta (tabla Linea_Venta). Si se borra la venta se borran sus
 * lineas (ON DELETE CASCADE). El precio se copia del producto al crear la
 * linea, asi no cambia aunque luego cambie el precio del producto.
 */
public class LineaVenta implements Entidad {

	private int id; // PK AUTO_INCREMENT
	private int ventaId; // FK -> Venta.id
	private int productoId; // FK -> Producto.id
	private int cantidad; // INT CHECK > 0
	private BigDecimal precioUnitario = BigDecimal.ZERO; // DECIMAL(12,2)
	private BigDecimal subtotal = BigDecimal.ZERO; // DECIMAL(12,2)

	public LineaVenta() {
	}

	public LineaVenta(int ventaId, int productoId, int cantidad, BigDecimal precioUnitario) {
		this.ventaId = Validar.referencia(ventaId, "venta");
		this.productoId = Validar.referencia(productoId, "producto");
		this.precioUnitario = Validar.importe(precioUnitario, "precio unitario");
		setCantidad(cantidad);
	}

	private void recalcular() {
		subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
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

	public int getVentaId() {
		return ventaId;
	}

	public int getProductoId() {
		return productoId;
	}

	public int getCantidad() {
		return cantidad;
	}

	public void setCantidad(int cantidad) {
		this.cantidad = Validar.positivo(cantidad, "cantidad");
		recalcular();
	}

	public BigDecimal getPrecioUnitario() {
		return precioUnitario;
	}

	public void setPrecioUnitario(BigDecimal precioUnitario) {
		this.precioUnitario = Validar.importe(precioUnitario, "precio unitario");
		recalcular();
	}

	public BigDecimal getSubtotal() {
		return subtotal;
	}
}
