package se.gtradinit.dev;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

public class DevelopmentEnvironmentInitialiserTest {

    @Test
    public void testJarCreation() throws Exception {
        System.setProperty("skip.docker", "true");
        DevelopmentEnvironmentInitialiser initialiser = new DevelopmentEnvironmentInitialiser();
        
        // Manual resource loading to simulate init()
        byte[] slaBytes = "test sla".getBytes();
        byte[] puBytes = "test pu".getBytes();
        
        File jarFile = new File("test-su-test.jar");
        
        // Use reflection to call private createJar if needed, or just test the result of init()
        // For simplicity, let's just run a modified version of the creation logic here
        // Or I can make createJar protected/package-private for testing
        
        initialiser.init(); // This will create test-su.jar using real resources
        
        File realJar = new File("test-su.jar");
        assertTrue(realJar.exists(), "test-su.jar should exist");
        
        try (JarFile jar = new JarFile(realJar)) {
            JarEntry slaEntry = jar.getJarEntry("META-INF/spring/sla.xml");
            assertNotNull(slaEntry, "sla.xml should be in META-INF/spring/");
            
            JarEntry puEntry = jar.getJarEntry("META-INF/spring/pu.xml");
            assertNotNull(puEntry, "pu.xml should be in META-INF/spring/");
            
            String slaContent = new String(jar.getInputStream(slaEntry).readAllBytes());
            assertTrue(slaContent.contains("partitioned"), "SLA should contain 'partitioned'");
            
            String puContent = new String(jar.getInputStream(puEntry).readAllBytes());
            assertTrue(puContent.contains("testSpace"), "PU should contain 'testSpace'");
        } finally {
            realJar.delete();
        }
    }
}
