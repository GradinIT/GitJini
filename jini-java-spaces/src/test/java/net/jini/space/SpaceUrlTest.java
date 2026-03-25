package net.jini.space;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;
import net.jini.space.config.modifiers.WriteModifier;

public class SpaceUrlTest {

    @Test
    public void testEmbeddedUrl() throws Exception {
        Object space = SpaceFinder.find("/./mySpace");
        assertNotNull(space);
        assertTrue(space instanceof BasicJavaSpace);
    }

    @Test
    public void testEmbeddedUrlWithParams() throws Exception {
        Object space = SpaceFinder.find("/./mySpace?schema=default&versioned=true");
        assertNotNull(space);
        assertTrue(space instanceof BasicJavaSpace);
    }

    @Test
    public void testImplicitEmbeddedUrl() throws Exception {
        Object space = SpaceFinder.find("mySpace");
        assertNotNull(space);
        assertTrue(space instanceof BasicJavaSpace);
    }

    @Test
    public void testEmbeddedFactoryBean() throws Exception {
        EmbeddedSpaceFactoryBean factory = new EmbeddedSpaceFactoryBean();
        factory.setName("testSpace");
        factory.setVersioned(true);
        factory.setLookupGroups("testGroup");
        
        Object space = factory.getObject();
        assertNotNull(space);
        assertTrue(space instanceof BasicJavaSpace);
    }

    @Test
    public void testSpaceFactoryBean() throws Exception {
        BasicJavaSpace innerSpace = new BasicJavaSpace();
        SpaceFactoryBean factory = new SpaceFactoryBean();
        factory.setSpace(innerSpace);
        factory.setClustered(true);
        factory.setDefaultReadTimeout(1000);
        
        WriteModifier[] modifiers = new WriteModifier[] { WriteModifier.UPDATE_ONLY, WriteModifier.PARTIAL_UPDATE };
        factory.setDefaultWriteModifiers(modifiers);
        
        Object space = factory.getObject();
        assertNotNull(space);
        assertEquals(innerSpace, space);
    }
}
