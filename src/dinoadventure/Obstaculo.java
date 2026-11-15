package dinoadventure;

import java.awt.Rectangle;

// Representa um obstaculo da rua (cone, caixa ou barreira).
// Esta classe apenas guarda dados e se movimenta; ela NAO tem semaforo.
// Quem protege a lista de obstaculos e a classe ControleJogo.
public class Obstaculo {

    public static final int CONE = 0;
    public static final int CAIXA = 1;
    public static final int BARREIRA = 2;

    private double x;
    private int y;
    private int largura;
    private int altura;
    private int tipo;

    public Obstaculo(int tipo) {
        this.tipo = tipo;

        // Cada tipo de obstaculo tem um tamanho diferente
        if (tipo == CONE) {
            largura = 25;
            altura = 40;
        } else if (tipo == CAIXA) {
            largura = 40;
            altura = 40;
        } else {
            largura = 50;
            altura = 50;
        }

        // Nasce fora da tela, no lado direito, apoiado no chao
        x = ControleJogo.LARGURA;
        y = ControleJogo.CHAO - altura;
    }

    // Move o obstaculo para a esquerda
    public void mover(double velocidade) {
        x = x - velocidade;
    }

    // Retorna true quando o obstaculo ja saiu da tela pela esquerda
    public boolean saiuDaTela() {
        return x + largura < 0;
    }

    // Retangulo usado para verificar colisao
    public Rectangle getArea() {
        return new Rectangle((int) x, y, largura, altura);
    }

    public int getX() {
        return (int) x;
    }

    public int getY() {
        return y;
    }

    public int getLargura() {
        return largura;
    }

    public int getAltura() {
        return altura;
    }

    public int getTipo() {
        return tipo;
    }
}