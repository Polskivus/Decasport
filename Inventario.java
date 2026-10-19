package erp;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gestión del inventario del ERP.
 *
 * Trabaja sobre dos tablas del script Azul_DB.sql:
 *   - Producto   : catálogo de artículos y stock actual.
 *   - inventario : trazabilidad de movimientos (ENTRADA / SALIDA / AJUSTE).
 *
 * Al registrar un movimiento, el stock de Producto se actualiza dentro de
 * una transacción, de modo que no puede quedar descuadrado con el histórico.
 */
public class Inventario {

    /** Tipos de movimiento admitidos por la tabla inventario. */
    public enum TipoMovimiento {
        ENTRADA, SALIDA, AJUSTE
    }

    // ==================================================================
    // PRODUCTOS
    // ==================================================================

    /** Devuelve todos los productos ordenados por nombre. */
    public List<Producto> listarProductos() throws SQLException {
        String sql = "SELECT id, sku, nombre, precio_venta, precio_coste, "
                + "stock_actual, stock_minimo, proveedor_id FROM Producto ORDER BY nombre";
        List<Producto> productos = new ArrayList<>();
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                productos.add(mapearProducto(rs));
            }
        }
        return productos;
    }

    /** Busca productos cuyo SKU o nombre contenga el texto indicado. */
    public List<Producto> buscarProductos(String texto) throws SQLException {
        String sql = "SELECT id, sku, nombre, precio_venta, precio_coste, "
                + "stock_actual, stock_minimo, proveedor_id FROM Producto "
                + "WHERE sku LIKE ? OR nombre LIKE ? ORDER BY nombre";
        String filtro = "%" + (texto == null ? "" : texto.trim()) + "%";
        List<Producto> productos = new ArrayList<>();
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, filtro);
            ps.setString(2, filtro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapearProducto(rs));
                }
            }
        }
        return productos;
    }

    /** Busca un producto por su id. */
    public Optional<Producto> buscarPorId(int id) throws SQLException {
        String sql = "SELECT id, sku, nombre, precio_venta, precio_coste, "
                + "stock_actual, stock_minimo, proveedor_id FROM Producto WHERE id = ?";
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapearProducto(rs)) : Optional.empty();
            }
        }
    }

    /** Busca un producto por su SKU (código único). */
    public Optional<Producto> buscarPorSku(String sku) throws SQLException {
        String sql = "SELECT id, sku, nombre, precio_venta, precio_coste, "
                + "stock_actual, stock_minimo, proveedor_id FROM Producto WHERE sku = ?";
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapearProducto(rs)) : Optional.empty();
            }
        }
    }

    /** Productos con stock igual o por debajo del mínimo (para reposición). */
    public List<Producto> listarStockBajo() throws SQLException {
        String sql = "SELECT id, sku, nombre, precio_venta, precio_coste, "
                + "stock_actual, stock_minimo, proveedor_id FROM Producto "
                + "WHERE stock_actual <= stock_minimo ORDER BY (stock_minimo - stock_actual) DESC";
        List<Producto> productos = new ArrayList<>();
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                productos.add(mapearProducto(rs));
            }
        }
        return productos;
    }

    /**
     * Inserta un producto nuevo y devuelve su id generado.
     * El stock inicial se guarda tal cual, sin generar movimiento.
     */
    public int insertarProducto(Producto producto) throws SQLException {
        validarProducto(producto);
        String sql = "INSERT INTO Producto "
                + "(sku, nombre, precio_venta, precio_coste, stock_actual, stock_minimo, proveedor_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, producto.getSku());
            ps.setString(2, producto.getNombre());
            ps.setBigDecimal(3, producto.getPrecioVenta());
            ps.setBigDecimal(4, producto.getPrecioCoste());
            ps.setInt(5, producto.getStockActual());
            ps.setInt(6, producto.getStockMinimo());
            if (producto.getProveedorId() == null) {
                ps.setNull(7, Types.INTEGER);
            } else {
                ps.setInt(7, producto.getProveedorId());
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    producto.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("No se pudo obtener el id del producto insertado.");
    }

    /**
     * Actualiza los datos de un producto (no el stock: para eso usa
     * {@link #registrarMovimiento}).
     */
    public boolean actualizarProducto(Producto producto) throws SQLException {
        validarProducto(producto);
        String sql = "UPDATE Producto SET sku = ?, nombre = ?, precio_venta = ?, precio_coste = ?, "
                + "stock_minimo = ?, proveedor_id = ? WHERE id = ?";
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, producto.getSku());
            ps.setString(2, producto.getNombre());
            ps.setBigDecimal(3, producto.getPrecioVenta());
            ps.setBigDecimal(4, producto.getPrecioCoste());
            ps.setInt(5, producto.getStockMinimo());
            if (producto.getProveedorId() == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, producto.getProveedorId());
            }
            ps.setInt(7, producto.getId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Elimina un producto. Devuelve false si no existe y lanza SQLException
     * si tiene movimientos o líneas asociadas (restricción de clave ajena).
     */
    public boolean eliminarProducto(int id) throws SQLException {
        String sql = "DELETE FROM Producto WHERE id = ?";
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ==================================================================
    // MOVIMIENTOS DE INVENTARIO
    // ==================================================================

    /**
     * Registra un movimiento en la tabla inventario y actualiza el stock:
     *   - ENTRADA: suma la cantidad al stock.
     *   - SALIDA : resta la cantidad (falla si no hay stock suficiente).
     *   - AJUSTE : fija el stock al valor indicado.
     *
     * @param productoId producto afectado
     * @param tipo       tipo de movimiento
     * @param cantidad   unidades (en AJUSTE es el stock final)
     * @param ventaId    venta de origen, o null
     * @param compraId   compra de origen, o null
     * @return id del movimiento creado
     */
    public int registrarMovimiento(int productoId, TipoMovimiento tipo, int cantidad,
                                   Integer ventaId, Integer compraId) throws SQLException {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de movimiento es obligatorio.");
        }
        if (tipo == TipoMovimiento.AJUSTE ? cantidad < 0 : cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad no es válida para un movimiento " + tipo + ".");
        }
        if (ventaId != null && compraId != null) {
            throw new IllegalArgumentException("Un movimiento no puede venir de una venta y una compra a la vez.");
        }

        try (Connection cn = Conexion.getConnection()) {
            cn.setAutoCommit(false);
            try {
                int stockActual = bloquearStock(cn, productoId);
                int nuevoStock;
                switch (tipo) {
                    case ENTRADA:
                        nuevoStock = stockActual + cantidad;
                        break;
                    case SALIDA:
                        if (cantidad > stockActual) {
                            throw new SQLException("Stock insuficiente: disponible " + stockActual
                                    + ", solicitado " + cantidad + ".");
                        }
                        nuevoStock = stockActual - cantidad;
                        break;
                    case AJUSTE:
                    default:
                        nuevoStock = cantidad;
                        break;
                }

                actualizarStock(cn, productoId, nuevoStock);
                int id = insertarMovimiento(cn, productoId, tipo, cantidad, ventaId, compraId);

                cn.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            } finally {
                cn.setAutoCommit(true);
            }
        }
    }

    /** Movimientos de un producto, del más reciente al más antiguo. */
    public List<Movimiento> listarMovimientos(int productoId) throws SQLException {
        String sql = "SELECT id, producto_id, tipo, cantidad, fecha, venta_id, compra_id "
                + "FROM inventario WHERE producto_id = ? ORDER BY fecha DESC, id DESC";
        List<Movimiento> movimientos = new ArrayList<>();
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movimientos.add(mapearMovimiento(rs));
                }
            }
        }
        return movimientos;
    }

    /** Todos los movimientos registrados, del más reciente al más antiguo. */
    public List<Movimiento> listarMovimientos() throws SQLException {
        String sql = "SELECT id, producto_id, tipo, cantidad, fecha, venta_id, compra_id "
                + "FROM inventario ORDER BY fecha DESC, id DESC";
        List<Movimiento> movimientos = new ArrayList<>();
        try (Connection cn = Conexion.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                movimientos.add(mapearMovimiento(rs));
            }
        }
        return movimientos;
    }

    // ==================================================================
    // MÉTODOS INTERNOS
    // ==================================================================

    /** Lee el stock bloqueando la fila para evitar condiciones de carrera. */
    private int bloquearStock(Connection cn, int productoId) throws SQLException {
        String sql = "SELECT stock_actual FROM Producto WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock_actual");
                }
            }
        }
        throw new SQLException("No existe el producto con id " + productoId + ".");
    }

    private void actualizarStock(Connection cn, int productoId, int nuevoStock) throws SQLException {
        String sql = "UPDATE Producto SET stock_actual = ? WHERE id = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, nuevoStock);
            ps.setInt(2, productoId);
            ps.executeUpdate();
        }
    }

    private int insertarMovimiento(Connection cn, int productoId, TipoMovimiento tipo,
                                   int cantidad, Integer ventaId, Integer compraId) throws SQLException {
        String sql = "INSERT INTO inventario (producto_id, tipo, cantidad, venta_id, compra_id) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, productoId);
            ps.setString(2, tipo.name());
            ps.setInt(3, cantidad);
            if (ventaId == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, ventaId);
            }
            if (compraId == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, compraId);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo obtener el id del movimiento insertado.");
    }

    private Producto mapearProducto(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getInt("id"));
        p.setSku(rs.getString("sku"));
        p.setNombre(rs.getString("nombre"));
        p.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        p.setPrecioCoste(rs.getBigDecimal("precio_coste"));
        p.setStockActual(rs.getInt("stock_actual"));
        p.setStockMinimo(rs.getInt("stock_minimo"));
        p.setProveedorId(rs.getObject("proveedor_id", Integer.class));
        return p;
    }

    private Movimiento mapearMovimiento(ResultSet rs) throws SQLException {
        Movimiento m = new Movimiento();
        m.setId(rs.getInt("id"));
        m.setProductoId(rs.getInt("producto_id"));
        m.setTipo(TipoMovimiento.valueOf(rs.getString("tipo")));
        m.setCantidad(rs.getInt("cantidad"));
        m.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        m.setVentaId(rs.getObject("venta_id", Integer.class));
        m.setCompraId(rs.getObject("compra_id", Integer.class));
        return m;
    }

    private void validarProducto(Producto p) {
        if (p == null) {
            throw new IllegalArgumentException("El producto no puede ser nulo.");
        }
        if (p.getSku() == null || p.getSku().trim().isEmpty()) {
            throw new IllegalArgumentException("El SKU es obligatorio.");
        }
        if (p.getNombre() == null || p.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (p.getPrecioVenta() == null || p.getPrecioVenta().signum() < 0
                || p.getPrecioCoste() == null || p.getPrecioCoste().signum() < 0) {
            throw new IllegalArgumentException("Los precios no pueden ser negativos.");
        }
        if (p.getStockActual() < 0 || p.getStockMinimo() < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo.");
        }
    }

    // ==================================================================
    // MODELOS AUXILIARES
    // ==================================================================

    /** Fila de la tabla Producto. */
    public static class Producto {

        private int id;
        private String sku;
        private String nombre;
        private BigDecimal precioVenta = BigDecimal.ZERO;
        private BigDecimal precioCoste = BigDecimal.ZERO;
        private int stockActual;
        private int stockMinimo;
        private Integer proveedorId;

        public Producto() {
        }

        public Producto(String sku, String nombre, BigDecimal precioVenta, BigDecimal precioCoste,
                        int stockActual, int stockMinimo, Integer proveedorId) {
            this.sku = sku;
            this.nombre = nombre;
            this.precioVenta = precioVenta;
            this.precioCoste = precioCoste;
            this.stockActual = stockActual;
            this.stockMinimo = stockMinimo;
            this.proveedorId = proveedorId;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public BigDecimal getPrecioVenta() {
            return precioVenta;
        }

        public void setPrecioVenta(BigDecimal precioVenta) {
            this.precioVenta = precioVenta;
        }

        public BigDecimal getPrecioCoste() {
            return precioCoste;
        }

        public void setPrecioCoste(BigDecimal precioCoste) {
            this.precioCoste = precioCoste;
        }

        public int getStockActual() {
            return stockActual;
        }

        public void setStockActual(int stockActual) {
            this.stockActual = stockActual;
        }

        public int getStockMinimo() {
            return stockMinimo;
        }

        public void setStockMinimo(int stockMinimo) {
            this.stockMinimo = stockMinimo;
        }

        public Integer getProveedorId() {
            return proveedorId;
        }

        public void setProveedorId(Integer proveedorId) {
            this.proveedorId = proveedorId;
        }

        /** Indica si el producto está en el mínimo o por debajo. */
        public boolean necesitaReposicion() {
            return stockActual <= stockMinimo;
        }

        @Override
        public String toString() {
            return String.format("Producto[id=%d, sku=%s, nombre=%s, pvp=%s, coste=%s, "
                            + "stock=%d/%d, proveedor=%s]",
                    id, sku, nombre, precioVenta.toPlainString(), precioCoste.toPlainString(),
                    stockActual, stockMinimo, proveedorId);
        }
    }

    /** Fila de la tabla inventario (trazabilidad). */
    public static class Movimiento {

        private int id;
        private int productoId;
        private TipoMovimiento tipo;
        private int cantidad;
        private LocalDateTime fecha;
        private Integer ventaId;
        private Integer compraId;

        public Movimiento() {
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

        @Override
        public String toString() {
            return String.format("Movimiento[id=%d, producto=%d, tipo=%s, cantidad=%d, fecha=%s, "
                            + "venta=%s, compra=%s]",
                    id, productoId, tipo, cantidad, fecha, ventaId, compraId);
        }
    }
}
