-- NexoStock: Control de Inventario y Prevención de Sobre-stock (SQL Server Express)
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'ControlInventarioDB')
    CREATE DATABASE ControlInventarioDB;
GO
USE ControlInventarioDB;
GO
IF OBJECT_ID('MovimientosInventario', 'U') IS NOT NULL DROP TABLE MovimientosInventario;
IF OBJECT_ID('Productos', 'U') IS NOT NULL DROP TABLE Productos;
GO
CREATE TABLE Productos (
    IdProducto INT IDENTITY(1,1) PRIMARY KEY,
    Codigo VARCHAR(50) NOT NULL UNIQUE,
    Nombre VARCHAR(100) NOT NULL,
    StockActual INT NOT NULL CHECK (StockActual >= 0),
    StockMinimo INT NOT NULL CHECK (StockMinimo >= 0),
    StockMaximo INT NOT NULL,
    ConsumoDiarioPromedio DECIMAL(10,2) NOT NULL CHECK (ConsumoDiarioPromedio >= 0),
    TiempoEntregaDias INT NOT NULL CHECK (TiempoEntregaDias >= 0),
    StockSeguridad INT NOT NULL CHECK (StockSeguridad >= 0),
    PrecioUnitario DECIMAL(10,2) NOT NULL CHECK (PrecioUnitario >= 0),
    FechaVencimiento DATE NOT NULL,
    CONSTRAINT CK_Stock_Max CHECK (StockMaximo >= StockMinimo)
);
GO
CREATE TABLE MovimientosInventario (
    IdMovimiento INT IDENTITY(1,1) PRIMARY KEY,
    IdProducto INT NOT NULL,
    TipoMovimiento VARCHAR(30) NOT NULL
        CHECK (TipoMovimiento IN ('ENTREGA_PROVEEDOR','VENTA','LIQUIDACION','AJUSTE')),
    Cantidad INT NOT NULL,
    FechaMovimiento DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Movimiento_Producto FOREIGN KEY (IdProducto) REFERENCES Productos(IdProducto)
);
GO
-- Las fechas de vencimiento de los ejemplos son relativas a hoy para que la demo siempre funcione
DECLARE @hoy DATE = CAST(GETDATE() AS DATE);
INSERT INTO Productos (Codigo, Nombre, StockActual, StockMinimo, StockMaximo, ConsumoDiarioPromedio, TiempoEntregaDias, StockSeguridad, PrecioUnitario, FechaVencimiento) VALUES
('ABAR-001', 'Arroz Blanco 5kg',   150, 40, 300, 10.0, 3, 20, 45.50, DATEADD(day, 300, @hoy)), -- Normal
('ABAR-002', 'Aceite Vegetal 1L',  420, 50, 350, 15.0, 4, 30, 18.00, DATEADD(day, 110, @hoy)), -- Sobre-stock
('ABAR-003', 'Yogurt Natural 1L',   60, 20, 150,  8.0, 2, 10, 22.50, DATEADD(day,  15, @hoy)), -- Próximo a vencer
('ABAR-004', 'Frijol Negro 1kg',    25, 30, 200, 12.0, 3, 15, 14.00, DATEADD(day, 240, @hoy)), -- Reorden
('ABAR-005', 'Azúcar Morena 2kg',  110, 25, 250,  9.0, 3, 12, 20.00, DATEADD(day, 400, @hoy)); -- Normal
GO
INSERT INTO MovimientosInventario (IdProducto, TipoMovimiento, Cantidad, FechaMovimiento) VALUES
(1, 'ENTREGA_PROVEEDOR', 100, DATEADD(day, -27, GETDATE())),
(2, 'ENTREGA_PROVEEDOR', 400, DATEADD(day, -32, GETDATE())),
(1, 'VENTA', 300, DATEADD(day, -10, GETDATE())),
(3, 'VENTA',  15, DATEADD(day,  -3, GETDATE())),
(4, 'VENTA',  25, DATEADD(day,  -4, GETDATE()));
GO
-- Permisos para el usuario que ya usa la aplicación (ajusta si usas otro)
IF EXISTS (SELECT * FROM sys.server_principals WHERE name = 'bodega_user')
BEGIN
    IF NOT EXISTS (SELECT * FROM sys.database_principals WHERE name = 'bodega_user')
        CREATE USER bodega_user FOR LOGIN bodega_user;
    ALTER ROLE db_datareader ADD MEMBER bodega_user;
    ALTER ROLE db_datawriter ADD MEMBER bodega_user;
END
GO
SELECT * FROM Productos;
