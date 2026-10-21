package servicio;

import java.math.BigDecimal;
import java.util.List;

import datos.BaseDatosMemoria;
import modelo.ErpException;
import modelo.MovimientoInventario;
import modelo.Producto;
import modelo.TipoMovimiento;

/**
 * Paquetes 3 y 5: Inventario. Productos, stock y el historico de movimientos.
 *
 * El stock solo cambia en registrarMovimiento(), que actualiza el producto y
 * guarda el movimiento a la vez (en MySQL sera una unica transaccion).
 */
public class InventarioServicio {

	private final BaseDatosMemoria bd;

	public InventarioServicio(BaseDatosMemoria bd) {
		this.bd = bd;
	}

	// ------------------------------------------------------------------
	// Productos
	// ------------------------------------------------------------------
	public List<Producto> listarProductos() {
		return bd.productos.todos();
	}

	public Producto obtenerProducto(int id) {
		return bd.productos.obtener(id);
	}

	public List<Producto> buscarProductos(String texto) {
		String t = texto == null ? "" : texto.trim().toLowerCase();
		return bd.productos.filtrar(p -> p.getSku().toLowerCase().contains(t) || p.getNombre().toLowerCase().contains(t));
	}

	/** Da de alta el producto; si trae stock inicial se registra como ajuste. */
	public Producto crearProducto(Producto producto) {
		comprobarSkuLibre(producto.getSku(), 0);
		comprobarProveedor(producto.getProveedorId());
		int stockInicial = producto.getStockActual();
		producto.setStockActual(0);
		bd.productos.insertar(producto);
		if (stockInicial > 0) {
			registrarMovimiento(producto.getId(), TipoMovimiento.AJUSTE, stockInicial, null, null);
		}
		return producto;
	}

	/** Modifica los datos del producto. El stock no se toca aqui. */
	public void modificarProducto(int id, Producto datos) {
		Producto existente = obtenerProducto(id);
		comprobarSkuLibre(datos.getSku(), id);
		comprobarProveedor(datos.getProveedorId());
		existente.copiarDatosDe(datos);
	}

	/** No se borra un producto que aparece en ventas, compras o movimientos. */
	public void eliminarProducto(int id) {
		obtenerProducto(id);
		if (bd.lineasVenta.existe(l -> l.getProductoId() == id) || bd.lineasCompra.existe(l -> l.getProductoId() == id)
				|| bd.inventario.existe(m -> m.getProductoId() == id)) {
			throw new ErpException("El producto tiene ventas, compras o movimientos y no se puede eliminar");
		}
		bd.productos.eliminar(id);
	}

	public List<Producto> productosBajoMinimo() {
		return bd.productos.filtrar(Producto::isBajoMinimo);
	}

	/** Valor del almacen a precio de coste. */
	public BigDecimal valorInventario() {
		BigDecimal valor = BigDecimal.ZERO;
		for (Producto p : bd.productos.todos()) {
			valor = valor.add(p.getPrecioCoste().multiply(BigDecimal.valueOf(p.getStockActual())));
		}
		return Importes.redondear(valor);
	}

	private void comprobarSkuLibre(String sku, int idPropio) {
		if (bd.productos.existe(p -> p.getSku().equalsIgnoreCase(sku) && p.getId() != idPropio)) {
			throw new ErpException("Ya existe un producto con SKU " + sku);
		}
	}

	private void comprobarProveedor(Integer proveedorId) {
		if (proveedorId != null) {
			bd.proveedores.obtener(proveedorId);
		}
	}

	// ------------------------------------------------------------------
	// Movimientos de stock
	// ------------------------------------------------------------------
	/** Ajuste manual (recuento, rotura...). La cantidad lleva signo. */
	public MovimientoInventario ajustarStock(int productoId, int cantidad) {
		return registrarMovimiento(productoId, TipoMovimiento.AJUSTE, cantidad, null, null);
	}

	/**
	 * Cambia el stock del producto y guarda el movimiento. Se valida todo
	 * antes de modificar nada, para que no quede a medias.
	 */
	public MovimientoInventario registrarMovimiento(int productoId, TipoMovimiento tipo, int cantidad, Integer ventaId,
			Integer compraId) {
		Producto producto = obtenerProducto(productoId);
		MovimientoInventario mov = new MovimientoInventario(productoId, tipo, cantidad, ventaId, compraId);
		int nuevoStock = producto.getStockActual() + mov.getEfectoEnStock();
		if (nuevoStock < 0) {
			throw new ErpException("Stock insuficiente de " + producto + ": hay " + producto.getStockActual());
		}
		producto.setStockActual(nuevoStock);
		return bd.inventario.insertar(mov);
	}

	public List<MovimientoInventario> listarMovimientos() {
		return bd.inventario.todos();
	}

	public List<MovimientoInventario> movimientosDeProducto(int productoId) {
		obtenerProducto(productoId);
		return bd.inventario.filtrar(m -> m.getProductoId() == productoId);
	}
}