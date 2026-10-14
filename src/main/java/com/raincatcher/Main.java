package com.raincatcher;

import com.raincatcher.ui.GameWindow;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point for the RAIN CATCHER desktop arcade game.
 */
public class Main {

    public static void main(String[] args) {
        // Configure Direct3D acceleration for smooth Windows rendering
        System.setProperty("sun.java2d.d3d", "true");

        // Attempt system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable ignored) {
            // Default look and feel fallback
        }

        // Launch UI strictly on the Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.setVisible(true);
        });
    }
}

