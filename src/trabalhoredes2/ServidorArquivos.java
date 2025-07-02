package trabalhoredes2;

// ServidorArquivos.java

// Importações para entrada/saída de dados
import java.io.*;
// Importações para comunicação via socket TCP
import java.net.*;
// Importações para manipulação de arquivos
import java.nio.file.*;
// Importações para gerar hash criptográfico
import java.security.MessageDigest;
// Importações para listas, arrays, etc.
import java.util.*;
// Biblioteca para manipulação de objetos JSON
import org.json.*;
// Importação para codificação/decodificação Base64
import java.util.Base64;

public class ServidorArquivos {
    // Porta onde o servidor vai escutar conexões
    private static final int PORTA = 5001;

    // Caminho absoluto onde os arquivos serão salvos e lidos
    private static final String DIRETORIO_ARQUIVOS = ("src/servidor_arquivos");

    public static void main(String[] args) {
        try {
            // Garante que o diretório para salvar arquivos exista
            Files.createDirectories(Paths.get(DIRETORIO_ARQUIVOS));

            // Cria o socket do servidor escutando na porta definida
            ServerSocket serverSocket = new ServerSocket(PORTA);
            System.out.println("Servidor iniciado na porta " + PORTA);

            // Loop infinito para aceitar múltiplos clientes
            while (true) {
                Socket cliente = serverSocket.accept(); // Espera conexão de cliente
                System.out.println("Cliente conectado: " + cliente.getInetAddress());

                // Cria uma nova thread para lidar com cada cliente de forma independente
                new Thread(() -> {
                    try {
                        // Leitor para receber dados do cliente
                        BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
                        // Escritor para enviar dados ao cliente
                        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(cliente.getOutputStream()));

                        String linha;
                        // Loop para processar comandos enquanto o cliente envia mensagens
                        while ((linha = in.readLine()) != null) {
                            // Converte a linha recebida para um objeto JSON
                            JSONObject req = new JSONObject(linha);
                            String cmd = req.getString("cmd"); // Extrai o comando

                            // ===========================
                            // Comando LIST_REQ
                            // Cliente pede a lista de arquivos disponíveis no servidor
                            // ===========================
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

                            // ===========================
                            // Comando PUT_REQ
                            // Cliente envia um arquivo para o servidor
                            // ===========================
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

                            // ===========================
                            // Comando GET_REQ
                            // Cliente solicita o download de um arquivo
                            // ===========================
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