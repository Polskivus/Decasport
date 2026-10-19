# Decasport

Aplicación de gestión administrativa para una tienda de deportes.

## Añadido por Gahona

En esta rama (**Gahona**) he incorporado la conexión a base de datos y el
modelo de inventario, además de la dependencia necesaria para el driver JDBC.

### 1. Dependencia: PostgreSQL

En el `pom.xml` se añadió el driver JDBC de PostgreSQL:

```xml
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
  <version>42.7.14</version>
</dependency>
```

Sin esta dependencia la clase de conexión no encuentra el driver
(`org.postgresql.Driver`) y no puede abrir la conexión.

### 2. Clase `utilidades/Conexione.java`

Utilidad encargada de abrir conexiones contra la base de datos PostgreSQL.

Métodos principales:

| Método | Descripción |
| --- | --- |
| `getConnection()` | Devuelve una `Connection` nueva. Hay que cerrarla. |
| `cerrar(AutoCloseable...)` | Cierra varios recursos JDBC ignorando los nulos. |
| `probarConexion()` | Comprueba que la base de datos responde. |
| `main(String[])` | Prueba rápida de conexión por consola. |

Los parámetros de conexión están como constantes al inicio de la clase
(`HOST`, `PUERTO`, `BASE_DATOS`, `USUARIO`, `PASSWORD`) y hay que ajustarlos
a tu PostgreSQL local.

Ejemplo de uso:

```java
Connection cn = Conexione.getConnection();
// ... usar la conexión ...
Conexione.cerrar(cn);
```

O mejor, con try-with-resources (se cierra sola):

```java
try (Connection cn = Conexione.getConnection();
     PreparedStatement ps = cn.prepareStatement("SELECT ...")) {
    // ...
}
```

### 3. Clase `objetos/Inventario.java`

Objeto (POJO) que representa una fila de la tabla `inventario`, es decir,
el histórico de movimientos de stock.

Campos:

- `id`
- `productoId`
- `tipo` (`Inventario.TipoMovimiento`: `ENTRADA`, `SALIDA`, `AJUSTE`)
- `cantidad`
- `fecha`
- `ventaId` (puede ser `null`)
- `compraId` (puede ser `null`)

Incluye constructores (vacío, alta y completo), getters/setters y los
métodos de ayuda `esEntrada()`, `esSalida()` y `esAjuste()`.

Ejemplo:

```java
Inventario movimiento = new Inventario(
        1, Inventario.TipoMovimiento.ENTRADA, 10, null, null);
movimiento.setFecha(LocalDateTime.now());
```

> La lógica de base de datos (insertar, listar, actualizar...) irá en
> `funciones/CRUD.java`; estas clases de `objetos` son solo datos.

## Estructura del proyecto

```
Decasport/
├── pom.xml
└── src/
    ├── funciones/    -> lógica de negocio y CRUD
    ├── objetos/      -> clases de datos (Compras, Inventario...)
    └── utilidades/   -> utilidades (Conexione)
```

## Cómo compilar y ejecutar

Con Maven:

```bash
mvn clean compile
```

Probar la conexión a la base de datos:

```bash
mvn exec:java -Dexec.mainClass="utilidades.Conexione"
```

O directamente desde el IDE (Eclipse / IntelliJ / VS Code) ejecutando el
`main` de `utilidades.Conexione`.
