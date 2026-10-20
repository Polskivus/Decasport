package erp.modelo;

import java.math.BigDecimal;

/**
 * Producto (tabla Producto). El stock se guarda en el propio producto porque
 * hay un unico almacen. Solo debe cambiarse junto con un movimiento de
 * inventario (lo hace InventarioServicio).
 */
public class Producto implements Entidad {

	private int id; // PK AUTO_INCREMENT
	private String sku; // VARCHAR(50) UQ
	private String nombre; // VARCHAR(150)
	private BigDecimal precioVenta = BigDecimal.ZERO; // DECIMAL(12,2) CHECK >= 0
	private BigDecimal precioCoste = BigDecimal.ZERO; // DECIMAL(12,2) CHECK >= 0
	private int stockActual; // INT CHECK >= 0
	private int stockMinimo; // INT CHECK >= 0
	private Integer proveedorId; // FK -> Proveedor.id (opcional)

	public Producto() {
	}

	public Producto(String sku, String nombre, BigDecimal precioVenta, BigDecimal precioCoste, int stockActual,
			int stockMinimo, Integer proveedorId) {
		setSku(sku);
		setNombre(nombre);
		setPrecioVenta(precioVenta);
		setPrecioCoste(precioCoste);
		setStockActual(stockActual);
		setStockMinimo(stockMinimo);
		setProveedorId(proveedorId);
	}

	/** Copia los datos editables (no el id ni el stock actual). */
	public void copiarDatosDe(Producto otro) {
		this.sku = otro.sku;
		this.nombre = otro.nombre;
		this.precioVenta = otro.precioVenta;
		this.precioCoste = otro.precioCoste;
		this.stockMinimo = otro.stockMinimo;
		this.proveedorId = otro.proveedorId;
	}

	public boolean isBajoMinimo() {
		return stockActual < stockMinimo;
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

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = Validar.texto(sku, "SKU", 50, true).toUpperCase();
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = Validar.texto(nombre, "nombre", 150, true);
	}

	public BigDecimal getPrecioVenta() {
		return precioVenta;
	}

	public void setPrecioVenta(BigDecimal precioVenta) {
		this.precioVenta = Validar.importe(precioVenta, "precio de venta");
	}

	public BigDecimal getPrecioCoste() {
		return precioCoste;
	}

	public void setPrecioCoste(BigDecimal precioCoste) {
		this.precioCoste = Validar.importe(precioCoste, "precio de coste");
	}

	public int getStockActual() {
		return stockActual;
	}

	public void setStockActual(int stockActual) {
		this.stockActual = Validar.noNegativo(stockActual, "stock actual");
	}

	public int getStockMinimo() {
		return stockMinimo;
	}

	public void setStockMinimo(int stockMinimo) {
		this.stockMinimo = Validar.noNegativo(stockMinimo, "stock minimo");
	}

	public Integer getProveedorId() {
		return proveedorId;
	}

	public void setProveedorId(Integer proveedorId) {
		this.proveedorId = proveedorId == null || proveedorId <= 0 ? null : proveedorId;
	}

	@Override
	public String toString() {
		return sku + " - " + nombre;
	}
}
