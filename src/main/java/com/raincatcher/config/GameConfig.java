package com.raincatcher.config;

/** this part is for: INTINDIHIN NYO PER SECTIONS
 * Centralized game configuration values for Rain Catcher.
 * Adjust falling speed, spawn intervals, bomb probabilities,
 * display settings, and gameplay parameters here.
 */
public final class GameConfig {

    private GameConfig() {
        // Prevent instantiation
    }

    // -------------------------------------------------------------
    // Display & Frame Rate
    // -------------------------------------------------------------
    public static final String GAME_TITLE = "Rain Catcher";
    public static final int WINDOW_WIDTH = 800;
    public static final int WINDOW_HEIGHT = 650;
    public static final int TARGET_FPS = 60;
    public static final int TIMER_DELAY_MS = 1000 / TARGET_FPS; // ~16 ms

    // -------------------------------------------------------------
    // Ground & Boundaries
    // -------------------------------------------------------------
    public static final int GROUND_HEIGHT = 50;
    public static final int PLAY_AREA_BOTTOM = WINDOW_HEIGHT - GROUND_HEIGHT;

    // -------------------------------------------------------------
    // Player / Basket
    // -------------------------------------------------------------
    public static final int BASKET_WIDTH = 96;
    public static final int BASKET_HEIGHT = 44;
    public static final double BASKET_SPEED = 9.0;
    public static final int BASKET_START_Y = PLAY_AREA_BOTTOM - BASKET_HEIGHT - 6;

    // -------------------------------------------------------------
    // Top Storm Clouds & Spawning
    // -------------------------------------------------------------
    public static final int TOP_CLOUD_HEIGHT = 75;
    public static final int SPAWN_START_Y = 48;

    // -------------------------------------------------------------
    // Falling Objects Dimensions
    // -------------------------------------------------------------
    public static final int RAINDROP_WIDTH = 20;
    public static final int RAINDROP_HEIGHT = 28;
    public static final int BOMB_DIAMETER = 32;

    // -------------------------------------------------------------
    // Progressive Difficulty Settings
    // -------------------------------------------------------------
    /** Initial falling speed in pixels per frame (Requirement: 2-3) */
    public static final double INITIAL_FALLING_SPEED = 2.5;

    /** Maximum falling speed in pixels per frame (Requirement: 10-12) */
    public static final double MAX_FALLING_SPEED = 10.5;

    /** Initial spawn interval in milliseconds (Requirement: 900-1200 ms) */
    public static final int INITIAL_SPAWN_INTERVAL_MS = 1100;

    /** Minimum spawn interval in milliseconds (Requirement: 300-400 ms) */
    public static final int MIN_SPAWN_INTERVAL_MS = 350;

    /** Initial bomb spawn probability (Requirement: 10-15%) */
    public static final double INITIAL_BOMB_PROBABILITY = 0.12;

    /** Maximum bomb spawn probability (Requirement: ~25%) */
    public static final double MAX_BOMB_PROBABILITY = 0.25;

    /** Elapsed seconds to reach maximum difficulty */
    public static final double SECONDS_TO_MAX_DIFFICULTY = 100.0;

    /** Points to reach maximum difficulty boost */
    public static final int SCORE_FOR_MAX_DIFFICULTY = 80;

    // -------------------------------------------------------------
    // Scoring & Fairness
    // -------------------------------------------------------------
    public static final int RAINDROP_POINTS = 1;

    /** Minimum horizontal distance (in pixels) between consecutive bombs */
    public static final int MIN_BOMB_HORIZONTAL_SEPARATION = 160;

    /** Maximum consecutive bombs before forcing at least one raindrop */
    public static final int MAX_CONSECUTIVE_BOMBS = 2;

    // -------------------------------------------------------------
    // Visual Polish
    // -------------------------------------------------------------
    public static final int CLOUD_COUNT = 6;
    public static final int MAX_PARTICLES = 300;
}
