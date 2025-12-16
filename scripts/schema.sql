-- Schema SQL para MotoresBits (moved here to avoid automatic execution by Spring)
-- Ejecuta manualmente con mysql o a traves del contenedor Docker cuando quieras (ver README o scripts/recreate_db.ps1).

-- Asegurarse de eliminar tablas previas (orden: hijos primero)
DROP TABLE IF EXISTS resenas;
DROP TABLE IF EXISTS detalles_pedido;
DROP TABLE IF EXISTS pedidos;
DROP TABLE IF EXISTS productos;
DROP TABLE IF EXISTS categorias;
DROP TABLE IF EXISTS usuarios;

-- Tabla de usuarios
CREATE TABLE usuarios (
  id_usuario BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(255),
  email VARCHAR(255) NOT NULL UNIQUE,
  contrasena VARCHAR(255) NOT NULL,
  rol VARCHAR(50),
  direccion VARCHAR(512),
  telefono VARCHAR(50),
  saldo DECIMAL(10,2) NOT NULL DEFAULT 0
) ENGINE=InnoDB;

-- Categorías
CREATE TABLE categorias (
  id_categoria BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(255),
  descripcion TEXT
) ENGINE=InnoDB;

-- Productos (FK sin nombre explícito)
CREATE TABLE productos (
  id_producto BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_categoria BIGINT NOT NULL,
  nombre VARCHAR(255) NOT NULL,
  referencia VARCHAR(255) NOT NULL UNIQUE,
  precio DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  version INT,
  imagen VARCHAR(120),
  imagen_url VARCHAR(1000) NULL,
  FOREIGN KEY (id_categoria) REFERENCES categorias(id_categoria)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Pedidos (FK sin nombre explícito)
CREATE TABLE pedidos (
  id_pedido BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_usuario BIGINT NOT NULL,
  fec_pedido DATE NOT NULL,
  estado VARCHAR(50) NOT NULL,
  total DECIMAL(10,2) NOT NULL,
  FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Detalles de pedido (PK compuesta, FKs sin nombre explícito)
CREATE TABLE detalles_pedido (
  id_pedido BIGINT NOT NULL,
  id_producto BIGINT NOT NULL,
  cantidad INT NOT NULL,
  precio DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (id_pedido, id_producto),
  FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido)
    ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Reseñas (FKs sin nombre explícito)
CREATE TABLE resenas (
  id_resena BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_producto BIGINT NOT NULL,
  id_usuario BIGINT NOT NULL,
  puntuacion INT NOT NULL,
  comentario TEXT,
  creada_en TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_puntuacion CHECK (puntuacion >= 1 AND puntuacion <= 5)
) ENGINE=InnoDB;

-- Índices adicionales
CREATE INDEX idx_producto_categoria ON productos(id_categoria);
CREATE INDEX idx_pedido_usuario ON pedidos(id_usuario);
CREATE INDEX idx_resena_producto ON resenas(id_producto);
CREATE INDEX idx_resena_usuario ON resenas(id_usuario);

-- Datos iniciales (ejemplos coherentes con la temática "Motores & Bits")
INSERT INTO usuarios (id_usuario, nombre, email, contrasena, rol, direccion, telefono,saldo) VALUES
  (1, 'Admin', 'admin@example.com', 'admin', 'ADMIN', 'C/ Administrador 1', '600000000', 99999.99),
  (2, 'Cliente Ejemplo', 'cliente@example.com', 'cliente123', 'CLIENTE', 'C/ Cliente 2', '600000001', 1500.00);

INSERT INTO categorias (id_categoria, nombre, descripcion) VALUES
  (1, 'Motores', 'Piezas y motores de combustion y sus variantes'),
  (2, 'Electronica', 'Unidades de control, sensores y componentes electronicos'),
  (3, 'Transmision', 'Embragues, cajas de cambio y piezas de transmision'),
  (4, 'Accesorios', 'Filtros, tornilleria y accesorios varios');

INSERT INTO productos (id_producto, id_categoria, nombre, referencia, precio, stock, version, imagen,imagen_url) VALUES
(1, 1, 'Motor V8 5.0L', 'MV8-001', 4999.99, 5, 1, 'Motor_V8_5_0L.jpg',NULL),
(2, 2, 'ECU Controlador X100', 'ECU-100', 899.90, 10, 1, 'ECU_Controlador_X100.jpg',NULL),
(3, 3, 'Kit Embrague Deportivo', 'KEM-200', 149.99, 20, 1, 'Kit_Embrague_Deportivo.jpg',NULL),
(4, 4, 'Filtro de Aceite Premium', 'FA-300', 19.99, 50, 1, 'Filtro_de_Aceite_Premium.jpg',NULL),
(5, 2, 'Sensor de Oxigeno Bosch', 'SOX-050', 29.50, 30, 1, 'Sensor_de_Oxigeno_Bosch.jpg',NULL);

-- Pedido de ejemplo para el cliente (con lineas de pedido)
INSERT INTO pedidos (id_pedido, id_usuario, fec_pedido, estado, total) VALUES
  (1, 2, '2025-11-29', 'PENDIENTE', 5299.97);

INSERT INTO detalles_pedido (id_pedido, id_producto, cantidad, precio) VALUES
  (1, 1, 1, 4999.99),
  (1, 3, 2, 149.99);

-- Reseñas de ejemplo
INSERT INTO resenas (id_resena, id_producto, id_usuario, puntuacion, comentario, creada_en) VALUES
  (1, 1, 2, 5, 'Excelente motor, potente y fiable.', '2025-11-28 10:00:00'),
  (2, 3, 2, 4, 'Buen embrague por el precio.', '2025-11-27 15:30:00');
