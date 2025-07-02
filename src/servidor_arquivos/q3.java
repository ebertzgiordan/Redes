import java.util.Scanner;

public class q3 {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        System.out.print("Digite o valor do ângulo em graus: ");
        double graus = input.nextDouble();
        double radianos = Math.toRadians(graus);
        
        
        double seno = 0;
        for (int i = 0; i < 8; i++) {
            double termo = Math.pow(-1, i) * Math.pow(radianos, 2 * i + 1) / fatorial(2 * i + 1);
            seno += termo;
        }
        System.out.println("O seno de " + graus + " graus é: " + seno);
        input.close();
    }
    
    public static long fatorial(int n) {
        long resultado = 1;
        for (int i = 2; i <= n; i++) {
            resultado *= i;
        }
        return resultado;
    }
}
