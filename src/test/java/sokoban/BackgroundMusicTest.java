package sokoban;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

/** Check the parts of the music code that do not need speakers: the file and the volume. */
class BackgroundMusicTest {
    @Test
    void theSongIsPackagedWithTheGame() throws IOException {
        try (InputStream song = BackgroundMusic.class.getResourceAsStream(
                BackgroundMusic.RESOURCE)) {
            assertNotNull(song, "Expected " + BackgroundMusic.RESOURCE + " on the classpath.");
            assertTrue(song.read() >= 0, "The song file should not be empty.");
        }
    }

    @Test
    void volumeIsASensibleBackgroundLevel() {
        // Not silent and not full strength: the song is mastered loud, so it must be turned down.
        assertTrue(BackgroundMusic.VOLUME > 0.05f);
        assertTrue(BackgroundMusic.VOLUME < 0.5f);
    }

    @Test
    void applyVolumeScalesEverySampleInTheRequestedRange() {
        short[] samples = {1000, -1000, 2000, 3000};

        BackgroundMusic.applyVolume(samples, 1, 2, 0.5f);

        // Only the two samples starting at index 1 change.
        assertArrayEquals(new short[] {1000, -500, 1000, 3000}, samples);
    }

    @Test
    void volumeOfZeroIsSilenceAndVolumeOfOneChangesNothing() {
        short[] silent = {32767, -32768, 123};
        BackgroundMusic.applyVolume(silent, 0, silent.length, 0f);
        assertArrayEquals(new short[] {0, 0, 0}, silent);

        short[] unchanged = {32767, -32768, 123};
        BackgroundMusic.applyVolume(unchanged, 0, unchanged.length, 1f);
        assertArrayEquals(new short[] {32767, -32768, 123}, unchanged);
    }
}
