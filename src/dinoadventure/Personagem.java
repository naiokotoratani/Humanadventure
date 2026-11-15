package dinoadventure;

import java.awt.Rectangle;
import java.util.concurrent.Semaphore;

// Controla a pessoa que corre: posicao vertical, pulo e gravidade.
// O personagem e um recurso compartilhado:
//  - a EDT (tela) chama pular() e atualizar()
//  - a ThreadObstaculos le a posicao para verificar colisao
// Por isso ele tem o seu proprio semaforo.
public class Personagem {

    public static final int X = 80;        // posicao horizontal fixa
    public static final int LARGURA = 30;
    public static final int ALTURA = 48;

    private static final double GRAVIDADE = 0.8;
    private static final double FORCA_PULO = -16;
    private static final double Y_CHAO = ControleJogo.CHAO - ALTURA;

    private double y = Y_CHAO;
    private double velocidadeY = 0;
    private boolean noChao = true;

    // Semaforo com 1 permissao = so uma thread usa o personagem por vez
    private Semaphore semaforo = new Semaphore(1);

    // Volta o personagem para o chao (usado ao iniciar uma partida)
    public void resetar() {
        try {
            semaforo.acquire();
            try {
                y = Y_CHAO;
                velocidadeY = 0;
                noChao = true;
            } finally {
                semaforo.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Chamado quando o jogador aperta ESPACO
    public void pular() {
        try {
            semaforo.acquire();
            try {
                // So pula se estiver no chao: impede pulo duplo no ar
                if (noChao) {
                    velocidadeY = FORCA_PULO;
                    noChao = false;
                }
            } finally {
                semaforo.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Aplica a gravidade. Chamado a cada quadro pela tela.
    public void atualizar() {
        try {
            semaforo.acquire();
            try {
                if (!noChao) {
                    velocidadeY = velocidadeY + GRAVIDADE; // gravidade puxa para baixo
                    y = y + velocidadeY;

                    // Chegou no chao: para de cair
                    if (y >= Y_CHAO) {
                        y = Y_CHAO;
                        velocidadeY = 0;
                        noChao = true;
                    }
                }
            } finally {
                semaforo.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getY() {
        int valor = (int) Y_CHAO;
        try {
            semaforo.acquire();
            try {
                valor = (int) y;
            } finally {
                semaforo.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return valor;
    }

    public boolean estaNoChao() {
        boolean valor = true;
        try {
            semaforo.acquire();
            try {
                valor = noChao;
            } finally {
                semaforo.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return valor;
    }

    // Area usada na colisao. E um pouco menor que o desenho
    // para o jogo nao parecer injusto.
    public Rectangle getAreaColisao() {
        int posY = getY();
        return new Rectangle(X + 5, posY + 4, LARGURA - 10, ALTURA - 6);
    }
}