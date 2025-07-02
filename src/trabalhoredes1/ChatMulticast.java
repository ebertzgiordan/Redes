package trabalhoredes1;

// Importa classes de entrada e saída de dados, como IOException
import java.io.*;

// Importa classes de rede (MulticastSocket, InetAddress, DatagramPacket)
import java.net.*;

// Para formatar datas no formato "dd/MM/yyyy"
import java.text.SimpleDateFormat;

// Para obter a data e hora atual
import java.util.Date;

// Utilizado para ler entradas no console (Scanner é prático para isso)
import java.util.Scanner;

// Permite criar caixas de diálogo gráficas para entrada de dados ou mensagens
import javax.swing.JOptionPane;

// Biblioteca para trabalhar com objetos JSON (é necessário adicionar ao projeto)
import org.json.JSONObject;

// Biblioteca para ler arquivos como strings
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Programa de Chat Multicast com troca de mensagens JSON e interface simples com JOptionPane.
 * Utiliza UDP Multicast para envio e recepção simultânea.
 */
public class ChatMulticast {

    // Endereço do grupo multicast (deve estar entre 224.0.0.0 e 239.255.255.255)
    private static final String MULTICAST_ADDRESS = "230.0.0.0";

    // Porta onde o chat multicast escuta e envia mensagens (todos os usuários devem usar a mesma)
    private static final int PORT = 8080;

    public static void main(String[] args) {
        boolean conectado = false;
        
        try {
            // Tenta carregar o arquivo externo template.json como base de configuração inicial
            JSONObject template = carregarJson("src/trabalhoredes1/template.json");

            // Se o template não for encontrado ou estiver inválido, interrompe o programa
            if (template == null) {
                JOptionPane.showMessageDialog(null, "template.json não encontrado ou mal formatado!", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Lê o campo "username" do JSON ou usa "Anônimo" se não estiver presente
            String username = template.optString("username", "Anônimo");

            // Solicita ao usuário que confirme ou altere o nome (pré-preenchido com o valor do JSON)
            String nomeUsuario = JOptionPane.showInputDialog(null, "Digite seu nome:", username);

            // Se o usuário cancelar ou deixar o campo vazio, o programa é encerrado
            if (nomeUsuario == null || nomeUsuario.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Nome inválido. Encerrando.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Cria um socket multicast na porta definida
            MulticastSocket socket = new MulticastSocket(PORT);

            // Resolve o IP do grupo multicast
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);

            // Junta-se ao grupo multicast para começar a receber mensagens dele
            socket.joinGroup(group);
            
            //Cria mensagem na primeira vez que conecta para poder ouvir os demais usuários do chat em diferentes computadores
            String welcomeMessege = "conectou-se ao chat";
            JSONObject jsonWelcome = new JSONObject();
            jsonWelcome.put("username", nomeUsuario);
            jsonWelcome.put("message", welcomeMessege);
            jsonWelcome.put("date", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
            jsonWelcome.put("time", new SimpleDateFormat("HH:mm:ss").format(new Date()));
            
            byte[] welcomeBytes = jsonWelcome.toString().getBytes();
            DatagramPacket welcomePacket = new DatagramPacket(welcomeBytes, welcomeBytes.length, group, PORT);
            socket.send(welcomePacket);
            // ------------------------------------

            // Thread responsável por receber mensagens de outros usuários via multicast
            Thread receiver = new Thread(() -> {
                // Buffer para armazenar os dados recebidos -> Armazena cerca de 680 caracteres para a lingua portuguesa
                // Parte do buffer já é consumido pelo arquivo JSON, portanto a mensagem pode armazenar menos de 680 caracteres
                // ASCII - 1 byte - 1024 bytes = 1024 caracteres simples(a-z, 0-9, pontuação)
                // Língua portuguesa - ~1.5 bytes = ~680 bytes = 680 caracteres(a-z + ç-ã, 0-9, pontuação)
                byte[] buffer = new byte[1024];
                while (true) {
                    try {
                        // Cria pacote de recepção
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                        // Aguarda o recebimento de um pacote
                        socket.receive(packet);

                        // Converte o conteúdo do pacote de bytes para String
                        String msg = new String(packet.getData(), 0, packet.getLength());

                        // Converte a string para um objeto JSON
                        JSONObject json = new JSONObject(msg);

                        // Extrai os campos do JSON
                        String date = json.getString("date");
                        String time = json.getString("time");
                        String user = json.getString("username");
                        String message = json.getString("message");

                        // Mostra a mensagem formatada no console (como histórico)
                        System.out.printf("[%s %s] %s: %s%n", date, time, user, message);
                        
                        // Exibe popup somente se não for a própria mensagem do usuário
                        if (!user.equals(nomeUsuario)) {
                            //JOptionPane.showMessageDialog(null, String.format("[%s %s] %s: %s", date, time, user, message), "Nova Mensagem", JOptionPane.INFORMATION_MESSAGE);
                        }

                    } catch (Exception e) {
                        // Mostra erro de recepção, se houver
                        JOptionPane.showMessageDialog(null, "Erro ao receber mensagem: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            // Inicia a thread que escuta mensagens recebidas
            receiver.start();

            // Cria leitor para entrada de texto pelo terminal (poderia ser JOptionPane, mas Scanner é prático em loop)
            Scanner scanner = new Scanner(System.in);
            
            System.out.println("    COMANDOS \nsair -> Encerrar programa \nhelp -> Lista comandos \n");

            // Loop principal para o envio de mensagens
            while (true) {
                               
                // Solicita mensagem via terminal
                System.out.print("Digite sua mensagem: ");
                String message = scanner.nextLine();
                
                if (message.equalsIgnoreCase("sair")) {
                    String exitMessege = "desconectou-se do chat";
                    JSONObject jsonExit = new JSONObject();
                    jsonExit.put("username", nomeUsuario);
                    jsonExit.put("message", exitMessege);
                    jsonExit.put("time", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
                    jsonExit.put("date", new SimpleDateFormat("HH:mm:ss").format(new Date()));
            
                    byte[] exitBytes = jsonExit.toString().getBytes();
                    DatagramPacket exitPacket = new DatagramPacket(exitBytes, exitBytes.length, group, PORT);
                    socket.send(exitPacket);
                    System.out.println("Encerrando");
                    socket.leaveGroup(group);
                    socket.close();
                    System.exit(0);
                }
                
                if (message.equalsIgnoreCase("help") || message.equalsIgnoreCase("ajuda")) {
                    System.out.println("    COMANDOS \nsair -> Encerrar programa \nhelp -> Lista comandos \n");
                }

                // Gera a data e hora atuais no formato esperado
                String date = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
                String time = new SimpleDateFormat("HH:mm:ss").format(new Date());

                // Cria objeto JSON com os dados da mensagem
                JSONObject json = new JSONObject();
                if (!(message.equalsIgnoreCase("help") || message.equalsIgnoreCase("ajuda"))) {
                    json.put("date", date);
                    json.put("time", time);
                    json.put("username", nomeUsuario);
                    json.put("message", message);
                }
                

                // Converte JSON em bytes para enviar via UDP
                byte[] msgBytes = json.toString().getBytes();

                // Cria pacote com os dados da mensagem para enviar ao grupo multicast
                DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, group, PORT);

                // Envia a mensagem para o grupo
                socket.send(packet);

                // Confirma ao usuário que a mensagem foi enviada com sucesso
                //JOptionPane.showMessageDialog(null, "Mensagem enviada!", "Enviado", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (IOException e) {
            // Captura qualquer exceção de rede ou leitura de arquivo
            JOptionPane.showMessageDialog(null, "Erro geral: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Função auxiliar para carregar e interpretar o conteúdo de um arquivo JSON externo.
     * @param caminho Caminho para o arquivo (ex: "template.json")
     * @return JSONObject com os dados do arquivo, ou null se erro
     */
    public static JSONObject carregarJson(String caminho) {
        try {
            // Lê todos os bytes do arquivo como uma string
            String conteudo = new String(Files.readAllBytes(Paths.get(caminho)));

            // Retorna um objeto JSON a partir do conteúdo
            return new JSONObject(conteudo);
        } catch (Exception e) {
            // Em caso de erro (arquivo não encontrado, mal formado), retorna null
            System.err.println("Erro ao carregar o JSON: " + e.getMessage());
            return null;
        }
    }
}