package dinoadventure;

// THREAD 3: movimenta o cenario da cidade (predios e faixas da rua).
public class ThreadCenario extends Thread {

    private ControleJogo controle;

    public ThreadCenario(ControleJogo controle) {
        this.controle = controle;
    }

    @Override
    public void run() {
        while (controle.estaJogando() && !isInterrupted()) {

            try {
                Thread.sleep(30); // ~33 movimentos por segundo
            } catch (InterruptedException e) {
                break;
            }

            // Usa a velocidade que a ThreadPontuacao vai aumentando
            double velocidade = controle.getVelocidade();

            // Atualiza as posicoes do cenario (protegido por semaforo).
            // A tela so le essas posicoes quando vai desenhar.
            controle.moverCenario(velocidade);
        }
    }
}