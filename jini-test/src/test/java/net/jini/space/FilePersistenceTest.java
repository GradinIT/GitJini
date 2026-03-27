package net.jini.space;

import net.jini.core.entry.Entry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FilePersistenceTest {

    public static class TestEntry implements Entry {
        public String name;
        public Integer value;

        public TestEntry() {}
        public TestEntry(String name, Integer value) {
            this.name = name;
            this.value = value;
        }
    }

    @TempDir
    Path tempDir;

    @Test
    public void testPersistence() throws Exception {
        File storeDir = tempDir.resolve("space-store").toFile();
        FilePersistenceStore store = new FilePersistenceStore(storeDir);

        BasicJavaSpace space = new BasicJavaSpace();
        space.setPersistenceStore(store);

        TestEntry e1 = new TestEntry("first", 1);
        space.write(e1, null, 10000);

        TestEntry e2 = new TestEntry("second", 2);
        space.write(e2, null, 10000);

        // Verify they are there
        assertNotNull(space.read(new TestEntry("first", null), null, 0));
        assertNotNull(space.read(new TestEntry("second", null), null, 0));

        // Create a new space instance with the same store
        BasicJavaSpace space2 = new BasicJavaSpace();
        space2.setPersistenceStore(new FilePersistenceStore(storeDir));

        // Verify data is reloaded
        TestEntry found1 = (TestEntry) space2.read(new TestEntry("first", null), null, 0);
        assertNotNull(found1);
        assertEquals(1, found1.value);

        TestEntry found2 = (TestEntry) space2.read(new TestEntry("second", null), null, 0);
        assertNotNull(found2);
        assertEquals(2, found2.value);
    }

    @Test
    public void testRemovalPersistence() throws Exception {
        File storeDir = tempDir.resolve("space-store-removal").toFile();
        FilePersistenceStore store = new FilePersistenceStore(storeDir);

        BasicJavaSpace space = new BasicJavaSpace();
        space.setPersistenceStore(store);

        TestEntry e1 = new TestEntry("to-remove", 1);
        space.write(e1, null, 10000);

        assertNotNull(space.take(new TestEntry("to-remove", null), null, 0));
        assertNull(space.read(new TestEntry("to-remove", null), null, 0));

        // New space instance
        BasicJavaSpace space2 = new BasicJavaSpace();
        space2.setPersistenceStore(new FilePersistenceStore(storeDir));

        // Should be empty
        assertNull(space2.read(new TestEntry("to-remove", null), null, 0));
    }
}
