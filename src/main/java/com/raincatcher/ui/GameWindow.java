package com.raincatcher.ui;

import com.raincatcher.config.GameConfig;

import javax.swing.JFrame;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * Main application window for Rain Catcher.
 */
public class GameWindow extends JFrame {

    private final GamePanel gamePanel;

    public GameWindow() {
        super(GameConfig.GAME_TITLE);

        this.gamePanel = new GamePanel();
        add(gamePanel);

        // Window properties
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);

        // Programmatically generate cute game window icon
        setIconImage(createAppIcon());

        // Clean shutdown listener
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                gamePanel.stopGameTimer();
            }
        });
    }

    /**
     * Programmatically renders a crisp raindrop icon for the window title bar and taskbar.
     */
    private BufferedImage createAppIcon() {
        int size = 64;
        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Circular background
        g.setColor(new Color(25, 45, 80));
        g.fillOval(2, 2, size - 4, size - 4);
        g.setColor(new Color(90, 160, 245));
        g.drawOval(2, 2, size - 4, size - 4);

        // Teardrop path
        Path2D.Double drop = new Path2D.Double();
        drop.moveTo(32, 10);
        drop.curveTo(48, 26, 52, 42, 32, 54);
        drop.curveTo(12, 42, 16, 26, 32, 10);
        drop.closePath();

        GradientPaint dropGrad = new GradientPaint(
            32, 10, new Color(140, 220, 255),
            32, 54, new Color(20, 120, 240)
        );
        g.setPaint(dropGrad);
        g.fill(drop);

        // Highlight
        g.setColor(new Color(255, 255, 255, 220));
        g.fillOval(24, 22, 10, 14);

        g.dispose();
        return icon;
    }

    public GamePanel getGamePanel() {
        return gamePanel;
    }
}

