package com.raincatcher;

import com.raincatcher.audio.SoundManager;
import com.raincatcher.config.GameConfig;
import com.raincatcher.logic.CollisionManager;
import com.raincatcher.logic.DifficultyManager;
import com.raincatcher.logic.ScoreManager;
import com.raincatcher.logic.SpawnManager;
import com.raincatcher.model.Basket;
import com.raincatcher.model.Bomb;
import com.raincatcher.model.FallingObject;
import com.raincatcher.model.GameState;
import com.raincatcher.model.Particle;
import com.raincatcher.model.Raindrop;
import com.raincatcher.ui.GamePanel;

import java.util.ArrayList;
import java.util.List;

/**
 * Self-contained Java test runner verifying game mechanics, scoring,
 * collisions, difficulty scaling, fairness rules, and persistence.
 */
public class GameLogicTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   RAIN CATCHER — JAVA AUTOMATED TEST SUITE      ");
        System.out.println("=================================================");

        runTest("Basket Movement and Boundary Clamping", GameLogicTest::testBasketMovementAndClamping);
        runTest("Raindrop Catch and Single Scoring", GameLogicTest::testRaindropCatchAndScoring);
        runTest("Bomb Collision Triggers Game Over", GameLogicTest::testBombCollisionGameOver);
        runTest("Missed Objects Disappear Past Screen", GameLogicTest::testMissedObjectsRemoval);
        runTest("Progressive Difficulty Scaling", GameLogicTest::testDifficultyProgression);
        runTest("Spawn Fairness (No Unfair Bomb Patterns)", GameLogicTest::testSpawnFairnessRules);
        runTest("Score and High Score Persistence", GameLogicTest::testScoreAndHighScorePersistence);
        runTest("State Transitions & Timer Sanity", GameLogicTest::testStateTransitionsAndTimer);
        runTest("Procedural Sound Synthesis Stability", GameLogicTest::testSoundSynthesisStability);

        System.out.println("=================================================");
        System.out.println("TEST RESULTS: " + testsPassed + " Passed, " + testsFailed + " Failed.");
        System.out.println("=================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void runTest(String testName, Runnable test) {
        try {
            test.run();
            System.out.println(" [PASS] " + testName);
            testsPassed++;
        } catch (Throwable t) {
            System.err.println(" [FAIL] " + testName + ": " + t.getMessage());
            t.printStackTrace(System.err);
            testsFailed++;
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError("Assertion failed: " + message + " (Expected: " + expected + ", Actual: " + actual + ")");
    }

    private static void testBasketMovementAndClamping() {
        Basket basket = new Basket(100, 500, 96, 44, 10.0);

        // Move Left
        basket.setMovingLeft(true);
        basket.update(800);
        assertEquals(90.0, basket.getX(), "Basket should move left by speed");

        // Move off left screen edge -> Clamped to min 4
        for (int i = 0; i < 20; i++) {
            basket.update(800);
        }
        assertEquals(4.0, basket.getX(), "Basket must clamp to left border");
        basket.setMovingLeft(false);

        // Move off right screen edge -> Clamped to 800 - 96 - 4 = 700
        basket.setMovingRight(true);
        for (int i = 0; i < 100; i++) {
            basket.update(800);
        }
        assertEquals(700.0, basket.getX(), "Basket must clamp to right border");
        basket.setMovingRight(false);

        // Reset
        basket.reset(800);
        assertEquals((800 - 96) / 2.0, basket.getX(), "Basket must center on reset");
    }

    private static void testRaindropCatchAndScoring() {
        CollisionManager collisionManager = new CollisionManager();
        ScoreManager scoreManager = new ScoreManager();
        scoreManager.reset();
        SoundManager soundManager = new SoundManager();
        soundManager.setMuted(true);

        Basket basket = new Basket(300, 500, 96, 44, 9.0);
        List<FallingObject> objects = new ArrayList<>();
        List<Particle> particles = new ArrayList<>();

        // Place raindrop directly inside basket catch zone
        Raindrop drop = new Raindrop(340, 504, 20, 28, 3.0);
        objects.add(drop);

        boolean gameOver = collisionManager.checkCollisions(
                basket, objects, particles, scoreManager, soundManager, 650
        );

        assertTrue(!gameOver, "Raindrop catch should not cause game over");
        assertEquals(1, scoreManager.getCurrentScore(), "Score should increase by 1 point");
        assertEquals(0, objects.size(), "Caught raindrop must be removed from active objects");
        assertTrue(particles.size() > 0, "Water splash particles should be generated");

        // Subsequent collision checks must not award additional points
        boolean secondCheck = collisionManager.checkCollisions(
                basket, objects, particles, scoreManager, soundManager, 650
        );
        assertEquals(1, scoreManager.getCurrentScore(), "Score must not increment again for consumed object");
    }

    private static void testBombCollisionGameOver() {
        CollisionManager collisionManager = new CollisionManager();
        ScoreManager scoreManager = new ScoreManager();
        SoundManager soundManager = new SoundManager();
        soundManager.setMuted(true);

        Basket basket = new Basket(300, 500, 96, 44, 9.0);
        List<FallingObject> objects = new ArrayList<>();
        List<Particle> particles = new ArrayList<>();

        // Place bomb intersecting basket body
        Bomb bomb = new Bomb(330, 510, 32, 3.0);
        objects.add(bomb);

        boolean gameOver = collisionManager.checkCollisions(
                basket, objects, particles, scoreManager, soundManager, 650
        );

        assertTrue(gameOver, "Bomb collision must trigger game over");
        assertEquals(0, objects.size(), "Exploded bomb must be removed from active objects");
        assertTrue(particles.size() >= 30, "Explosion should produce fire and smoke particles");
    }

    private static void testMissedObjectsRemoval() {
        CollisionManager collisionManager = new CollisionManager();
        ScoreManager scoreManager = new ScoreManager();
        SoundManager soundManager = new SoundManager();
        soundManager.setMuted(true);

        Basket basket = new Basket(300, 500, 96, 44, 9.0);
        List<FallingObject> objects = new ArrayList<>();
        List<Particle> particles = new ArrayList<>();

        // Object fallen past bottom of screen (screen height = 650, y = 680)
        Raindrop offscreenDrop = new Raindrop(100, 680, 20, 28, 4.0);
        Bomb offscreenBomb = new Bomb(200, 680, 32, 4.0);
        objects.add(offscreenDrop);
        objects.add(offscreenBomb);

        collisionManager.checkCollisions(
                basket, objects, particles, scoreManager, soundManager, 650
        );

        assertEquals(0, objects.size(), "Off-screen objects must be cleaned up to prevent memory leak");
        assertEquals(0, scoreManager.getCurrentScore(), "Missed drops should not award points");
    }

    private static void testDifficultyProgression() {
        DifficultyManager diff = new DifficultyManager();

        // Level 1 baseline
        assertEquals(1, diff.getLevel(), "Starts at Level 1");
        double initialSpeed = diff.getCurrentFallingSpeed();
        int initialInterval = diff.getCurrentSpawnIntervalMs();
        double initialBombProb = diff.getCurrentBombProbability();

        assertTrue(initialSpeed >= GameConfig.INITIAL_FALLING_SPEED - 0.01, "Falling speed starts at initial value");
        assertTrue(initialInterval <= GameConfig.INITIAL_SPAWN_INTERVAL_MS, "Spawn interval starts at initial value");
        assertTrue(initialBombProb >= GameConfig.INITIAL_BOMB_PROBABILITY - 0.01, "Bomb prob starts at initial value");

        // Advance time by 60 seconds and score to 40
        diff.update(60000, 40);
        assertTrue(diff.getCurrentFallingSpeed() > initialSpeed, "Falling speed should increase with progression");
        assertTrue(diff.getCurrentSpawnIntervalMs() < initialInterval, "Spawn interval should decrease with progression");
        assertTrue(diff.getCurrentBombProbability() > initialBombProb, "Bomb probability should increase with progression");

        // Max progression (120 seconds, 100 score)
        diff.update(120000, 100);
        assertEquals(5, diff.getLevel(), "Should reach max level 5");
        assertTrue(diff.getCurrentFallingSpeed() <= GameConfig.MAX_FALLING_SPEED + 0.01, "Speed capped at max");
        assertTrue(diff.getCurrentSpawnIntervalMs() >= GameConfig.MIN_SPAWN_INTERVAL_MS - 1, "Interval capped at min");
        assertTrue(diff.getCurrentBombProbability() <= GameConfig.MAX_BOMB_PROBABILITY + 0.01, "Bomb prob capped at max");
    }

    private static void testSpawnFairnessRules() {
        DifficultyManager diff = new DifficultyManager();
        // Force high bomb probability to test consecutive bomb clamp
        diff.update(120000, 100);

        SpawnManager spawner = new SpawnManager();
        List<FallingObject> list = new ArrayList<>();

        int consecutiveBombs = 0;
        int maxConsecutiveSeen = 0;

        for (int i = 0; i < 100; i++) {
            FallingObject obj = spawner.spawnNextObject(diff, list, 800);
            if (obj instanceof Bomb) {
                consecutiveBombs++;
                if (consecutiveBombs > maxConsecutiveSeen) {
                    maxConsecutiveSeen = consecutiveBombs;
                }
            } else {
                consecutiveBombs = 0;
            }
        }

        assertTrue(maxConsecutiveSeen <= GameConfig.MAX_CONSECUTIVE_BOMBS,
                "Fairness rule must limit consecutive bombs to " + GameConfig.MAX_CONSECUTIVE_BOMBS
                        + " (Max observed: " + maxConsecutiveSeen + ")");
    }

    private static void testScoreAndHighScorePersistence() {
        ScoreManager sm = new ScoreManager();
        sm.reset();

        int initialHigh = sm.getHighScore();
        sm.addPoints(5);
        assertEquals(5, sm.getCurrentScore(), "Score should be 5");

        // Force a new high score
        int targetScore = initialHigh + 10;
        sm.addPoints(targetScore - 5);
        assertTrue(sm.isNewRecordAchieved(), "New record flag must be set");
        assertEquals(targetScore, sm.getHighScore(), "High score must equal target score");

        // Simulate reload from storage
        ScoreManager smReloaded = new ScoreManager();
        assertTrue(smReloaded.getHighScore() >= targetScore,
                "High score must persist across instances (Found: " + smReloaded.getHighScore() + ")");
    }

    private static void testStateTransitionsAndTimer() {
        GamePanel panel = new GamePanel();
        assertEquals(GameState.MENU, panel.getState(), "Initial state must be MENU");

        panel.startGame();
        assertEquals(GameState.PLAYING, panel.getState(), "State after startGame() must be PLAYING");

        panel.setState(GameState.PAUSED);
        assertEquals(GameState.PAUSED, panel.getState(), "State after pause must be PAUSED");

        // Clean shutdown
        panel.stopGameTimer();
    }

    private static void testSoundSynthesisStability() {
        SoundManager sm = new SoundManager();
        // Trigger all synthesizers to ensure zero crashes or exceptions
        sm.playCatch();
        sm.playExplosion();
        sm.playGameOver();
        sm.playNewRecord();
        sm.playClick();
        sm.toggleMute();
        assertTrue(sm.isMuted(), "SoundManager should be muted after toggle");
        sm.toggleMute();
        assertTrue(!sm.isMuted(), "SoundManager should be unmuted after second toggle");
    }
}

