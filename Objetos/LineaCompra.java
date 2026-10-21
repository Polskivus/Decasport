package modelo;

import java.math.BigDecimal;

/**
 * Linea de una compra (tabla Linea_Compra). Si se borra la compra se borran
 * sus lineas (ON DELETE CASCADE).
 */
public class LineaCompra implements Entidad {

	private int id; // PK AUTO_INCREMENT
	private int compraId; // FK -> Compra.id
	private int productoId; // FK -> Producto.id
	private int cantidad; // INT CHECK > 0
	private BigDecimal precioCoste = BigDecimal.ZERO; // DECIMAL(12,2)
	private BigDecimal subtotal = BigDecimal.ZERO; // DECIMAL(12,2)

	public LineaCompra() {
	}

	public LineaCompra(int compraId, int productoId, int cantidad, BigDecimal precioCoste) {
		this.compraId = Validar.referencia(compraId, "compra");
		this.productoId = Validar.referencia(productoId, "producto");
		this.precioCoste = Validar.importe(precioCoste, "precio de coste");
		setCantidad(cantidad);
	}

	private void recalcular() {
		subtotal = precioCoste.multiply(BigDecimal.valueOf(cantidad));
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

	public int getCompraId() {
		return compraId;
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

	public BigDecimal getPrecioCoste() {
		return precioCoste;
	}

	public void setPrecioCoste(BigDecimal precioCoste) {
		this.precioCoste = Validar.importe(precioCoste, "precio de coste");
		recalcular();
	}

	public BigDecimal getSubtotal() {
		return subtotal;
	}
}
