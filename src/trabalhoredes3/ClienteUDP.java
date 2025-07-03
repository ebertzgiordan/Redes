package trabalhoredes3; 
// API de rede(MulticastSocket, InetAddress, DatagramPacket)
import java.net.*;
//IOException
import java.io.*;
// Para usar UTF-8
import java.nio.charset.StandardCharsets;
// Caixas de dialogo graficas
import javax.swing.*;
// Manipulacao JSON
import org.json.JSONObject;
import org.json.JSONArray;

public class ClienteUDP {

    private static boolean executandoPeriodico = false;
    private static int periodo = 5; // valor padrão

    public static void main(String[] args) {
        try {
            String ipServidor = JOptionPane.showInputDialog("Informe o IP do servidor (ex: localhost//IP maquina):");
            if (ipServidor == null || ipServidor.isBlank()) return;

            int porta = 9876;
            InetAddress enderecoServidor = InetAddress.getByName(ipServidor);
            DatagramSocket socket = new DatagramSocket();

            // Thread para envio periódico GET TODOS
            Thread threadPeriodico = new Thread(() -> {
                while (executandoPeriodico) {
                    try {
                        JSONObject requisicao = new JSONObject();
                        requisicao.put("cmd", "get_req");
                        requisicao.put("place", "all");

                        byte[] dadosEnvio = requisicao.toString().getBytes(StandardCharsets.UTF_8);
                        DatagramPacket pacoteEnvio = new DatagramPacket(dadosEnvio, dadosEnvio.length, enderecoServidor, porta);
                        socket.send(pacoteEnvio);

                        byte[] bufferRecebimento = new byte[1024];
                        DatagramPacket pacoteRecebido = new DatagramPacket(bufferRecebimento, bufferRecebimento.length);
                        socket.receive(pacoteRecebido);

                        String resposta = new String(pacoteRecebido.getData(), 0, pacoteRecebido.getLength(), StandardCharsets.UTF_8);
                        JSONObject respostaJSON = new JSONObject(resposta);

                        // Exibe a resposta no console
                        System.out.println("\n[RESPOSTA PERIODICA]");
                        mostrarRespostaConsole(respostaJSON);

                        Thread.sleep(periodo * 1000L);
                    } catch (Exception e) {
                        System.err.println("Erro no envio periódico: " + e.getMessage());
                        break;
                    }
                }
            });

            while (true) {
                String[] opcoes = { 
                    "LISTAR Dispositivos", 
                    "GET Um Dispositivo", 
                    "GET TODOS", 
                    "SET Atuador", 
                    executandoPeriodico ? "PARAR GET PERIODICO" : "INICIAR GET PERIODICO", 
                    "Sair" 
                };

                int escolha = JOptionPane.showOptionDialog(null, "Escolha uma ação:", "Menu Principal - Cliente UDP",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opcoes, opcoes[0]);

                if (escolha == 5 || escolha == JOptionPane.CLOSED_OPTION) break;

                JSONObject requisicao = new JSONObject();

                switch (escolha) {
                    case 0: // LIST
                        requisicao.put("cmd", "list_req");
                        break;

                    case 1: // GET
                        String getNome = JOptionPane.showInputDialog("Digite o nome do dispositivo:");
                        if (getNome == null || getNome.isBlank()) continue;
                        requisicao.put("cmd", "get_req");
                        requisicao.put("place", getNome);
                        break;

                    case 2: // GET ALL
                        requisicao.put("cmd", "get_req");
                        requisicao.put("place", "all");
                        break;

                    case 3: // SET
                        String setNome = JOptionPane.showInputDialog("Digite o nome do atuador:");
                        if (setNome == null || setNome.isBlank()) continue;
                        String valor = JOptionPane.showInputDialog("Digite o valor (ex: on, off, 23.0):");
                        if (valor == null || valor.isBlank()) continue;

                        Object valorObj;
                        try {
                            valorObj = Double.parseDouble(valor); // Tenta número
                        } catch (NumberFormatException e) {
                            valorObj = valor; // Se não for número, trata como string
                        }

                        requisicao.put("cmd", "set_req");
                        requisicao.put("locate", setNome);
                        requisicao.put("value", valorObj);
                        break;

                    case 4: // GET TODOS (PERIÓDICO) - inicia ou para
                        if (!executandoPeriodico) {
                            String sPeriodo = JOptionPane.showInputDialog("Informe o período em segundos:");
                            if (sPeriodo == null || sPeriodo.isBlank()) continue;

                            try {
                                periodo = Integer.parseInt(sPeriodo);
                                executandoPeriodico = true;
                                if (!threadPeriodico.isAlive()) threadPeriodico.start();
                                JOptionPane.showMessageDialog(null, "Envio periódico iniciado a cada " + periodo + " segundos.");
                            } catch (NumberFormatException e) {
                                JOptionPane.showMessageDialog(null, "Valor inválido para o período.");
                            }
                        } else {
                            executandoPeriodico = false;
                            JOptionPane.showMessageDialog(null, "Envio periódico PARADO.");
                        }
                        continue;
                }

                // Envia a requisição
                byte[] dadosEnvio = requisicao.toString().getBytes(StandardCharsets.UTF_8);
                DatagramPacket pacoteEnvio = new DatagramPacket(dadosEnvio, dadosEnvio.length, enderecoServidor, porta);
                socket.send(pacoteEnvio);

                // Recebe a resposta
                byte[] bufferRecebimento = new byte[1024];
                DatagramPacket pacoteRecebido = new DatagramPacket(bufferRecebimento, bufferRecebimento.length);
                socket.receive(pacoteRecebido);

                String resposta = new String(pacoteRecebido.getData(), 0, pacoteRecebido.getLength(), StandardCharsets.UTF_8);
                JSONObject respostaJSON = new JSONObject(resposta);

                // Exibe a resposta formatada
                mostrarRespostaFormatada(respostaJSON);
            }

            executandoPeriodico = false; // finaliza thread ao sair
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Erro: " + e.getMessage());
        }
    }

    private static void mostrarRespostaFormatada(JSONObject resposta) {
        StringBuilder mensagem = new StringBuilder();
        String cmd = resposta.optString("cmd");

        switch (cmd) {
            case "list_resp":
                mensagem.append("Dispositivos disponíveis:\n\n");
                JSONArray lista = resposta.optJSONArray("place");
                for (int i = 0; i < lista.length(); i++) {
                    mensagem.append("- ").append(lista.getString(i)).append("\n");
                }
                break;

            case "get_resp":
                Object place = resposta.get("place");
                Object value = resposta.get("value");

                if (place instanceof JSONArray && value instanceof JSONArray) {
                    mensagem.append("Estado atual de TODOS os dispositivos:\n\n");
                    JSONArray lugares = (JSONArray) place;
                    JSONArray valores = (JSONArray) value;
                    for (int i = 0; i < lugares.length(); i++) {
                        mensagem.append("- ").append(lugares.getString(i))
                                .append(" = ").append(valores.get(i)).append("\n");
                    }
                } else {
                    mensagem.append("").append(place).append(" = ").append(value).append("\n");
                }
                break;

            case "set_resp":
                mensagem.append("Atuador alterado com sucesso:\n");
                mensagem.append("- ").append(resposta.getString("locate"))
                        .append(" = ").append(resposta.get("value")).append("\n");
                break;

            default:
                mensagem.append("Resposta desconhecida:\n").append(resposta.toString(4));
                break;
        }

        JOptionPane.showMessageDialog(null, mensagem.toString(), "Resposta do Servidor", JOptionPane.INFORMATION_MESSAGE);
    }

    // Versão para impressão em console (usada na thread periódica)
    private static void mostrarRespostaConsole(JSONObject resposta) {
        StringBuilder mensagem = new StringBuilder();
        String cmd = resposta.optString("cmd");

        switch (cmd) {
            case "get_resp":
                Object place = resposta.get("place");
                Object value = resposta.get("value");

                if (place instanceof JSONArray && value instanceof JSONArray) {
                    JSONArray lugares = (JSONArray) place;
                    JSONArray valores = (JSONArray) value;
                    for (int i = 0; i < lugares.length(); i++) {
                        mensagem.append("- ").append(lugares.getString(i))
                                .append(" = ").append(valores.get(i)).append("\n");
                    }
                } else {
                    mensagem.append("- ").append(place).append(" = ").append(value).append("\n");
                }
                break;

            default:
                mensagem.append("Resposta: ").append(resposta.toString(4));
        }

        System.out.println(mensagem.toString());
    }
}
