package trabalhoredes2;
//IOException
import java.io.*;
// API de rede(MulticastSocket, InetAddress, DatagramPacket)
import java.net.*;
// Manipulação de arquivos
import java.nio.file.*;
// Gerar hash (SHA-256)
import java.security.MessageDigest;
// Array
import java.util.*;
// Objetos JSON
import org.json.*;
// Codificação/decodificação Base64
import java.util.Base64;

public class ServidorArquivos {
    // Porta onde o servidor vai escutar conexões
    private static final int PORTA = 5001;

    // Onde os arquivos serão salvos e lidos
    private static final String DIRETORIO_ARQUIVOS = ("src/servidor_arquivos");

    public static void main(String[] args) {
        try {
            
            Files.createDirectories(Paths.get(DIRETORIO_ARQUIVOS));
            ServerSocket serverSocket = new ServerSocket(PORTA);
            System.out.println("Servidor iniciado na porta " + PORTA);

            // Loop infinito para aceitar múltiplos clientes
            while (true) {
                Socket cliente = serverSocket.accept(); // Espera conexão de cliente
                System.out.println("Cliente conectado: " + cliente.getInetAddress());

                // Cada cliente tera uma thread
                new Thread(() -> {
                    try {
                        // Buffer para receber
                        BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
                        // Buffer para enviar
                        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(cliente.getOutputStream()));

                        String linha;
                        // Loop para processar comandos enquanto o cliente envia mensagens
                        while ((linha = in.readLine()) != null) {
                            JSONObject req = new JSONObject(linha);
                            String cmd = req.getString("cmd"); // Extrai o comando
                            // Cliente pede a lista de arquivos disponíveis no servidor
                            if (cmd.equals("list_req")) {
                                File pasta = new File(DIRETORIO_ARQUIVOS);
                                String[] arquivos = pasta.list(); // Lista os nomes dos arquivos
                                JSONArray lista = new JSONArray(Arrays.asList(arquivos));

                                // Monta resposta com os arquivos
                                JSONObject resp = new JSONObject();
                                resp.put("cmd", "list_resp");
                                resp.put("files", lista);
                                out.write(resp.toString() + "\n");
                                out.flush();
                                
                            // Cliente envia um arquivo para o servidor
                            } else if (cmd.equals("put_req")) {
                                String nome = req.getString("file");
                                String base64 = req.getString("value");
                                byte[] dados = Base64.getDecoder().decode(base64); // Decodifica o conteúdo

                                Path destino = Paths.get(DIRETORIO_ARQUIVOS, nome); // Caminho final do arquivo
                                Files.write(destino, dados); // Escreve o arquivo

                                // Responde ao cliente com confirmação
                                JSONObject resp = new JSONObject();
                                resp.put("cmd", "put_resp");
                                resp.put("file", nome);
                                resp.put("status", "ok");
                                out.write(resp.toString() + "\n");
                                out.flush();

                            // Cliente solicita o download de um arquivo
                            } else if (cmd.equals("get_req")) {
                                String nome = req.getString("file");
                                Path caminho = Paths.get(DIRETORIO_ARQUIVOS, nome);

                                JSONObject resp = new JSONObject();
                                resp.put("cmd", "get_resp");
                                resp.put("file", nome);

                                // Verifica se o arquivo existe
                                if (Files.exists(caminho)) {
                                    byte[] dados = Files.readAllBytes(caminho);
                                    String base64 = Base64.getEncoder().encodeToString(dados); // Codifica o conteúdo
                                    String hash = gerarHash(dados); // Gera hash para verificação de integridade

                                    resp.put("value", base64);
                                    resp.put("hash", hash);
                                } else {
                                    // Se o arquivo não existir, envia campos vazios
                                    resp.put("value", "");
                                    resp.put("hash", "");
                                }

                                // Envia resposta ao cliente
                                out.write(resp.toString() + "\n");
                                out.flush();
                            }
                        }

                        // Encerra conexão após fim da comunicação
                        cliente.close();
                        System.out.println("Cliente desconectado.");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start(); // Inicia a thread
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Função utilitária para gerar hash SHA-256 de um array de bytes
    private static String gerarHash(byte[] dados) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return Base64.getEncoder().encodeToString(digest.digest(dados));
    }
}