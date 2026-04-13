package lcmc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ToolsTest {

    @Test
    void testSingletonInstance() {
        Tools instance1 = Tools.getInstance();
        Tools instance2 = Tools.getInstance();
        assertNotNull(instance1, "getInstance() should not return null.");
        assertSame(instance1, instance2, "Both instances should be the same for a singleton.");
    }

    @Test
    void testPrivateConstructor() throws NoSuchMethodException {
        // Attempt to get the private constructor
        Constructor<Tools> constructor = Tools.class.getDeclaredConstructor();
        // Assert that the constructor is private
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private.");
        // Make sure it's not accessible to prevent accidental instantiation
        // constructor.setAccessible(true); // Don't call this in a successful test
    }
}
