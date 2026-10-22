package nexostock;

import nexostock.vista.Ventana;

public class NexoStock {

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new Ventana().setVisible(true));
    }
}
