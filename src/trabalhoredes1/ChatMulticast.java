package trabalhoredes1;

//IOException
import java.io.*;

// API de rede (MulticastSocket, InetAddress, DatagramPacket)
import java.net.*;

// Datas
import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.Scanner;

import javax.swing.JOptionPane;

// Objetos JSON
import org.json.JSONObject;

// Ler como String
import java.nio.file.Files;
import java.nio.file.Paths;

public class ChatMulticast {

    // Faixa de endereços multicast varia de 224.0.0.0 a 239.255.255.255
    private static final String MULTICAST_ADDRESS = "235.0.0.0";
    private static final int PORT = 8080;

    public static void main(String[] args) {
        boolean conectado = false;

        try {
            // Carrega arquivo JSON de template com configurações iniciais
            JSONObject template = carregarJson("src/trabalhoredes1/template.json");

            if (template == null) {
                JOptionPane.showMessageDialog(null, "template.json não encontrado ou mal formatado!", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Nome do usuário pré-definido no template, ou solicita ao usuário
            String username = template.optString("username");
            String nomeUsuario = JOptionPane.showInputDialog(null, "Digite seu nome:", username);

            if (nomeUsuario == null || nomeUsuario.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Nome inválido. Encerrando.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Cria socket multicast na porta especificada
            MulticastSocket socket = new MulticastSocket(PORT);

            // Resolve o IP do grupo multicast
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);

            // Entra no grupo multicast
            socket.joinGroup(group);

            // Cria mensagem de boas-vindas ao conectar
            String welcomeMessege = "conectou-se ao chat";
            JSONObject jsonWelcome = new JSONObject();
            jsonWelcome.put("username", nomeUsuario);
            jsonWelcome.put("message", welcomeMessege);
            jsonWelcome.put("date", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
            jsonWelcome.put("time", new SimpleDateFormat("HH:mm:ss").format(new Date()));

            // Converte JSON em bytes e envia
            byte[] welcomeBytes = jsonWelcome.toString().getBytes();
            DatagramPacket welcomePacket = new DatagramPacket(welcomeBytes, welcomeBytes.length, group, PORT);
            socket.send(welcomePacket);

            // Thread para receber mensagens
            Thread receiver = new Thread(() -> {
                byte[] buffer = new byte[1024]; // Buffer para mensagens recebidas
                while (true) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);

                        // De bytes para string
                        String msg = new String(packet.getData(), 0, packet.getLength());

                        // Converte string para JSON
                        JSONObject json = new JSONObject(msg);

                        // Extrai dados
                        String date = json.getString("date");
                        String time = json.getString("time");
                        String user = json.getString("username");
                        String message = json.getString("message");

                        // Exibe mensagem formatada
                        System.out.printf("[%s %s] %s: %s%n", date, time, user, message);

                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, "Erro ao receber mensagem: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            // Inicia a thread de recepção
            receiver.start();

            Scanner scanner = new Scanner(System.in);

            // Comandos disponíveis
            System.out.println("COMANDOS \nsair -> Encerrar programa \nhelp -> Lista comandos \n");

            // Loop principal para enviar mensagens
            while (true) {
                System.out.print("Digite sua mensagem: ");
                String message = scanner.nextLine();

                // Caso digite "sair", desconecta e envia mensagem de saída
                if (message.equalsIgnoreCase("sair")) {
                    String exitMessege = "desconectou-se do chat";
                    JSONObject jsonExit = new JSONObject();
                    jsonExit.put("username", nomeUsuario);
                    jsonExit.put("message", exitMessege);
                    jsonExit.put("date", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
                    jsonExit.put("time", new SimpleDateFormat("HH:mm:ss").format(new Date()));

                    byte[] exitBytes = jsonExit.toString().getBytes();
                    DatagramPacket exitPacket = new DatagramPacket(exitBytes, exitBytes.length, group, PORT);
                    socket.send(exitPacket);

                    System.out.println("Encerrando");
                    socket.leaveGroup(group);
                    socket.close();
                    System.exit(0);
                }

                // Mostra comandos disponíveis
                if (message.equalsIgnoreCase("help") || message.equalsIgnoreCase("ajuda")) {
                    System.out.println("    COMANDOS \nsair -> Encerrar programa \nhelp -> Lista comandos \n");
                    continue;
                }

                // Cria mensagem com data e hora atual
                String date = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
                String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                
                JSONObject json = new JSONObject();
                json.put("username", nomeUsuario);
                json.put("message", message);
                json.put("date", date);
                json.put("time", time);

                byte[] msgBytes = json.toString().getBytes();
                DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, group, PORT);
                socket.send(packet);
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Erro geral: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Método auxiliar para carregar arquivo JSON de configuração
    public static JSONObject carregarJson(String caminho) {
        try {
            // Lê conteúdo do arquivo como string
            String conteudo = new String(Files.readAllBytes(Paths.get(caminho)));
            return new JSONObject(conteudo); // Converte para JSONObject
        } catch (Exception e) {
            System.err.println("Erro ao carregar o JSON: " + e.getMessage());
            return null;
        }
    }
}
