import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static Map<String, ClientHandler> clients = new HashMap<>();
    private static Map<String, Set<ClientHandler>> rooms = new HashMap<>();

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java ChatServer <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor de chat iniciado na porta " + port + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String userName;
        private String currentRoom;
        private String state = "init";

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String message;
                while ((message = in.readLine()) != null) {
                    processMessage(message);
                }
            } catch (IOException e) {
                disconnect();
            }
        }

        private void processMessage(String message) {
            if (message.startsWith("/nick")) {
                handleNickCommand(message.split(" ", 2)[1]);
            } else if (message.startsWith("/join")) {
                handleJoinCommand(message.split(" ", 2)[1]);
            } else if (message.equals("/leave")) {
                handleLeaveCommand();
            } else if (message.equals("/bye")) {
                handleByeCommand();
            } else if (state.equals("inside")) {
                handleMessage(message);
            } else {
                out.println("ERROR");
            }
        }

        private void handleNickCommand(String newNick) {
            synchronized (clients) {
                if (clients.containsKey(newNick)) {
                    out.println("ERROR");
                } else {
                    if (userName != null) clients.remove(userName);
                    userName = newNick;
                    clients.put(userName, this);
                    out.println("OK");
                    if (state.equals("init")) state = "outside";
                }
            }
        }

        private void handleJoinCommand(String room) {
            if (state.equals("outside") || (state.equals("inside") && !room.equals(currentRoom))) {
                leaveRoom();
                currentRoom = room;
                state = "inside";
                rooms.computeIfAbsent(room, k -> new HashSet<>()).add(this);
                out.println("OK");
                broadcastToRoom("JOINED " + userName, room);
            }
        }

        private void handleLeaveCommand() {
            if (state.equals("inside")) {
                out.println("OK");
                leaveRoom();
                state = "outside";
            }
        }

        private void handleByeCommand() {
            out.println("BYE");
            disconnect();
        }

        private void handleMessage(String message) {
            String escapedMessage = message.replace("/", "//");
            out.println("MESSAGE " + userName + " " + escapedMessage);
            broadcastToRoom("MESSAGE " + userName + " " + escapedMessage, currentRoom);
        }

        private void leaveRoom() {
            if (currentRoom != null) {
                Set<ClientHandler> roomClients = rooms.get(currentRoom);
                if (roomClients != null) {
                    roomClients.remove(this);
                    broadcastToRoom("LEFT " + userName, currentRoom);
                    if (roomClients.isEmpty()) {
                        rooms.remove(currentRoom);
                    }
                }
            }
        }

        private void broadcastToRoom(String message, String room) {
            Set<ClientHandler> roomClients = rooms.get(room);
            if (roomClients != null) {
                for (ClientHandler client : roomClients) {
                    if (client != this) {
                        client.out.println(message);
                    }
                }
            }
        }

        private void disconnect() {
            leaveRoom();
            if (userName != null) clients.remove(userName);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
