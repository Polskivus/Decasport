# Decasport

Aplicación de gestión administrativa (ERP) para una tienda de deportes. Proyecto del Reto 1 de 2DM3.

Permite gestionar usuarios y permisos, clientes y proveedores, productos e inventario, ventas, compras y la emisión de albaranes y facturas.

## Tecnologías

- **Java 17**
- **Maven** (`Decasport/pom.xml`), con el código fuente en `Decasport/src`
- **PostgreSQL** mediante JDBC (`org.postgresql:postgresql` 42.7.14)
- Proyecto preparado para **Eclipse** (configuración en `Decasport/.settings`)

## Estructura

```
Decasport/
├── pom.xml
└── src/
    ├── modelo/       Entidades (una clase por tabla) y enums de estado
    ├── servicio/     Lógica de negocio de cada módulo del ERP
    ├── objetos/      Clases de datos para JDBC (Venta, Inventario)
    └── utilidades/   Conexión con PostgreSQL (Conexione)
```

### `modelo`

| Clase | Tabla / uso |
|---|---|
| `Usuario`, `Rol` | Usuarios del ERP y su rol. La contraseña se guarda como hash SHA-256 con sal |
| `Cliente` | Clientes (hereda de `Tercero`) |
| `Producto` | Productos con SKU, precios, stock actual, stock mínimo y proveedor |
| `MovimientoInventario` | Histórico de entradas, salidas y ajustes de stock |
| `Venta`, `LineaVenta` | Cabecera y líneas de venta |
| `Compra`, `LineaCompra` | Cabecera y líneas de compra |
| `DocumentoFiscal` | Albaranes y facturas |
| `EstadoVenta`, `EstadoCompra`, `EstadoDocumento` | Estados de cada documento |
| `Entidad` | Interfaz común: todas las entidades tienen un `id` entero |
| `ErpException` | Error de negocio o de validación |

### `servicio`

| Clase | Módulo |
|---|---|
| `Erp` | Punto de entrada: crea los servicios y carga datos de prueba |
| `AccesoServicio` | Login, usuarios y permisos por rol |
| `TercerosServicio` | Alta, modificación, baja y búsqueda de clientes y proveedores |
| `InventarioServicio` | Productos, stock y movimientos de inventario |
| `VentaServicio` | Ventas y sus cambios de estado |
| `CompraServicio` | Compras y sus cambios de estado |
| `DocumentoServicio` | Emisión, cobro y anulación de albaranes y facturas |
| `Importes` | Cálculo de totales con IVA único del 21 % |
| `Modulo` | Partes del ERP a las que se da acceso según el rol |

## Reglas de negocio principales

### Roles y permisos

| Rol | Módulos |
|---|---|
| `ADMIN` | Todos |
| `VENTAS` | Clientes, ventas, documentos |
| `COMPRAS` | Proveedores, compras, inventario |
| `ALMACEN` | Inventario, compras |
| `CONTABILIDAD` | Documentos, ventas, compras |

Un usuario no puede cambiar su propio rol ni desactivarse o eliminarse a sí mismo. Si un usuario tiene ventas o compras, no se elimina: se desactiva.

### Ventas

`BORRADOR → CONFIRMADA → ENTREGADA` (o `ANULADA`)

- En borrador se añaden y quitan líneas. El precio se copia del producto al crear la línea.
- Al confirmar se comprueba el stock de todas las líneas y después se descuenta (movimiento `SALIDA`).
- Al anular una venta confirmada, el stock vuelve al almacén. No se puede anular si está entregada o si tiene documentos sin anular.

### Compras

`BORRADOR → CONFIRMADA → RECIBIDA` (o `ANULADA`)

- La compra no guarda el proveedor: se deduce del proveedor de los productos de sus líneas.
- Al recibirla entra el stock (movimiento `ENTRADA`). Una compra recibida no se puede anular.

### Inventario

- El stock solo cambia a través de un movimiento de inventario, para que siempre quede registrado.
- El stock nunca puede quedar en negativo.
- No se puede eliminar un producto que aparece en ventas, compras o movimientos.

### Albaranes y facturas

- Se emiten para ventas confirmadas o entregadas, y para compras recibidas.
- Cada venta o compra tiene como máximo un albarán y una factura vigentes. La factura enlaza con el albarán si existe.
- Numeración automática por año: `ALB-2026-0001`, `FAC-2026-0001`…
- Estados: `EMITIDO → PAGADO` (solo facturas) o `ANULADO`. Un albarán incluido en una factura vigente no se puede anular.

## Base de datos

La conexión se configura en `Decasport/src/utilidades/Conexione.java`:

| Parámetro | Valor por defecto |
|---|---|
| Host | `localhost` |
| Puerto | `5432` |
| Base de datos | `erp` |
| Usuario | `postgres` |
| Contraseña | `postgres` |

Cambia estos valores según tu PostgreSQL local. Para comprobar la conexión, ejecuta el `main` de `Conexione`.

## Puesta en marcha

1. Clona el repositorio:
   ```bash
   git clone https://github.com/Polskivus/Decasport.git
   ```
2. En Eclipse: **File → Import → Maven → Existing Maven Projects** y selecciona la carpeta `Decasport`.
3. Crea la base de datos `erp` en PostgreSQL y ajusta los datos de `Conexione` si hace falta.

O desde la terminal, dentro de `Decasport/`:

```bash
mvn compile
```

### Datos de prueba

`Erp.cargarDatosDePrueba()` crea un usuario por rol, con la contraseña igual al nombre seguido de `123`:

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `ventas` | `ventas123` | VENTAS |
| `compras` | `compras123` | COMPRAS |
| `almacen` | `almacen123` | ALMACEN |
| `conta` | `conta123` | CONTABILIDAD |

También crea proveedores, clientes y productos de ejemplo, una venta con su albarán y su factura, y una compra confirmada pendiente de recibir.

## Estado actual

El proyecto está en desarrollo y **todavía no compila**:

- Faltan clases a las que hace referencia el código: `datos.BaseDatosMemoria`, `datos.Tabla`, `modelo.Tercero`, `modelo.Proveedor`, `modelo.Validar`, `modelo.TipoMovimiento` y `modelo.TipoDocumento`.
- `DocumentoFiscal`, `Entidad`, `ErpException`, `EstadoCompra` y `EstadoDocumento` declaran `package erp.modelo;`, pero están en la carpeta `modelo` y el resto del código usa `package modelo;`.
- Los servicios trabajan sobre una base de datos en memoria (`BaseDatosMemoria`). Queda pendiente pasarlos a PostgreSQL usando `Conexione`.
- Hay clases duplicadas entre paquetes: `objetos.Venta` / `modelo.Venta` y `objetos.Inventario` / `modelo.MovimientoInventario`.
- Todavía no hay una interfaz (consola o gráfica) con un `main` para usar el ERP.
- Los datos de prueba son de informática (ratones, teclados, monitores) en lugar de material deportivo.

## Equipo

Ramas de trabajo: `Joseba`, `Gahona`, `Marta` y `Polskivus`. Los cambios se integran en `master` mediante pull requests.
