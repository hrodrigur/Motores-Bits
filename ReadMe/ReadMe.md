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

# Funcionalidades, Requisitos, "Pliego de condiciones"

- Administrador

- El administrador puede crear, editar, eliminar y actualizar los distintos productos que tengamos.
- El administrador puede  crear, editar, eliminar las distintas categorias que tengamos.
- El administrador puede consultar y ver los detalles de los pedidos.
- El administrador puede actualizar el estado en el que se encuentran los pedidos.
- El administrador puede consultar, editar y eliminar el historial que tienen los distintos clientes.
- El administrador puede ver, moderar y eliminar las distintas reseñas que los clientes pongan.

- Cliente

- Los clientes deben poder registrarse e iniciar sesión.
- Los clientes pueden consultar las distintas categorias, buscar entre los productos y ver los detalles de los distintos productos.
- Los clientes pueden añadir y eliminar los productos que deseen al carro.
- Los clientes a la hora de realizar la compra hace un Checkout (el Checkout consiste en confirmar los productos que tengamos en el carro, una vez confirmados, el sistema crea un nuevo registro en la tabla pedidos con el id_usuario, fec_pedido, total y estado="Pendiente", el sistema debe crear uno o más registros en Detalles_Pedido uno por cada producto, vinculando id_pedido, id_producto, cantidad y el precio de ese momento. También se actualizará el Stock).
- Los clientes pueden ver sus pedidos, escribir reseñas en ellos y ver las reseñas que han escrito.

## Funcionalidades opcionales, recomendables o futuribles 

-   Recomendación de productos relacionados en base a la búsqueda o
    compras anteriores.

-   Posibilidad de que los clientes dejen reseñas y valoraciones de los
    productos.

-   Multilenguaje para la interfaz de la aplicación.
  
-   Posibilidad de que el cliente puede modificar su perfil.
