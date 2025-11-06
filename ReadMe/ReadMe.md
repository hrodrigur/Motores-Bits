# Motores&Bits

# LOGO

<img src="Motores&Bits_G.png" style="width:300px;"/>

# Integrantes

-   Javier Prada Naharros 07272502V

<img src="Javier.jpg" style="width:200px;"/>

-   Hugo Rodríguez Galván 45971797H
  
<img src="Hugo.jpg" style="width:200px;"/>

# Eslogan

"Motores que giran, bits que conectan"

# Resumen

La aplicación web nos permitirá gestionar un taller de forma digital, el
administrador se encargará de añadir, modificar o eliminar objetos
(piezas de coches, recambios u otros elementos disponibles). Los
clientes podrán navegar por la aplicación, consultar los productos y
realizar compras de manera sencilla

# Descripción 

La aplicación está diseñada para optimizar la interacción entre un
taller y sus clientes.

El sistema cuenta con dos roles principales:

-   Administrador: encargado de gestionar el inventario, actualizar
    productos y mantener la información al día.

-   Cliente: usuario que podrá explorar los objetos disponibles, filtrar
    por categorías y efectuar compras si lo desea.

# Casos de Usos

### Cliente
1. **Registrarse / Iniciar sesión**
2. **Explorar catálogo por categoría**
3. **Ver detalle de producto** (incluye reseñas)
4. **Añadir al carro**
5. **Checkout**
    - Crea `PEDIDOS` (id_usuario, fec_pedido, total, estado="PENDIENTE").
    - Crea `DETALLES_PEDIDO` (id_pedido, id_producto, cantidad, precio del momento).
    - **Actualiza `PRODUCTOS.stock`**.
6. **Ver mis pedidos**
7. **Crear reseña** (puntuación 1–5)

### Administrador
8. **CRUD de categorías**
9. **CRUD de productos** (referencia única; stock ≥ 0)
10. **Consultar pedidos y su detalle**
11. **Actualizar estado del pedido** (PENDIENTE→PAGADO→ENVIADO→ENTREGADO; CANCELADO con reposición de stock si procede)
12. **Moderación de reseñas**

## Funcionalidades opcionales, recomendables o futuribles 

-   Recomendación de productos relacionados en base a la búsqueda o
    compras anteriores.

-   Posibilidad de que los clientes dejen reseñas y valoraciones de los
    productos.

-   Multilenguaje para la interfaz de la aplicación.
  
-   Posibilidad de que el cliente puede modificar su perfil.

# DiagramaE/R

<img src="diagramaER.png">

## Explicacion diagramaE/R

El diagrama representa un sistema de ventas para un taller, donde los usuarios realizan pedidos compuestos por varios productos.
Los productos se organizan en categorías y pueden recibir reseñas de los usuarios, con puntuación y comentario.
La relación entre pedidos y productos se resuelve con Detalles_Pedido, que guarda cantidad y el precio histórico de cada línea.

### Tablas

- USUARIOS (id_usuario PK)
Campos: nombre, email, contrasena, rol.

- CATEGORIAS (id_categoria PK)
Campos: nombre, descripcion.

- PRODUCTOS (id_producto PK, FK id_categoria)
  Campos: nombre, referencia, precio, stock.
  
- PEDIDOS (id_pedido PK, FK id_usuario)
  Campos: fec_pedido, total, estado.

- Detalles_Pedido (FK id_pedido, FK id_producto)
  Campos: cantidad, precio.

- RESEÑAS (id_reseña PK, FK id_producto, FK id_usuario)
  Campos: puntuacion, comentario.

### Relaciones (cardinalidades)

- Usuarios 1—N Pedidos: un usuario puede tener muchos pedidos; cada pedido pertenece a un usuario.
- Usuarios 1—N Reseñas: un usuario puede escribir muchas reseñas.
- Productos 1—N Reseñas: un producto puede recibir muchas reseñas.
- Categorías 1—N Productos: una categoría agrupa varios productos.
- Pedidos N—N Productos a través de Detalles_Pedido: un pedido tiene varios productos y un producto puede aparecer en muchos pedidos.

## Explicacion de docker con MariaDB

Aqui se va a explicar como crear un docker con la imagen de MariaDB

1. Tener descargado la aplicacion de docker en el dispositivo y funcionando.
2. Descargar la imagen de MariaDB (si no la tienes)
```bash
  docker pull mariadb:latest
```
3. Crear el contenedor con el nombre motores_bits
```bash
  docker run -d --name motores_bits -e MYSQL_ROOT_PASSWORD=proyectomdai -e MYSQL_DATABASE=MotoresBits -p 3306:3306 mariadb:latest
```
4. Verificar que el contenedor está corriendo
```bash
  docker ps
```
## Reglas y validaciones
- `Usuario.email` **único**.
- `Producto.referencia` **única**; `stock ≥ 0`.
- `DetallePedido.cantidad ≥ 1`.
- `Resena.puntuacion` ∈ [1..5].
- Estados de pedido con transiciones válidas.

## Flujo de Checkout 
1. Cliente confirma carro → **calcular total**.
2. Crear `PEDIDO` (estado = `PENDIENTE`).
3. Por cada ítem: crear `DETALLE_PEDIDO` con **precio snapshot** y **cantidad**, y **descontar stock**.
4. Devolver nº de pedido.
5. Admin puede **actualizar estado** posteriormente.

## Tecnología / Tests
- Spring Boot, JPA/Hibernate.
- BBDD de desarrollo/tests: **H2**; producción: MariaDB.
- Tests de integración con perfil `test` y **fábrica de datos** (H2 en memoria).