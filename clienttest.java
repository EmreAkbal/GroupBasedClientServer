package groupbasedchat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CapitalizeServerTest {

    @Mock
    private Socket socket;

    private ByteArrayOutputStream outputStream;
    private ByteArrayInputStream inputStream;
    private CapitalizeServer.ClientHandler clientHandler;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        // Simulate socket input/output streams
        outputStream = new ByteArrayOutputStream();
        inputStream = new ByteArrayInputStream("".getBytes());

        when(socket.getOutputStream()).thenReturn(outputStream);
        when(socket.getInputStream()).thenReturn(inputStream);

        clientHandler = new CapitalizeServer.ClientHandler(socket, 1); // Using 1 as a sample client ID
    }

    @Test
    void testClientHandlerInitialization() {
        assertNotNull(clientHandler, "ClientHandler should be initialized");
    }

    @Test
    void testSetCoordinator() {
        clientHandler.setCoordinator(true);
        assertTrue(outputStream.toString().contains("You are now the coordinator"), "Coordinator message not sent");
    }

    // Further tests would go here, such as testing processInput, broadcastMessage, etc.

}
