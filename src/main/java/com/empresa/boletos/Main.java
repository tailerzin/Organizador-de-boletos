package com.empresa.boletos;

import com.empresa.boletos.gui.MainWindow;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // segue com o look and feel padrão se o do sistema não estiver disponível
            }
            new MainWindow().setVisible(true);
        });
    }
}
