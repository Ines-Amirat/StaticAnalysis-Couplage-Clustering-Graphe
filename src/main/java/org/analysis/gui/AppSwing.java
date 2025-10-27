package org.analysis.gui;

import javax.swing.*;

public class AppSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new AppFrame().setVisible(true);
        });
    }
}