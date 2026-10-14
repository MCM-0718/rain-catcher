package com.raincatcher.logic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.prefs.Preferences;

/**
 * Manages player score, high score tracking, and persistent storage
 * using standard Java Preferences with an automatic file I/O fallback.
 */
public class ScoreManager {

    private static final String PREF_KEY_HIGH_SCORE = "raincatcher_highscore";
    private static final String FALLBACK_FILE_NAME = ".raincatcher_highscore.dat";

    private int currentScore = 0;
    private int highScore = 0;
    private boolean newRecordAchieved = false;
    private final Preferences preferences;
    private final File fallbackFile;

    public ScoreManager() {
        Preferences prefs = null;
        try {
            prefs = Preferences.userNodeForPackage(ScoreManager.class);
        } catch (Throwable t) {
            prefs = null;
        }
        this.preferences = prefs;

        // Fallback file in user's home directory
        String userHome = System.getProperty("user.home", ".");
        this.fallbackFile = new File(userHome, FALLBACK_FILE_NAME);

        loadHighScore();
    }

    /**
     * Loads the persisted high score from Preferences or fallback file.
     */
    public final void loadHighScore() {
        int loaded = 0;
        boolean loadedFromPrefs = false;

        if (preferences != null) {
            try {
                loaded = preferences.getInt(PREF_KEY_HIGH_SCORE, 0);
                loadedFromPrefs = true;
            } catch (Throwable ignored) {
                // Ignore and try fallback
            }
        }

        if (!loadedFromPrefs || loaded == 0) {
            loaded = Math.max(loaded, readScoreFromFile());
        }

        this.highScore = Math.max(0, loaded);
    }

    /**
     * Saves the current high score to persistent storage.
     */
    public void saveHighScore() {
        if (preferences != null) {
            try {
                preferences.putInt(PREF_KEY_HIGH_SCORE, highScore);
                preferences.flush();
            } catch (Throwable ignored) {
                // Fallback will save it
            }
        }
        writeScoreToFile(highScore);
    }

    /**
     * Adds points to the current score and checks for a new high score.
     */
    public void addPoints(int points) {
        currentScore += points;
        if (currentScore > highScore) {
            highScore = currentScore;
            newRecordAchieved = true;
            saveHighScore();
        }
    }

    /**
     * Resets current score for a new game session.
     */
    public void reset() {
        currentScore = 0;
        newRecordAchieved = false;
    }

    private int readScoreFromFile() {
        if (!fallbackFile.exists()) {
            return 0;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(fallbackFile))) {
            String line = reader.readLine();
            if (line != null) {
                return Integer.parseInt(line.trim());
            }
        } catch (Throwable ignored) {
            // Safe fallback
        }
        return 0;
    }

    private void writeScoreToFile(int score) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fallbackFile))) {
            writer.println(score);
        } catch (Throwable ignored) {
            // Silently ignore if filesystem write is blocked
        }
    }

    // Getters
    public int getCurrentScore() {
        return currentScore;
    }

    public int getHighScore() {
        return highScore;
    }

    public boolean isNewRecordAchieved() {
        return newRecordAchieved;
    }

    // For testing
    public void setHighScoreDirect(int score) {
        this.highScore = score;
        saveHighScore();
    }
}

