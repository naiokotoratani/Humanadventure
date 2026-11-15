package dinoadventure;

// THREAD 2: atualiza pontuacao, tempo de sobrevivencia e velocidade.
public class ThreadPontuacao extends Thread {

    private ControleJogo controle;

    public ThreadPontuacao(ControleJogo controle) {
        this.controle = controle;
    }

    @Override
    public void run() {
        int contador = 0; // conta quantas vezes o laco rodou (1 volta = 100 ms)

        while (controle.estaJogando() && !isInterrupted()) {

            try {
                // Espera 100 ms entre uma atualizacao e outra
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break; // thread interrompida: encerra
            }

            // Mais 1 ponto a cada 100 ms (o semaforo fica dentro do metodo)
            controle.somarPontuacao(1);
            contador++;

            // 10 voltas de 100 ms = 1 segundo de sobrevivencia
            if (contador % 10 == 0) {
                controle.somarTempo(1);
            }

            // A cada 5 segundos o jogo fica mais rapido.
            // As outras duas threads leem essa velocidade e passam a andar mais rapido.
            if (contador % 50 == 0) {
                controle.aumentarVelocidade();
            }
        }
    }
}