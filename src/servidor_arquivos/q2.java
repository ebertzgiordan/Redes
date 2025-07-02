package servidor_arquivos;
import java.util.Scanner;

public class q2 {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        double gast, alug, renda, alim, propali, propalu, gastmalug = 0, gastAlim2000 = 0, gastAlug2000 = 0,
                gasttalug = 0;
        int vezes = 0, renda2000 = 0;
        char continuar;

        do {
            System.out.println("Olá! Qual é o nome do aluno? ");
            String nome = input.nextLine();

            System.out.println(nome + " qual a sua renda mensal? ");
            renda = input.nextDouble();

            System.out.println("Qual o seu gasto com alimentação? ");
            alim = input.nextDouble();

            System.out.println("Qual o seu gasto com aluguel? ");
            alug = input.nextDouble();
            vezes++;
            gast = alim + alug;
            propali = (alim / renda) * 100;
            propalu = (alug / renda) * 100;
            gasttalug += alug;
            if (renda > 2000) {
                gastAlim2000 += propali;
                gastAlug2000 += propalu;
                renda2000++;
            }
            System.out.println("O gasto total do aluno: " + nome + " é de " + gast);
            System.out.println("A proporção de gastos com alimentação em relação a renda: " + propali);
            System.out.println("A proporção de gastos com aluguel em relação a renda: " + propalu);
            input.nextLine();
            System.out.print("Deseja continuar (S/N)? ");
            continuar = input.nextLine().toUpperCase().charAt(0);
            if(continuar != 'S' && continuar != 'N') {
                System.out.println("Bocó, tenta novamente o louco");
                continuar = input.nextLine().toUpperCase().charAt(0);
                input.close();
            }
        } while (continuar == 'S');
        gastmalug = gasttalug / vezes;
        System.out.println("O gasto médio com aluguel dos alunos é de: " + gastmalug);
        if (renda2000 > 0) {
            double gastMedioAlim2000 = gastAlim2000 / renda2000;
            double gastMedioAlug2000 = gastAlug2000 / renda2000;
            System.out.println(
                    "O gasto médio com alimentação dos alunos cuja renda familiar é superior a R$ 2000,00 é de: " + gastMedioAlim2000);
            System.out.println("O gasto médio com aluguel dos alunos cuja renda familiar é superior a R$ 2000,00 é de: " + gastMedioAlug2000);
        } else {
            System.out.println("Não há alunos com renda familiar superior a R$ 2000,00 para calcular a média.");
        }

    }
}
