package nexostock.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import nexostock.dao.ProductoDAO;
import nexostock.modelo.Producto;

public class Ventana extends JFrame {

    private final ProductoDAO dao = new ProductoDAO();
    private List<Producto> productos = List.of();

    private final JLabel lblTitulo = new JLabel("NexoStock - Control y Prevención de Sobre-Stock - Módulo Bodega", SwingConstants.CENTER);
    private final DefaultTableModel mInv = new DefaultTableModel(new String[]{
        "Código", "Producto", "Stock Actual", "Stock Máx.", "Punto Reorden", "Vencimiento", "Estado"}, 0);
    private final JTable dgvInventario = new JTable(mInv);
    private final JButton btnGenerarAlerta = new JButton("Generar Alertas");
    private final JButton btnMovimiento = new JButton("Registrar movimiento");
    private final JButton btnNuevo = new JButton("Nuevo producto");
    private final JButton btnActualizar = new JButton("Actualizar");
    private final JCheckBox chkConsumoAuto = new JCheckBox("Calcular consumo diario con ventas de los últimos 30 días");
    private final JTextArea txtAlertas = new JTextArea();

    public Ventana() {
        setTitle("NexoStock");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 640);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));

        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 18f));
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(10, 8, 4, 8));

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.add(btnGenerarAlerta);
        botones.add(btnMovimiento);
        botones.add(btnNuevo);
        botones.add(btnActualizar);
        botones.add(chkConsumoAuto);
        JPanel norte = new JPanel(new BorderLayout(5, 5));
        norte.add(lblTitulo, BorderLayout.NORTH);
        norte.add(botones, BorderLayout.CENTER);
        norte.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 8));
        add(norte, BorderLayout.NORTH);

        dgvInventario.setDefaultEditor(Object.class, null);
        dgvInventario.setRowHeight(24);
        dgvInventario.setAutoCreateRowSorter(true);
        dgvInventario.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                String est = t.getValueAt(r, 6).toString();
                Color fondo;
                switch (est) {
                    case "Alerta Sobre-stock":
                        fondo = new Color(255, 200, 200);
                        break;
                    case "Próximo a Vencer":
                        fondo = new Color(255, 225, 170);
                        break;
                    case "Vencido":
                        fondo = new Color(230, 160, 160);
                        break;
                    case "Alerta Reorden":
                        fondo = new Color(255, 245, 190);
                        break;
                    default:
                        fondo = new Color(200, 245, 200);
                }
                comp.setBackground(sel ? fondo.darker() : fondo);
                comp.setForeground(Color.BLACK);
                return comp;
            }
        });
        JScrollPane sInv = new JScrollPane(dgvInventario);
        sInv.setBorder(BorderFactory.createTitledBorder("Inventario"));
        JPanel centro = new JPanel(new BorderLayout());
        centro.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        centro.add(sInv, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        txtAlertas.setEditable(false);
        txtAlertas.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        txtAlertas.setText("Presiona \"Generar Alertas\" para calcular las sugerencias de compra o liquidación.\n");
        JScrollPane sAl = new JScrollPane(txtAlertas);
        sAl.setBorder(BorderFactory.createTitledBorder("Alertas y sugerencias"));
        sAl.setPreferredSize(new Dimension(0, 190));
        JPanel sur = new JPanel(new BorderLayout());
        sur.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        sur.add(sAl, BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);

        btnGenerarAlerta.addActionListener(e -> generarAlertas());
        btnMovimiento.addActionListener(e -> registrarMovimiento());
        btnNuevo.addActionListener(e -> nuevoProducto());
        btnActualizar.addActionListener(e -> cargar());
        chkConsumoAuto.addActionListener(e -> cargar());
        cargar();
    }

    private void cargar() {
        try {
            productos = dao.listar(chkConsumoAuto.isSelected());
            mInv.setRowCount(0);
            for (Producto p : productos) {
                mInv.addRow(new Object[]{p.codigo, p.nombre, p.stockActual, p.stockMaximo,
                    p.puntoReorden(), p.fechaVencimiento, p.estado()});
            }
        } catch (Exception e) {
            error(e);
        }
    }

    private void generarAlertas() {
        cargar();
        StringBuilder sb = new StringBuilder();
        BigDecimal total = BigDecimal.ZERO;
        int n = 0;
        for (Producto p : productos) {
            String s = p.sugerencia();
            if (s != null) {
                n++;
                sb.append("[").append(p.estado()).append("] ").append(p.codigo).append(" ")
                        .append(p.nombre).append("\n    -> ").append(s).append("\n");
            }
        }
        if (n == 0) {
            sb.append("Sin alertas: todo el inventario está en estado Normal.\n");
        } else {
            sb.insert(0, n + " alerta(s) generada(s) - " + LocalDate.now() + "\n\n");
        }
        txtAlertas.setText(sb.toString());
        txtAlertas.setCaretPosition(0);
    }

    private void registrarMovimiento() {
        if (productos.isEmpty()) {
            return;
        }
        JComboBox<String> cbProd = new JComboBox<>();
        for (Producto p : productos) {
            cbProd.addItem(p.codigo + " - " + p.nombre);
        }
        int fila = dgvInventario.getSelectedRow();
        if (fila >= 0) {
            cbProd.setSelectedIndex(dgvInventario.convertRowIndexToModel(fila));
        }
        JComboBox<String> cbTipo = new JComboBox<>(new String[]{"ENTREGA_PROVEEDOR", "VENTA", "LIQUIDACION", "AJUSTE"});
        JTextField txtCant = new JTextField();
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Producto:"));
        panel.add(cbProd);
        panel.add(new JLabel("Tipo de movimiento:"));
        panel.add(cbTipo);
        panel.add(new JLabel("Cantidad (AJUSTE admite negativos):"));
        panel.add(txtCant);
        if (JOptionPane.showConfirmDialog(this, panel, "Registrar movimiento",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            int cant = Integer.parseInt(txtCant.getText().trim());
            if (cant == 0) {
                throw new Exception("La cantidad no puede ser 0");
            }
            dao.registrarMovimiento(productos.get(cbProd.getSelectedIndex()).idProducto,
                    (String) cbTipo.getSelectedItem(), cant);
            cargar();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "La cantidad debe ser un número entero", "Dato inválido", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            error(e);
        }
    }

    private void nuevoProducto() {
        JTextField codigo = new JTextField(), nombre = new JTextField(), stock = new JTextField(),
                min = new JTextField(), max = new JTextField(), consumo = new JTextField("0"),
                entrega = new JTextField(), seg = new JTextField(), precio = new JTextField(),
                vence = new JTextField(LocalDate.now().plusMonths(6).toString());
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        Object[][] campos = {{"Código:", codigo}, {"Nombre:", nombre}, {"Stock actual:", stock},
        {"Stock mínimo:", min}, {"Stock máximo:", max}, {"Consumo diario promedio:", consumo},
        {"Tiempo de entrega (días):", entrega}, {"Stock de seguridad:", seg},
        {"Precio unitario:", precio}, {"Vencimiento (AAAA-MM-DD):", vence}};
        for (Object[] c : campos) {
            panel.add(new JLabel((String) c[0]));
            panel.add((Component) c[1]);
        }
        if (JOptionPane.showConfirmDialog(this, panel, "Nuevo producto",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            Producto p = new Producto();
            p.codigo = codigo.getText().trim();
            p.nombre = nombre.getText().trim();
            if (p.codigo.isEmpty() || p.nombre.isEmpty()) {
                throw new Exception("Código y nombre son obligatorios");
            }
            p.stockActual = Integer.parseInt(stock.getText().trim());
            p.stockMinimo = Integer.parseInt(min.getText().trim());
            p.stockMaximo = Integer.parseInt(max.getText().trim());
            p.consumoDiario = Double.parseDouble(consumo.getText().trim());
            p.tiempoEntregaDias = Integer.parseInt(entrega.getText().trim());
            p.stockSeguridad = Integer.parseInt(seg.getText().trim());
            p.precioUnitario = new BigDecimal(precio.getText().trim());
            p.fechaVencimiento = LocalDate.parse(vence.getText().trim());
            if (p.stockMaximo < p.stockMinimo) {
                throw new Exception("El stock máximo no puede ser menor al mínimo");
            }
            dao.insertar(p);
            cargar();
        } catch (NumberFormatException | java.time.format.DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Revisa los datos numéricos y la fecha (AAAA-MM-DD)", "Dato inválido", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            error(e);
        }
    }

    private void error(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
