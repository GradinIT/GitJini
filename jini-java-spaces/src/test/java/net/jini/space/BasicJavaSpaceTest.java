package net.jini.space;

import net.jini.core.entry.Entry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BasicJavaSpaceTest {

    public static class TestEntry implements Entry {
        public String name;
        public Integer value;

        public TestEntry() {}
        public TestEntry(String name, Integer value) {
            this.name = name;
            this.value = value;
        }
    }

    @Test
    public void testWriteReadTake() throws Exception {
        JavaSpace space = new BasicJavaSpace();
        TestEntry entry1 = new TestEntry("test1", 1);
        
        space.write(entry1, null, 1000);
        
        // Read with exact match
        TestEntry template = new TestEntry("test1", null);
        TestEntry result = (TestEntry) space.read(template, null, 0);
        assertNotNull(result);
        assertEquals("test1", result.name);
        assertEquals(1, result.value);
        
        // Take entry
        TestEntry taken = (TestEntry) space.take(template, null, 0);
        assertNotNull(taken);
        assertEquals("test1", taken.name);
        
        // Should be gone now
        TestEntry result2 = (TestEntry) space.read(template, null, 0);
        assertNull(result2);
    }
    
    @Test
    public void testMultipleEntries() throws Exception {
        JavaSpace space = new BasicJavaSpace();
        space.write(new TestEntry("a", 1), null, 1000);
        space.write(new TestEntry("b", 2), null, 1000);
        
        TestEntry template = new TestEntry("b", null);
        TestEntry result = (TestEntry) space.read(template, null, 0);
        assertNotNull(result);
        assertEquals("b", result.name);
        assertEquals(2, result.value);
    }

    @Test
    public void testWildcardMatch() throws Exception {
        JavaSpace space = new BasicJavaSpace();
        space.write(new TestEntry("a", 1), null, 1000);
        
        TestEntry result = (TestEntry) space.read(null, null, 0);
        assertNotNull(result);
        assertEquals("a", result.name);
    }
}
