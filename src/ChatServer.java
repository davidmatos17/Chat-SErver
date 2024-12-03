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
                    processMessage(message.trim());
                }
            } catch (IOException e) {
                System.err.println("Erro ao comunicar com o cliente: " + e.getMessage());
            } finally {
                disconnect();
            }
        }

        private void processMessage(String message) {
            if (message.startsWith("/nick")) {
                if (message.split(" ").length > 1) {
                    handleNickCommand(message.split(" ", 2)[1]);
                } else {
                    out.println("ERROR");
                }
            } else if (message.startsWith("/join")) {
                if (message.split(" ").length > 1) {
                    handleJoinCommand(message.split(" ", 2)[1]);
                } else {
                    out.println("ERROR");
                }
            }else if (message.startsWith("/priv")) {
                    String[] parts = message.split(" ", 3);
                    if (parts.length == 3) {
                        handlePrivateMessage(parts[1], parts[2]);
                    } else {
                        out.println("ERROR");
                    }
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
                if (clients.containsKey(newNick) || newNick.isEmpty() || newNick.contains(" ")) {
                    out.println("ERROR");
                } else {
                    String oldNick = userName;
                    if (oldNick != null) {
                        clients.remove(oldNick);
                    }
                    userName = newNick;
                    clients.put(userName, this);
                    out.println("OK");

                    if (state.equals("init")) {
                        state = "outside";
                    } else if (state.equals("inside")) {
                        broadcastToRoom("NEWNICK " + oldNick + " " + userName, currentRoom);
                    }
                }
            }
        }

        private void handleJoinCommand(String room) {
            if (state.equals("init")) {
                out.println("ERROR");
                return;
            }

            if (state.equals("inside") && room.equals(currentRoom)) {
                out.println("ERROR");
                return;
            }

            leaveRoom();

            synchronized (rooms) {
                currentRoom = room;
                rooms.computeIfAbsent(room, k -> new HashSet<>()).add(this);
                state = "inside";
                out.println("OK");
                broadcastToRoom("JOINED " + userName, room);
            }
        }

        private void handleLeaveCommand() {
            if (state.equals("inside")) {
                out.println("OK");
                leaveRoom();
                state = "outside";
            } else {
                out.println("ERROR");
            }
        }

        private void handleByeCommand() {
            out.println("BYE");
            disconnect();
        }

        private void handleMessage(String var1) {
            if (var1.isEmpty()) {
                this.out.println("ERROR");
            } else {
                String var2 = var1.startsWith("/") ? "//" + var1.substring(1) : var1;
                this.broadcastToRoom("MESSAGE " + this.userName + " " + var2, this.currentRoom);
            }
        }
        

        private void leaveRoom() {
            if (currentRoom != null) {
                Set<ClientHandler> roomClients = rooms.get(currentRoom);
                if (roomClients != null) {
                    roomClients.remove(this);
                    if (!roomClients.isEmpty()) {
                        broadcastToRoom("LEFT " + userName, currentRoom);
                    } else {
                        rooms.remove(currentRoom);
                    }
                }
                currentRoom = null;
            }
        }

        private void broadcastToRoom(String message, String room) {
            Set<ClientHandler> roomClients = rooms.get(room);
            if (roomClients != null) {
                for (ClientHandler client : roomClients) {
                    try {
                        client.out.println(message);
                    } catch (Exception e) {
                        System.err.println("Erro ao enviar mensagem para o cliente: " + client.userName);
                    }
                }
            }
        }
        

        private void disconnect() {
            leaveRoom();
            synchronized (clients) {
                if (userName != null) {
                    clients.remove(userName);
                }
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private void handlePrivateMessage(String recipientName, String message) {
            synchronized (clients) {
                ClientHandler recipient = clients.get(recipientName);
                if (recipient == null) {
                    out.println("ERROR");
                } else {
                    out.println("OK");
                    recipient.out.println("PRIVATE " + userName + " " + message);
                }
            }
        }
        
    }
}
