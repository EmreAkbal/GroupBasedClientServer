package groupbasedchat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertSame;

public class junittest2 {

    @Test
    public void testSingletonInstance() {
        // Call getInstance() twice
        CapitalizeServer firstInstance = CapitalizeServer.getInstance();
        CapitalizeServer secondInstance = CapitalizeServer.getInstance();

        // Check if both instances are the same
        assertSame(firstInstance, secondInstance, "Both calls to getInstance() should return the same instance.");
    }
}
