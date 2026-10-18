package sokoban;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Check XSB parsing from temporary files and real bundled classpath resources. */
class LevelLoaderTest {
    // JUnit creates this isolated directory and cleans it up after each test.
    @TempDir
    Path directory;

    @Test
    void preservesSpacesAndSeparatesMetadataAndFinalBoard() throws IOException {
        // Include metadata, padded rows, and a final board without a trailing newline.
        Path file = directory.resolve("levels.txt");
        Files.writeString(file,
                "Title: example\n  ####  \n  #@*#  \n  ####  \n"
                + "\n; next board\n#####\n#+$.#\n#####");

        List<List<String>> levels = new LevelLoader().loadLevels(file);

        // Compare whole row strings so lost leading or trailing spaces cause a failure.
        assertEquals(List.of(
                List.of("  ####  ", "  #@*#  ", "  ####  "),
                List.of("#####", "#+$.#", "#####")), levels);
    }

    @Test
    void metadataOnlyFileContainsNoBoards() throws IOException {
        // Metadata and separators alone should never produce a phantom empty level.
        Path file = directory.resolve("empty.txt");
        Files.writeString(file, "Title: empty\n\n; comment\n");
        assertTrue(new LevelLoader().loadLevels(file).isEmpty());
    }

    @Test
    void bothBundledCollectionsLoadFromTheClasspath() throws IOException {
        // Exercise packaged resources as well as the external-file path tested above.
        LevelLoader loader = new LevelLoader();
        assertTrue(loader.loadResource("/original-levels.txt").size() > 10);
        assertTrue(loader.loadResource("/microban.txt").size() > 10);
    }

    @Test
    void missingResourceProducesADescriptiveError() {
        // Preserve the requested name in the exception so startup failures are actionable.
        IOException error = assertThrows(IOException.class,
                () -> new LevelLoader().loadResource("/missing-levels.txt"));
        assertTrue(error.getMessage().contains("/missing-levels.txt"));
    }
}
