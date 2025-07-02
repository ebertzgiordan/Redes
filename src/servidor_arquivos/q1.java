package servidor_arquivos;
import java.util.Scanner;

public class q1 {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        int num = -1, neg = 0, pos = 0, soma = 0, n = 0;
        double media;
        System.out.println("Digite um número: ");
        num = input.nextInt();
        while (num != 0) {

            soma = soma + num;
            if (num != 0) {
                n++;
                if (num >= 0) {
                    pos++;
                } else {
                    neg++;
                }
            }
        System.out.println("Digite um número: ");
        num = input.nextInt();
        input.close();
        }
        media = soma / n;
        System.out.println("Foram digitados " + neg + " números negativos, " + pos + " positivos, e média de " + media);
        System.out.println("O código rodou " + n + " vezes.");
    }
}