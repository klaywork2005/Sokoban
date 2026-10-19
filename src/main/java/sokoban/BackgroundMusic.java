package sokoban;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.JavaSoundAudioDevice;
import javazoom.jl.player.Player;

/**
 * Plays the game's background music on a loop.
 *
 * <p>Java's built-in sound classes cannot read MP3 files, so this class uses the small
 * JLayer library to decode the song. The song plays on its own background thread, so the
 * window never waits for it. If the sound cannot be played (no speakers, missing file),
 * the game carries on silently.
 */
final class BackgroundMusic {
    // Classpath location of the song (the file lives in src/main/resources).
    static final String RESOURCE = "/Soboku_Music.mp3";

    // How loud the song plays: 1.0 is the file's own level and 0.0 is silence. The song is
    // mastered very loud (about -9 dB average, peaking at the maximum), so it would drown
    // out everything at full level. 0.25 is 12 dB quieter: clearly audible, never tiring.
    static final float VOLUME = 0.25f;

    // volatile: the window thread writes these and the music thread reads them.
    private volatile boolean stopRequested;
    private volatile Player currentPlayer;
    // While muted the song keeps playing silently, so un-muting carries on from the same place.
    private volatile boolean muted;

    /** Start looping the song on a background thread. Call this once. */
    void start() {
        Thread thread = new Thread(this::playForever, "background-music");
        // A daemon thread never keeps the program alive, so closing the window ends the music.
        thread.setDaemon(true);
        thread.start();
    }

    /** Stop the music for good. */
    void stop() {
        stopRequested = true;
        Player player = currentPlayer;
        if (player != null) {
            player.close(); // Makes the play() call in playOnce() return right away.
        }
    }

    /** Silence the music (true) or let it be heard again (false). */
    void setMuted(boolean muted) {
        this.muted = muted;
    }

    /** True while the music is silenced. */
    boolean isMuted() {
        return muted;
    }

    /** Play the song again and again until stop() is called or the sound fails. */
    private void playForever() {
        try {
            while (!stopRequested) {
                playOnce();
            }
        } catch (IOException | JavaLayerException | RuntimeException problem) {
            // Music is a nice extra: report the problem, but never crash the game over it.
            System.err.println("Background music stopped: " + problem.getMessage());
        }
    }

    /** Play the song once from start to finish. Returns when it ends or stop() is called. */
    private void playOnce() throws IOException, JavaLayerException {
        InputStream file = BackgroundMusic.class.getResourceAsStream(RESOURCE);
        if (file == null) {
            throw new IOException("music file not found: " + RESOURCE);
        }
        // try-with-resources closes the file for us when the song ends.
        try (InputStream song = new BufferedInputStream(file)) {
            Player player = new Player(song, new QuietAudioDevice());
            currentPlayer = player;
            player.play(); // Blocks until the song is over.
        }
    }

    /**
     * Multiply every sample by {@code volume}. A sample is one tiny measurement of the
     * sound wave, so smaller samples mean a quieter sound.
     */
    static void applyVolume(short[] samples, int offset, int length, float volume) {
        for (int index = offset; index < offset + length; index++) {
            samples[index] = (short) Math.round(samples[index] * volume);
        }
    }

    /**
     * A speaker connection that lowers the volume. JLayer's own device has no volume
     * control, so we shrink the samples just before they are sent to the speakers.
     * It is an inner class (no {@code static}), so it can see whether the music is muted.
     */
    private final class QuietAudioDevice extends JavaSoundAudioDevice {
        @Override
        protected void writeImpl(short[] samples, int offset, int length)
                throws JavaLayerException {
            applyVolume(samples, offset, length, muted ? 0f : VOLUME);
            super.writeImpl(samples, offset, length);
        }
    }
}
