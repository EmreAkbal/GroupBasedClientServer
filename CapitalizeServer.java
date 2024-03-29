package groupbasedchat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

public class CapitalizeServer {
    private static final int PORT = 59898;
    private static AtomicInteger uniqueId = new AtomicInteger();
    private static Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private static volatile int coordinatorId = -1;
    private static Map<Integer, String> usernames = new ConcurrentHashMap<>();
    private static CapitalizeServer instance; // Singleton in    
   
    
    // Private constructor to prevent instantiation design pattern 
    private CapitalizeServer() {}
    
    public static synchronized CapitalizeServer getInstance() {
        if (instance == null) {
            instance = new CapitalizeServer();
        }
        return instance;
    }
    // desigm pattern ^
    public void startServer() throws IOException {
        System.out.println("The group server is running...");
        ExecutorService pool = Executors.newFixedThreadPool(20);

        try (ServerSocket listener = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = listener.accept();
                int clientId = uniqueId.incrementAndGet();
                ClientHandler clientHandler = new ClientHandler(socket, clientId);
                clients.put(clientId, clientHandler);
                pool.execute(clientHandler);

                if (coordinatorId == -1) {
                    coordinatorId = clientId;
                    clientHandler.setCoordinator(true);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("The group server is running...");
        ExecutorService pool = Executors.newFixedThreadPool(20);

        try (ServerSocket listener = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = listener.accept();
                int clientId = uniqueId.incrementAndGet();
                ClientHandler clientHandler = new ClientHandler(socket, clientId);
                clients.put(clientId, clientHandler);
                pool.execute(clientHandler);

                if (coordinatorId == -1) {
                    coordinatorId = clientId;
                    clientHandler.setCoordinator(true);
                }
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private int clientId;
        private PrintWriter out;
        private Scanner in;
        private boolean isCoordinator = false;

        ClientHandler(Socket socket, int clientId) {
            this.socket = socket;
            this.clientId = clientId;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new Scanner(socket.getInputStream());
            } catch (IOException e) {
                System.out.println("Error setting up streams: " + e.getMessage());
            }
        }

        public void setCoordinator(boolean isCoordinator) {
            this.isCoordinator = isCoordinator;
            sendMessage("You are now the coordinator");
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        @Override
        public void run() {
            System.out.println("Connected: " + socket + " with ID: " + clientId);
            try {
                while (in.hasNextLine()) {
                    String input = in.nextLine();
                    processInput(input);
                }
            } catch (Exception e) {
                System.out.println("Error handling client #" + clientId + ": " + e);
            } finally {
                cleanupConnection();
            }
        }

        private void processInput(String input) {
            if ("REQUEST_MEMBERS".equalsIgnoreCase(input)) {
                sendActiveMembers();
            } else if (input.toUpperCase().startsWith("JOIN ")) {
                String username = input.substring(5).trim();
                usernames.put(clientId, username);
                sendMessage("Welcome, " + username);
            } else if (input.toUpperCase().startsWith("MSG ")) {
                broadcastMessage(input.substring(4).trim(), clientId);
            } else if (input.toUpperCase().startsWith("PRIVATE_MSG ")) {
                sendPrivateMessage(input.substring(12).trim());
            } else if ("LEAVE".equalsIgnoreCase(input)) {
                in.close();
                out.close();
                clients.remove(clientId);
                usernames.remove(clientId);
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error closing socket for client #" + clientId);
                }
                System.out.println("Client #" + clientId + " left.");
            } else if (input.toUpperCase().startsWith("SET_COORDINATOR ")) {
                setNewCoordinator(input.substring(15).trim());
            }
        }

        private void sendActiveMembers() {
            StringBuilder membersList = new StringBuilder();
            for (Map.Entry<Integer, String> entry : usernames.entrySet()) {
                if (membersList.length() > 0) {
                    membersList.append(", ");
                }
                membersList.append(entry.getValue()).append(" (ID: ").append(entry.getKey()).append(")");
            }
            if (membersList.length() == 0) {
                membersList.append("No other members connected.");
            }
            sendMessage("Active Members: " + membersList);
        }
        private void broadcastMessage(String message, int senderId) {
            String senderName = usernames.getOrDefault(senderId, "Unknown");
            String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String fullMessage = timeStamp + " " + senderName + ": " + message;
            for (Map.Entry<Integer, ClientHandler> entry : clients.entrySet()) {
                // Including the sender in the message broadcast
                entry.getValue().sendMessage(fullMessage);
    }
}

        private void sendPrivateMessage(String messageDetails) {
            int firstSpaceIndex = messageDetails.indexOf(' ');
            if (firstSpaceIndex == -1) {
                sendMessage("Invalid private message format.");
                return;
            }
            int recipientId;
            try {
                recipientId = Integer.parseInt(messageDetails.substring(0, firstSpaceIndex));
            } catch (NumberFormatException e) {
                sendMessage("Invalid recipient ID.");
                return;
            }
            String message = messageDetails.substring(firstSpaceIndex + 1);
            ClientHandler recipient = clients.get(recipientId);
            if (recipient == null) {
                sendMessage("Recipient not found.");
            } else {
                recipient.sendMessage("Private from " + usernames.getOrDefault(clientId, "Unknown") + ": " + message);
            }
        }

        private void setNewCoordinator(String idString) {
            int newCoordinatorId;
            try {
                newCoordinatorId = Integer.parseInt(idString.trim());
            } catch (NumberFormatException e) {
                sendMessage("Invalid coordinator ID format.");
                return;
            }
            if (!clients.containsKey(newCoordinatorId)) {
                sendMessage("Client ID does not exist.");
                return;
            }
            if (coordinatorId == newCoordinatorId) {
                sendMessage("This client is already the coordinator.");
                return;
            }
            ClientHandler newCoordinator = clients.get(newCoordinatorId);
            if (newCoordinator != null) {
                clients.get(coordinatorId).setCoordinator(false); // Demote current coordinator
                newCoordinator.setCoordinator(true);
                coordinatorId = newCoordinatorId;
                broadcastMessage("New coordinator is " + usernames.getOrDefault(newCoordinatorId, "Unknown"), -1);
            } else {
                sendMessage("Failed to set new coordinator.");
            }
        }

        private void cleanupConnection() {
            clients.remove(clientId);
            usernames.remove(clientId);
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing socket for client #" + clientId + ": " + e.getMessage());
            }
            System.out.println("Connection with client #" + clientId + " closed.");
        }
    }
}