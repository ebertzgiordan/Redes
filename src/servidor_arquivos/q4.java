import java.util.Scanner;

public class q4 {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        int num, result;
        System.out.println("Digite um número entre 1 e 10");
        num = Integer.parseInt(input.nextLine());
        while (num < 0 || num > 10) {
            System.out.println("Poxa tu é burro pra caramba em fi, tenta de novo");
            num = Integer.parseInt(input.nextLine());
            input.close();
        }for (int i = 1; i <= 10; i++) {
            result = num * i;
            System.out.println("Tabuada do " + num + " x " + i + " = " + result);
        }
    }
}
