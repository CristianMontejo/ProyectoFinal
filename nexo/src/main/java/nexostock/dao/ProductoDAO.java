package nexostock.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import nexostock.db.Conexion;
import nexostock.modelo.Producto;

public class ProductoDAO {

 
    public List<Producto> listar(boolean consumoAuto) throws Exception {
        String sql = "SELECT p.IdProducto, p.Codigo, p.Nombre, p.StockActual, p.StockMinimo, p.StockMaximo, "
                + "p.ConsumoDiarioPromedio, p.TiempoEntregaDias, p.StockSeguridad, p.PrecioUnitario, p.FechaVencimiento, "
                + "ISNULL((SELECT SUM(m.Cantidad) FROM MovimientosInventario m WHERE m.IdProducto = p.IdProducto "
                + "AND m.TipoMovimiento = 'VENTA' AND m.FechaMovimiento >= DATEADD(day, -30, GETDATE())), 0) / 30.0 "
                + "FROM Productos p ORDER BY p.Codigo";
        List<Producto> lista = new ArrayList<>();
        try (Connection cn = Conexion.conectar(); ResultSet rs = cn.createStatement().executeQuery(sql)) {
            while (rs.next()) {
                Producto p = new Producto();
                p.idProducto = rs.getInt(1);
                p.codigo = rs.getString(2);
                p.nombre = rs.getString(3);
                p.stockActual = rs.getInt(4);
                p.stockMinimo = rs.getInt(5);
                p.stockMaximo = rs.getInt(6);
                p.consumoDiario = consumoAuto ? rs.getDouble(12) : rs.getDouble(7);
                p.tiempoEntregaDias = rs.getInt(8);
                p.stockSeguridad = rs.getInt(9);
                p.precioUnitario = rs.getBigDecimal(10);
                p.fechaVencimiento = rs.getDate(11).toLocalDate();
                lista.add(p);
            }
        }
        return lista;
    }

    public void insertar(Producto p) throws Exception {
        String sql = "INSERT INTO Productos (Codigo, Nombre, StockActual, StockMinimo, StockMaximo, "
                + "ConsumoDiarioPromedio, TiempoEntregaDias, StockSeguridad, PrecioUnitario, FechaVencimiento) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection cn = Conexion.conectar(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, p.codigo);
            ps.setString(2, p.nombre);
            ps.setInt(3, p.stockActual);
            ps.setInt(4, p.stockMinimo);
            ps.setInt(5, p.stockMaximo);
            ps.setDouble(6, p.consumoDiario);
            ps.setInt(7, p.tiempoEntregaDias);
            ps.setInt(8, p.stockSeguridad);
            ps.setBigDecimal(9, p.precioUnitario);
            ps.setDate(10, Date.valueOf(p.fechaVencimiento));
            ps.executeUpdate();
        }
    }

    public void registrarMovimiento(int idProducto, String tipo, int cantidad) throws Exception {
        boolean ajuste = tipo.equals("AJUSTE");
        int guardada = ajuste ? cantidad : Math.abs(cantidad);
        int delta = (tipo.equals("VENTA") || tipo.equals("LIQUIDACION")) ? -guardada : guardada;
        try (Connection cn = Conexion.conectar()) {
            cn.setAutoCommit(false);
            try {
                PreparedStatement ps = cn.prepareStatement(
                        "UPDATE Productos SET StockActual = StockActual + ? WHERE IdProducto = ? AND StockActual + ? >= 0");
                ps.setInt(1, delta);
                ps.setInt(2, idProducto);
                ps.setInt(3, delta);
                if (ps.executeUpdate() == 0) {
                    throw new Exception("El movimiento dejaría el stock en negativo");
                }
                ps = cn.prepareStatement(
                        "INSERT INTO MovimientosInventario (IdProducto, TipoMovimiento, Cantidad) VALUES (?, ?, ?)");
                ps.setInt(1, idProducto);
                ps.setString(2, tipo);
                ps.setInt(3, guardada);
                ps.executeUpdate();
                cn.commit();
            } catch (Exception e) {
                cn.rollback();
                throw e;
            }
        }
    }
}
