package dinoadventure;

// THREAD 1: cria e movimenta os obstaculos e verifica colisoes.
// Roda em paralelo com ThreadPontuacao e ThreadCenario.
public class ThreadObstaculos extends Thread {

    private ControleJogo controle;

    public ThreadObstaculos(ControleJogo controle) {
        this.controle = controle;
    }

    // run() e o que a thread executa depois do start()
    @Override
    public void run() {
        int tempoParaNovo = 1000; // ms ate o proximo obstaculo

        // Continua enquanto a partida estiver rolando e a thread nao
        // tiver sido interrompida (encerramento correto da thread).
        while (controle.estaJogando() && !isInterrupted()) {

            try {
                // sleep controla o ritmo: a thread "dorme" 30 ms e deixa
                // as outras threads usarem o processador.
                Thread.sleep(30);
            } catch (InterruptedException e) {
                // Alguem chamou interrupt(): sai do laco e a thread termina
                break;
            }

            // Le a velocidade compartilhada (protegida por semaforo)
            double velocidade = controle.getVelocidade();

            // Move todos os obstaculos (protegido por semaforo)
            controle.moverObstaculos(velocidade);

            // Controle do intervalo de criacao
            tempoParaNovo = tempoParaNovo - 30;
            if (tempoParaNovo <= 0) {
                controle.criarObstaculo();

                // Quanto mais rapido o jogo, menor o intervalo
                tempoParaNovo = 1300 - (int) (velocidade * 30)
                        + controle.sortearNumero(900);
            }

            // Controle de colisao: se bateu, termina a partida
            if (controle.verificarColisao()) {
                controle.fimDeJogo();
            }
        }
    }
}