package groupbasedchat;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class CapitalizeClient {
    private static final int DEFAULT_PORT = 59898;
    private ConnectionFactory connectionFactory;
    private MessageHandlerFactory messageHandlerFactory;

    // DESIGN PATTTERN 
    public CapitalizeClient() {
        this.connectionFactory = new SocketConnectionFactory();
        this.messageHandlerFactory = new StandardMessageHandlerFactory();
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java CapitalizeClient <ServerIPAddress>");
            return;
        }

        new CapitalizeClient().startClient(args[0]);
    }

    private void startClient(String serverIP) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your username: ");
        String username = scanner.nextLine().trim();

        try {
        	// DESIGN PATTTERN 
            Connection connection = connectionFactory.createConnection(serverIP, DEFAULT_PORT);
            System.out.println("Connected to the group server. Enter commands or messages. "
            		+ "\n For private message = PRIVATE_MSG \n For set coordinator = SET_COORDINATOR "
            		+ "\n For leaving = LEAVE \n For see wjo is onnline = REQUEST_MEMBERS \n For send a message = MSG (or directly wrie message)");

            MessageHandler messageHandler = messageHandlerFactory.createHandler(connection.getSocket(), username);

            messageHandler.startListening();
            messageHandler.handleOutgoingMessages(scanner);

        } catch (IOException e) {
            System.err.println("Cannot connect to the server at " + serverIP + ":" + DEFAULT_PORT);
            e.printStackTrace();
        } finally {
            scanner.close();
            System.out.println("Disconnected from the server.");
        }
    }

    // Connection Factory and its implementation
    private interface ConnectionFactory {
        Connection createConnection(String serverIP, int port) throws IOException;
    }

    private class SocketConnectionFactory implements ConnectionFactory {
        public Connection createConnection(String serverIP, int port) throws IOException {
            return new Connection(new Socket(serverIP, port));
        }
    }

    private static class Connection {
        private final Socket socket;

        public Connection(Socket socket) {
            this.socket = socket;
        }

        public Socket getSocket() {
            return socket;
        }
    }

    // Message Handler Factory and its implementation
    private interface MessageHandlerFactory {
        MessageHandler createHandler(Socket socket, String username) throws IOException;
    }

    private class StandardMessageHandlerFactory implements MessageHandlerFactory {
        public MessageHandler createHandler(Socket socket, String username) throws IOException {
            return new MessageHandler(socket, username);
        }
    }

    private static class MessageHandler {
        private final PrintWriter out;
        private final Scanner in;
        private final String username;

        public MessageHandler(Socket socket, String username) throws IOException {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new Scanner(socket.getInputStream());
            this.username = username;

            // Automatically send the JOIN command upon successful connection
            out.println("JOIN " + username);
        }

        public void startListening() {
            new Thread(() -> {
                while (in.hasNextLine()) {
                    String message = in.nextLine();
                    if (message.startsWith("Private from")) {
                        System.out.println("[Private] " + message);
                    } else {
                        System.out.println(message);
                    }
                }
            }).start();
        }

        public void handleOutgoingMessages(Scanner scanner) {
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine().trim();
                processInput(input);
                if (input.equalsIgnoreCase("LEAVE")) {
                    break; // Exit after sending LEAVE command
                }
            }
        }

        private void processInput(String input) {
            if (input.equalsIgnoreCase("REQUEST_MEMBERS") || 
                input.toUpperCase().startsWith("MSG ") || 
                input.toUpperCase().startsWith("PRIVATE_MSG ") ||
                input.equalsIgnoreCase("LEAVE") ||
                input.toUpperCase().startsWith("INFO") ||
                input.toUpperCase().startsWith("SET_COORDINATOR ")) {
                out.println(input); // Directly forward valid commands
            } else {
                // For any other input, consider it a broadcast message
                out.println("MSG " + input);
            }
        }
    }
}
