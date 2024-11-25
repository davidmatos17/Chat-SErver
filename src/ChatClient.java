import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class ChatClient {

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas coma interface gráfica

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message + "\n");
    }


    // Construtor
    public ChatClient(String server, int port) throws IOException {
        // Inicialização da interface gráfica --- * NÃO MODIFICAR *
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        chatBox.setBackground(Color.WHITE);
        chatBox.setForeground(Color.BLACK);
        chatBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
        chatBox.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

        // Estilo da área de mensagens (chatArea)
        chatArea.setBackground(new Color(230, 240, 255)); // Azul claro
        chatArea.setForeground(Color.DARK_GRAY);
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        chatArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        chatArea.setEditable(false);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                    chatBox.setText("");
                }
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                chatBox.requestFocusInWindow();
            }
        });
        // --- Fim da inicialização da interface gráfica

        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui


        socket = new Socket(server, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }


    // metodo invocado sempre que o utilizador envia uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
        if (message.startsWith("/")) {
            // Enviar comandos diretamente
            out.println(message);
        } else {
            // Adiciona escape para mensagens normais que começam com "/"
            out.println(message.replaceFirst("^/", "//"));
        }


    }


    // metodo principal
    public void run() {
        // Thread para ouvir as mensagens do servidor
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String response;
                    // Recebe mensagens do servidor e exibe na interface gráfica
                    while ((response = in.readLine()) != null) {
                        processServerMessage(response);
                    }
                } catch (IOException e) {
                    printMessage("Conexão com o servidor foi perdida.");
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void processServerMessage(String message) {
        // Formata as mensagens para uma visualização amigável no chat
        if (message.startsWith("MESSAGE")) {
            String[] parts = message.split(" ", 3);
            printMessage(parts[1] + ": " + parts[2]);
        } else if (message.startsWith("NEWNICK")) {
            String[] parts = message.split(" ");
            printMessage(parts[1] + " mudou de nome para " + parts[2]);
        } else if (message.startsWith("JOINED")) {
            String[] parts = message.split(" ");
            printMessage(parts[1] + " entrou na sala.");
        } else if (message.startsWith("LEFT")) {
            String[] parts = message.split(" ");
            printMessage(parts[1] + " saiu da sala.");
        } else if (message.startsWith("ERROR")) {
            printMessage("Comando inválido ou operação não permitida.");
        } else if (message.equals("BYE")) {
            printMessage("Desconectado do servidor.");
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (message.equals("OK")) {
            // Comando foi bem-sucedido, mas sem mensagem específica para mostrar
        } else {
            // Mensagem desconhecida
            printMessage("Mensagem desconhecida do servidor: " + message);
        }

    }


    //Instancia o ChatClient e arranca-o invoca o metodo run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Uso: java ChatClient <endereço_servidor> <porta>");
            return;
        }
        try {
            ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
            client.run();
        } catch (IOException e) {
            System.out.println("Não foi possível conectar ao servidor: " + e.getMessage());
        }
    }

}

