package com.raincatcher.ui;

import com.raincatcher.audio.SoundManager;
import com.raincatcher.config.GameConfig;
import com.raincatcher.logic.CollisionManager;
import com.raincatcher.logic.DifficultyManager;
import com.raincatcher.logic.ScoreManager;
import com.raincatcher.logic.SpawnManager;
import com.raincatcher.model.Basket;
import com.raincatcher.model.Cloud;
import com.raincatcher.model.FallingObject;
import com.raincatcher.model.GameState;
import com.raincatcher.model.Particle;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Main game panel containing the 60 FPS update-and-render loop,
 * game entities, input handling, and state transitions.
 */
public class GamePanel extends JPanel {

    private final Renderer renderer;
    private final SoundManager soundManager;
    private final ScoreManager scoreManager;
    private final DifficultyManager difficultyManager;
    private final SpawnManager spawnManager;
    private final CollisionManager collisionManager;

    private GameState state = GameState.MENU;

    private Basket basket;
    private final List<FallingObject> fallingObjects = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Cloud> clouds = new ArrayList<>();

    // Single master Swing Timer (never duplicated)
    private final Timer gameLoopTimer;
    private long lastTickTime = 0;

    // Screen Shake effect
    private double shakeIntensity = 0;
    private final Random rand = new Random();

    public GamePanel() {
        this.renderer = new Renderer();
        this.soundManager = new SoundManager();
        this.scoreManager = new ScoreManager();
        this.difficultyManager = new DifficultyManager();
        this.spawnManager = new SpawnManager();
        this.collisionManager = new CollisionManager();

        setPreferredSize(new Dimension(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT));
        setDoubleBuffered(true);
        setFocusable(true);

        initializeEntities();
        setupInputListeners();

        // 60 FPS Master Timer
        this.gameLoopTimer = new Timer(GameConfig.TIMER_DELAY_MS, e -> gameLoopStep());
        this.lastTickTime = System.currentTimeMillis();
        this.gameLoopTimer.start();
    }

    private void initializeEntities() {
        // Player Basket
        double startX = (GameConfig.WINDOW_WIDTH - GameConfig.BASKET_WIDTH) / 2.0;
        this.basket = new Basket(startX, GameConfig.BASKET_START_Y,
                GameConfig.BASKET_WIDTH, GameConfig.BASKET_HEIGHT, GameConfig.BASKET_SPEED);

        // Clouds for parallax background
        clouds.clear();
        for (int i = 0; i < GameConfig.CLOUD_COUNT; i++) {
            clouds.add(Cloud.createRandom(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, rand));
        }
    }

    private void setupInputListeners() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPressed(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                handleKeyReleased(e.getKeyCode());
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (state == GameState.PLAYING) {
                    basket.setMouseTargetX(e.getX());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (state == GameState.PLAYING) {
                    basket.setMouseTargetX(e.getX());
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (state == GameState.MENU || state == GameState.GAME_OVER) {
                    soundManager.playClick();
                    startGame();
                }
            }
        });
    }

    private void handleKeyPressed(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                if (state == GameState.PLAYING) {
                    basket.setMovingLeft(true);
                }
                break;

            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                if (state == GameState.PLAYING) {
                    basket.setMovingRight(true);
                }
                break;

            case KeyEvent.VK_ESCAPE:
                if (state == GameState.PLAYING) {
                    state = GameState.PAUSED;
                    soundManager.playClick();
                    soundManager.pauseMusic();
                } else if (state == GameState.PAUSED) {
                    state = GameState.PLAYING;
                    soundManager.playClick();
                    soundManager.playMusic();
                } else if (state == GameState.GAME_OVER) {
                    state = GameState.MENU;
                    soundManager.playClick();
                    soundManager.pauseMusic();
                }
                break;

            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_ENTER:
                if (state == GameState.MENU || state == GameState.GAME_OVER) {
                    soundManager.playClick();
                    startGame();
                } else if (state == GameState.PAUSED) {
                    // Restart from pause
                    soundManager.playClick();
                    startGame();
                }
                break;

            case KeyEvent.VK_M:
                soundManager.toggleMute();
                break;

            case KeyEvent.VK_Q:
                if (state == GameState.PAUSED) {
                    state = GameState.MENU;
                    soundManager.playClick();
                    soundManager.pauseMusic();
                }
                break;

            default:
                break;
        }
    }

    private void handleKeyReleased(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                basket.setMovingLeft(false);
                break;

            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                basket.setMovingRight(false);
                break;

            default:
                break;
        }
    }

    /**
     * Starts or restarts the game session.
     */
    public void startGame() {
        int w = getWidth() > 0 ? getWidth() : GameConfig.WINDOW_WIDTH;
        basket.reset(w);

        fallingObjects.clear();
        particles.clear();
        scoreManager.reset();
        difficultyManager.reset();
        spawnManager.reset();

        shakeIntensity = 0;
        state = GameState.PLAYING;
        soundManager.playMusic();
        lastTickTime = System.currentTimeMillis();
    }

    /**
     * The single controlled game loop tick called by javax.swing.Timer.
     */
    private void gameLoopStep() {
        long now = System.currentTimeMillis();
        long delta = now - lastTickTime;
        if (delta <= 0) delta = 1;
        if (delta > 100) delta = 100; // Clamp delta to avoid huge physics jumps
        lastTickTime = now;

        int currentWidth = getWidth() > 0 ? getWidth() : GameConfig.WINDOW_WIDTH;
        int currentHeight = getHeight() > 0 ? getHeight() : GameConfig.WINDOW_HEIGHT;

        // Clouds drift continuously in all states
        for (Cloud cloud : clouds) {
            cloud.update(currentWidth);
        }

        // Animated titles and prompts update in all states
        renderer.updateAnimation();

        // Gameplay physics and logic updates only when PLAYING
        if (state == GameState.PLAYING) {
            // Update basket
            basket.update(currentWidth);

            // Update difficulty progression
            difficultyManager.update(delta, scoreManager.getCurrentScore());

            // Procedural spawning
            spawnManager.update(delta, difficultyManager, fallingObjects, currentWidth);

            // Update falling objects
            for (FallingObject obj : fallingObjects) {
                obj.update();
            }

            // Check collisions (raindrops caught, bomb explosion, out of bounds)
            boolean lethalCollision = collisionManager.checkCollisions(
                    basket, fallingObjects, particles,
                    scoreManager, soundManager, currentHeight
            );

            if (lethalCollision) {
                state = GameState.GAME_OVER;
                soundManager.pauseMusic();
                shakeIntensity = 18.0; // Trigger dramatic screen shake
                if (scoreManager.isNewRecordAchieved()) {
                    soundManager.playNewRecord();
                } else {
                    soundManager.playGameOver();
                }
            }
        }

        // Particles update during PLAYING and GAME_OVER
        if (state == GameState.PLAYING || state == GameState.GAME_OVER) {
            Iterator<Particle> pIter = particles.iterator();
            while (pIter.hasNext()) {
                Particle p = pIter.next();
                p.update();
                if (!p.isAlive()) {
                    pIter.remove();
                }
            }
        }

        // Screen shake decay
        if (shakeIntensity > 0.1) {
            shakeIntensity *= 0.88;
        } else {
            shakeIntensity = 0;
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        double shakeX = 0;
        double shakeY = 0;
        if (shakeIntensity > 0.1) {
            shakeX = (rand.nextDouble() * 2.0 - 1.0) * shakeIntensity;
            shakeY = (rand.nextDouble() * 2.0 - 1.0) * shakeIntensity;
        }

        int w = getWidth() > 0 ? getWidth() : GameConfig.WINDOW_WIDTH;
        int h = getHeight() > 0 ? getHeight() : GameConfig.WINDOW_HEIGHT;

        renderer.render((Graphics2D) g, w, h, state, basket,
                fallingObjects, particles, clouds,
                scoreManager, difficultyManager, soundManager,
                shakeX, shakeY);

        java.awt.Toolkit.getDefaultToolkit().sync();
    }

    /**
     * Cleanly stops the game loop timer when closing the application.
     */
    public void stopGameTimer() {
        if (gameLoopTimer != null && gameLoopTimer.isRunning()) {
            gameLoopTimer.stop();
        }
        if (soundManager != null) {
            soundManager.stopMusic();
        }
    }

    // Getters for testing and inspection
    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public Basket getBasket() {
        return basket;
    }

    public List<FallingObject> getFallingObjects() {
        return fallingObjects;
    }

    public ScoreManager getScoreManager() {
        return scoreManager;
    }

    public DifficultyManager getDifficultyManager() {
        return difficultyManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }
}
