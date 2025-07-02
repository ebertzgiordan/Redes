package trabalhoredes3;
// Para UDP: DatagramSocket, DatagramPacket
import java.net.*; 
//IOException
import java.io.*; 
// Para usar UTF-8
import java.nio.charset.StandardCharsets;
// Para Map
import java.util.*; 
// Manipulação de JSON
import org.json.JSONObject; 
import org.json.JSONArray;

public class ServerUDP {
    // Mapa que armazena o estado de cada dispositivo (sensor/atuador)
    private static Map<String, Object> dispositivos = new HashMap<>();

    public static void main(String[] args) {
        try {
            // Carrega os dados iniciais do arquivo JSON
            carregarConfiguracao("src/trabalhoredes3/dispositivos.json");


            int porta = 9876;
            DatagramSocket socket = new DatagramSocket(porta);
            System.out.println("Servidor UDP iniciado no localhost 127.0.0.1 e porta " + porta + "...");

            byte[] bufferRecebimento = new byte[1024];

            while (true) {
                // Recebe o pacote do cliente
                DatagramPacket pacoteRecebido = new DatagramPacket(bufferRecebimento, bufferRecebimento.length);
                socket.receive(pacoteRecebido);

                String recebido = new String(pacoteRecebido.getData(), 0, pacoteRecebido.getLength(), StandardCharsets.UTF_8);
                JSONObject requisicao = new JSONObject(recebido);

                System.out.println("Recebido: " + requisicao.toString());

                JSONObject resposta = new JSONObject();

                String cmd = requisicao.optString("cmd");

                switch (cmd) {
                    case "list_req":
                        resposta.put("cmd", "list_resp");
                        JSONArray lista = new JSONArray();
                        for (String chave : dispositivos.keySet()) {
                            lista.put(chave);
                        }
                        resposta.put("place", lista);
                        break;

                    case "get_req":
                        String place = requisicao.optString("place");
                        resposta.put("cmd", "get_resp");

                        if (place.equals("all")) {
                            JSONArray chaves = new JSONArray();
                            JSONArray valores = new JSONArray();
                            for (Map.Entry<String, Object> entrada : dispositivos.entrySet()) {
                                chaves.put(entrada.getKey());
                                valores.put(entrada.getValue());
                            }
                            resposta.put("place", chaves);
                            resposta.put("value", valores);
                        } else if (dispositivos.containsKey(place)) {
                            resposta.put("place", place);
                            resposta.put("value", dispositivos.get(place));
                        } else {
                            resposta.put("place", place);
                            resposta.put("value", "dispositivo não encontrado");
                        }
                        break;

                    case "set_req":
                        String local = requisicao.optString("locate");
                        Object valor = requisicao.get("value");

                        resposta.put("cmd", "set_resp");
                        resposta.put("locate", local);

                        if (local.startsWith("atuador")) {
                            if (dispositivos.containsKey(local)) {
                                dispositivos.put(local, valor);
                                resposta.put("value", valor);
                            } else {
                                resposta.put("value", "dispositivo não encontrado");
                            }
                        } else {
                            resposta.put("value", "erro: apenas atuadores podem ser alterados");
                        }
                        break;

                    default:
                        resposta.put("cmd", "erro");
                        resposta.put("mensagem", "comando inválido");
                        break;
                }

                // Envia a resposta de volta ao cliente
                byte[] dadosEnvio = resposta.toString().getBytes(StandardCharsets.UTF_8);
                DatagramPacket pacoteEnvio = new DatagramPacket(
                        dadosEnvio, dadosEnvio.length,
                        pacoteRecebido.getAddress(), pacoteRecebido.getPort()
                );
                socket.send(pacoteEnvio);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


     //Carrega os dispositivos e seus valores iniciais de um arquivo JSON.

    private static void carregarConfiguracao(String caminho) throws Exception {
        FileReader reader = new FileReader(caminho, StandardCharsets.UTF_8);
        StringBuilder conteudo = new StringBuilder();
        int c;
        while ((c = reader.read()) != -1) {
            conteudo.append((char) c);
        }
        reader.close();

        JSONObject json = new JSONObject(conteudo.toString());

        for (String chave : json.keySet()) {
            dispositivos.put(chave, json.get(chave));
        }

        System.out.println("Dispositivos carregados do arquivo: " + dispositivos);
    }
}
