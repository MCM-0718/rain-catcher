package com.raincatcher.audio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Background music player using pure Java JLayer library.
 * Streams and loops MP3 audio on a background daemon thread only when gameplay is active.
 */
public class MusicPlayer implements Runnable {

    private final String audioFilePath;
    private final String resourcePath;
    private Thread musicThread;
    private volatile boolean running = true;
    private volatile boolean active = false; // Only true when game has started
    private volatile boolean muted = false;
    private javazoom.jl.player.Player activePlayer;

    public MusicPlayer(String audioFilePath, String resourcePath) {
        this.audioFilePath = audioFilePath;
        this.resourcePath = resourcePath;
    }

    /**
     * Starts or resumes music playback when the game starts.
     */
    public synchronized void play() {
        this.active = true;
        if (musicThread == null || !musicThread.isAlive()) {
            this.running = true;
            musicThread = new Thread(this, "RainCatcher-BGM-Thread");
            musicThread.setDaemon(true);
            musicThread.start();
        }
    }

    /**
     * Pauses music playback (e.g. in menu, paused state, or game over).
     */
    public synchronized void pause() {
        this.active = false;
        closePlayer();
    }

    /**
     * Completely stops music playback and terminates the background thread.
     */
    public synchronized void stop() {
        this.active = false;
        this.running = false;
        closePlayer();
        if (musicThread != null) {
            musicThread.interrupt();
            musicThread = null;
        }
    }

    /**
     * Sets mute state. If muted while active, closes active stream immediately.
     */
    public synchronized void setMuted(boolean muted) {
        this.muted = muted;
        if (muted) {
            closePlayer();
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public boolean isActive() {
        return active;
    }

    private synchronized void closePlayer() {
        if (activePlayer != null) {
            try {
                activePlayer.close();
            } catch (Throwable ignored) {
            }
            activePlayer = null;
        }
    }

    @Override
    public void run() {
        while (running) {
            // Only stream audio when game is actively running and not muted
            if (!active || muted) {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    break;
                }
                continue;
            }

            InputStream audioStream = openAudioStream();
            if (audioStream == null) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                }
                continue;
            }

            try {
                javazoom.jl.player.Player player = new javazoom.jl.player.Player(audioStream);
                synchronized (this) {
                    if (!running || !active || muted) {
                        player.close();
                        continue;
                    }
                    this.activePlayer = player;
                }

                // play() blocks until track finishes or close() is called
                player.play();
            } catch (Throwable t) {
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    break;
                }
            } finally {
                closePlayer();
                try {
                    audioStream.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private InputStream openAudioStream() {
        // 1. Try local filesystem path first (e.g. audio/over the moon.mp3)
        try {
            File file = new File(audioFilePath);
            if (file.exists() && file.isFile()) {
                return new BufferedInputStream(new FileInputStream(file));
            }
        } catch (Throwable ignored) {
        }

        // 2. Try embedded resource stream from JAR classpath
        try {
            if (resourcePath != null) {
                InputStream res = getClass().getResourceAsStream(resourcePath);
                if (res != null) {
                    return new BufferedInputStream(res);
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }
}
