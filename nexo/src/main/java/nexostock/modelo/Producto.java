package nexostock.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Producto {

    public static final int DIAS_ALERTA_VENCIMIENTO = 30;

    public int idProducto;
    public String codigo;
    public String nombre;
    public int stockActual;
    public int stockMinimo;
    public int stockMaximo;
    public double consumoDiario;
    public int tiempoEntregaDias;
    public int stockSeguridad;
    public BigDecimal precioUnitario;
    public LocalDate fechaVencimiento;


    public int puntoReorden() {
        return (int) ((consumoDiario * tiempoEntregaDias) + stockSeguridad);
    }

    public long diasParaVencer() {
        return ChronoUnit.DAYS.between(LocalDate.now(), fechaVencimiento);
    }

    public String estado() {
        if (diasParaVencer() < 0) {
            return "Vencido";
        }
        if (stockActual > stockMaximo) {
            return "Alerta Sobre-stock";
        }
        if (diasParaVencer() <= DIAS_ALERTA_VENCIMIENTO) {
            return "Próximo a Vencer";
        }
        if (stockActual <= consumoDiario * tiempoEntregaDias + stockSeguridad) {
            return "Alerta Reorden";
        }
        return "Normal";
    }

    
    public String sugerencia() {
        switch (estado()) {
            case "Vencido":
                return "RETIRAR/LIQUIDAR " + stockActual + " u. (" + dinero(stockActual) + ") - producto vencido";
            case "Alerta Sobre-stock": {
                int exceso = stockActual - stockMaximo;
                return "LIQUIDAR " + exceso + " u. (" + dinero(exceso) + ") - excede el stock máximo de "
                        + stockMaximo + ". No comprar.";
            }
            case "Próximo a Vencer": {
                long dias = diasParaVencer();
                int vendible = (int) Math.floor(consumoDiario * dias);
                int liquidar = Math.max(0, stockActual - vendible);
                return liquidar == 0
                        ? "VENCE EN " + dias + " DÍAS - el consumo esperado (" + vendible + " u.) cubre el stock actual"
                        : "LIQUIDAR " + liquidar + " u. (" + dinero(liquidar) + ") - vence en " + dias
                        + " días y solo se espera vender " + vendible;
            }
            case "Alerta Reorden": {
                int comprar = Math.max(1, stockMaximo - stockActual);
                return "COMPRAR " + comprar + " u. (" + dinero(comprar) + ") - stock " + stockActual
                        + " <= punto de reorden " + puntoReorden();
            }
            default:
                return null;
        }
    }

    private String dinero(int unidades) {
        return "Q" + precioUnitario.multiply(BigDecimal.valueOf(unidades)).setScale(2, RoundingMode.HALF_UP);
    }
}
