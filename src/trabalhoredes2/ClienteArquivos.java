package trabalhoredes2;
// Importa classes para entrada/saída de dados
import java.io.*;
// Importa classes para comunicação via socket TCP
import java.net.*;
// Importa classes para manipulação de arquivos
import java.nio.file.*;
// Importa classe para gerar hash (SHA-256)
import java.security.MessageDigest;
// Importa utilitários como listas, etc.
import java.util.*;
// Importa a biblioteca JSON usada para construir e interpretar objetos JSON
import org.json.*;
// Importa utilitários para codificar e decodificar Base64
import java.util.Base64;
// Importa JOptionPane para interações gráficas (no cliente)
import javax.swing.JOptionPane;

public class ClienteArquivos {

    // Endereço IP do servidor (localhost para testes locais)
    private static final String SERVER_IP = "127.0.0.1";
    // Porta utilizada para a conexão TCP
    private static final int PORTA = 5001;
    
    private static final String caminhoSalvar
            = System.getProperty("user.home") + File.separator + "Downloads";

    public static void main(String[] args) {
        try {
            // Conecta ao servidor via socket TCP
            Socket socket = new Socket(SERVER_IP, PORTA);

            // Cria leitores e escritores para entrada e saída de dados no socket
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            JOptionPane.showMessageDialog(null, "Conectado ao servidor em " + SERVER_IP + ":" + PORTA);

            while (true) {
                // Menu de opções para o usuário selecionar
                String[] opcoes = {"LIST", "PUT", "GET", "SAIR"};
                String comando = (String) JOptionPane.showInputDialog(
                        null,
                        "Escolha um comando:",
                        "Cliente FTP JSON",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        opcoes,
                        "LIST"
                );

                if (comando == null || comando.equals("SAIR")) {
                    break; // Encerra o loop
                }
                switch (comando) {
                    case "LIST":
                        // Solicita lista de arquivos ao servidor
                        JSONObject reqList = new JSONObject();
                        reqList.put("cmd", "list_req");
                        out.write(reqList.toString() + "\n");
                        out.flush();

                        // Recebe a resposta do servidor com os nomes dos arquivos
                        JSONObject respList = new JSONObject(in.readLine());
                        JSONArray arquivos = respList.getJSONArray("files");

                        // Constrói a lista formatada para exibição
                        StringBuilder lista = new StringBuilder("Arquivos no servidor:\n");
                        for (int i = 0; i < arquivos.length(); i++) {
                            lista.append("- ").append(arquivos.getString(i)).append("\n");
                        }

                        JOptionPane.showMessageDialog(null, lista.toString());
                        break;

                    case "PUT":
                        // Pede ao usuário o caminho do arquivo local
                        String caminhoLocal = JOptionPane.showInputDialog("Digite o caminho do arquivo local:");
                        if (caminhoLocal == null) {
                            break;
                        }

                        File arquivoLocal = new File(caminhoLocal);
                        if (!arquivoLocal.exists() || !arquivoLocal.isFile()) {
                            JOptionPane.showMessageDialog(null, "Arquivo inválido ou é uma pasta.");
                            break;
                        }

                        // Lê o conteúdo do arquivo, gera hash e converte para Base64
                        byte[] dados = Files.readAllBytes(arquivoLocal.toPath());
                        String base64 = Base64.getEncoder().encodeToString(dados);
                        String hash = gerarHash(dados);

                        // Monta requisição JSON para PUT
                        JSONObject reqPut = new JSONObject();
                        reqPut.put("cmd", "put_req");
                        reqPut.put("file", arquivoLocal.getName());
                        reqPut.put("value", base64);
                        reqPut.put("hash", hash);

                        out.write(reqPut.toString() + "\n");
                        out.flush();

                        // Lê resposta do servidor
                        JSONObject respPut = new JSONObject(in.readLine());
                        String status = respPut.getString("status");
                        JOptionPane.showMessageDialog(null, "Envio de arquivo: " + status);
                        break;

                    case "GET":
                        // Pede ao usuário o nome do arquivo que deseja baixar
                        String nomeRemoto = JOptionPane.showInputDialog("Digite o nome do arquivo no servidor:");
                        if (nomeRemoto == null) {
                            break;
                        }

                        // Monta requisição JSON para GET
                        JSONObject reqGet = new JSONObject();
                        reqGet.put("cmd", "get_req");
                        reqGet.put("file", nomeRemoto);

                        out.write(reqGet.toString() + "\n");
                        out.flush();

                        // Recebe resposta do servidor
                        JSONObject respGet = new JSONObject(in.readLine());
                        String conteudo = respGet.getString("value");

                        if (conteudo.isEmpty()) {
                            JOptionPane.showMessageDialog(null, "Arquivo não encontrado no servidor.");
                        } else {
                            byte[] dadosArq = Base64.getDecoder().decode(conteudo);

                            // Define o caminho fixo para salvar o arquivo na pasta desejada
                            
                            Files.createDirectories(Paths.get(caminhoSalvar)); // Cria a pasta se não existir
                            Path caminhoFinal = Paths.get(caminhoSalvar, nomeRemoto);
                            Files.write(caminhoFinal, dadosArq);

                            // Confirma onde foi salvo
                            JOptionPane.showMessageDialog(null, "Arquivo salvo em: " + caminhoFinal.toString());
                        }
                        break;
                }
            }

            // Fecha o socket ao final
            socket.close();
            JOptionPane.showMessageDialog(null, "Conexão encerrada.");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método utilitário para gerar hash SHA-256 em formato Base64
    private static String gerarHash(byte[] dados) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return Base64.getEncoder().encodeToString(digest.digest(dados));
    }
}
