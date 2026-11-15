package dinoadventure;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Semaphore;

// Classe central do jogo. Guarda TODOS os recursos compartilhados
// entre as threads e controla o inicio e o fim da partida.
// Cada recurso compartilhado tem o seu proprio semaforo:
//   semEstado       -> estado da partida
//   semPontuacao    -> pontuacao, tempo e recorde
//   semVelocidade   -> velocidade do jogo
//   semObstaculos   -> lista de obstaculos
//   semCenario      -> posicao dos predios e das faixas
//
// REGRA: nenhum metodo segura dois semaforos ao mesmo tempo.
// Assim nunca acontece deadlock (threads esperando umas as outras para sempre).
public class ControleJogo {

    // Tamanho da tela e altura do chao (compartilhados por todas as classes)
    public static final int LARGURA = 800;
    public static final int ALTURA = 400;
    public static final int CHAO = 320;

    // Estados possiveis da partida
    public static final int INICIO = 0;
    public static final int JOGANDO = 1;
    public static final int GAME_OVER = 2;

    // Recursos compartilhados
    private int estado = INICIO;
    private int pontuacao = 0;
    private int tempo = 0;       // segundos sobrevividos
    private int recorde = 0;
    private double velocidade = 6;
    private double deslocPredios = 0;
    private double deslocFaixas = 0;
    private ArrayList<Obstaculo> obstaculos = new ArrayList<Obstaculo>();

    // Semaforos (1 permissao cada = exclusao mutua) 
    private Semaphore semEstado = new Semaphore(1);
    private Semaphore semPontuacao = new Semaphore(1);
    private Semaphore semVelocidade = new Semaphore(1);
    private Semaphore semObstaculos = new Semaphore(1);
    private Semaphore semCenario = new Semaphore(1);

    private Personagem personagem = new Personagem();
    private Random sorteio = new Random();

    //  As tres threads 
    private ThreadObstaculos threadObstaculos;
    private ThreadPontuacao threadPontuacao;
    private ThreadCenario threadCenario;

    
    //  INICIO, FIM E ENCERRAMENTO DA PARTIDA

    // Comeca (ou recomeca) uma partida.
    public void iniciarPartida() {
        // Antes de criar threads novas, garante que as antigas terminaram.
        // Assim o reinicio NAO cria threads duplicadas.
        encerrarThreads();

        resetarDados();
        personagem.resetar();
        setEstado(JOGANDO);

        // Cria as tres threads. Cada uma roda em paralelo com as outras.
        threadObstaculos = new ThreadObstaculos(this);
        threadPontuacao = new ThreadPontuacao(this);
        threadCenario = new ThreadCenario(this);

        // start() faz a thread comecar a executar o seu metodo run()
        threadObstaculos.start();
        threadPontuacao.start();
        threadCenario.start();
    }

    // Volta os dados para os valores iniciais, cada um dentro do seu semaforo
    private void resetarDados() {
        try {
            semPontuacao.acquire();
            try {
                pontuacao = 0;
                tempo = 0;
            } finally {
                semPontuacao.release();
            }

            semVelocidade.acquire();
            try {
                velocidade = 6;
            } finally {
                semVelocidade.release();
            }

            semObstaculos.acquire();
            try {
                obstaculos.clear();
            } finally {
                semObstaculos.release();
            }

            semCenario.acquire();
            try {
                deslocPredios = 0;
                deslocFaixas = 0;
            } finally {
                semCenario.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Chamado pela ThreadObstaculos quando ha colisao.
    public void fimDeJogo() {
        boolean acabouAgora = false;

        try {
            semEstado.acquire();
            try {
                // So muda se ainda estava jogando. Isso evita processar
                // o Game Over duas vezes (verificacao e mudanca juntas
                // dentro da mesma secao critica).
                if (estado == JOGANDO) {
                    estado = GAME_OVER;
                    acabouAgora = true;
                }
            } finally {
                semEstado.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // As threads percebem o novo estado no "while" do run() e terminam sozinhas.
        if (acabouAgora) {
            atualizarRecorde();
        }
    }

    private void atualizarRecorde() {
        try {
            semPontuacao.acquire();
            try {
                if (pontuacao > recorde) {
                    recorde = pontuacao;
                }
            } finally {
                semPontuacao.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Para as threads de forma correta: interrupt() acorda quem esta no sleep(),
    // join() espera a thread realmente terminar.
    public void encerrarThreads() {
        pararThread(threadObstaculos);
        pararThread(threadPontuacao);
        pararThread(threadCenario);

        // Referencias antigas descartadas
        threadObstaculos = null;
        threadPontuacao = null;
        threadCenario = null;
    }

    private void pararThread(Thread thread) {
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(500); // espera no maximo 0,5 segundo
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // =====================================================
    //  ESTADO
    // =====================================================

    public int getEstado() {
        int valor = INICIO;
        try {
            semEstado.acquire();
            try {
                valor = estado;
            } finally {
                semEstado.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return valor;
    }

    private void setEstado(int novoEstado) {
        try {
            semEstado.acquire();
            try {
                estado = novoEstado;
            } finally {
                semEstado.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean estaJogando() {
        return getEstado() == JOGANDO;
    }

    //  PONTUACAO E TEMPO (usados pela ThreadPontuacao)
    
    public void somarPontuacao(int pontos) {
        try {
            semPontuacao.acquire();
            try {
                // Sem o semaforo, a thread e a tela poderiam ler/escrever
                // ao mesmo tempo e o valor ficaria inconsistente.
                pontuacao = pontuacao + pontos;
            } finally {
                semPontuacao.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void somarTempo(int segundos) {
        try {
            semPontuacao.acquire();
            try {
                tempo = tempo + segundos;
            } finally {
                semPontuacao.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getPontuacao() {
        int valor = 0;
        try {
            semPontuacao.acquire();
            try {
                valor = pontuacao;
            } finally {
                semPontuacao.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return valor;
    }

    public int getTempo() {
        int valor = 0;
        try {
            semPontuacao.acquire();
            try {
                valor = tempo;
            } finally {
                semPontuacao.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return valor;
    }

    public int getRecorde() {
        int valor = 0;
        try {
            semPontuacao.acquire();
            try {
                valor = recorde;
            } finally {
                semPontuacao.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return valor;
    }

    //  VELOCIDADE (escrita pela ThreadPontuacao, lida pelas outras duas)

    public double getVelocidade() {
        double valor = 6;
        try {
            semVelocidade.acquire();
            try {
                valor = velocidade;
            } finally {
                semVelocidade.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return valor;
    }

    public void aumentarVelocidade() {
        try {
            semVelocidade.acquire();
            try {
                if (velocidade < 16) {
                    velocidade = velocidade + 0.5;
                }
            } finally {
                semVelocidade.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    //  OBSTACULOS (usados pela ThreadObstaculos e desenhados pela tela)

    public void criarObstaculo() {
        int tipo = sorteio.nextInt(3); // sorteia 0, 1 ou 2
        Obstaculo novo = new Obstaculo(tipo);

        try {
            semObstaculos.acquire();
            try {
                obstaculos.add(novo);
            } finally {
                semObstaculos.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int sortearNumero(int limite) {
        return sorteio.nextInt(limite);
    }

    public void moverObstaculos(double velocidadeAtual) {
        try {
            // O semaforo impede que a tela desenhe a lista enquanto
            // ela esta sendo alterada (evita erro e dados inconsistentes).
            semObstaculos.acquire();
            try {
                // Percorre de tras para frente para poder remover com seguranca
                for (int i = obstaculos.size() - 1; i >= 0; i--) {
                    Obstaculo o = obstaculos.get(i);
                    o.mover(velocidadeAtual);

                    if (o.saiuDaTela()) {
                        obstaculos.remove(i);
                    }
                }
            } finally {
                semObstaculos.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Retorna true se algum obstaculo encostou no personagem
    public boolean verificarColisao() {
        // Pega a area do personagem ANTES de pegar o semaforo da lista.
        // (o personagem usa o semaforo dele; nunca seguramos dois ao mesmo tempo)
        Rectangle areaPersonagem = personagem.getAreaColisao();
        boolean bateu = false;

        try {
            semObstaculos.acquire();
            try {
                for (int i = 0; i < obstaculos.size(); i++) {
                    if (areaPersonagem.intersects(obstaculos.get(i).getArea())) {
                        bateu = true;
                    }
                }
            } finally {
                semObstaculos.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return bateu;
    }

    // A tela recebe uma COPIA da lista. Assim ela desenha com calma,
    // sem segurar o semaforo e sem atrapalhar a ThreadObstaculos.
    public ArrayList<Obstaculo> copiarObstaculos() {
        ArrayList<Obstaculo> copia = new ArrayList<Obstaculo>();
        try {
            semObstaculos.acquire();
            try {
                copia.addAll(obstaculos);
            } finally {
                semObstaculos.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return copia;
    }

    //  CENARIO (escrito pela ThreadCenario, lido pela tela)

    public void moverCenario(double velocidadeAtual) {
        try {
            semCenario.acquire();
            try {
                // Predios andam mais devagar que a rua (efeito de profundidade)
                deslocPredios = deslocPredios + velocidadeAtual * 0.3;
                deslocFaixas = deslocFaixas + velocidadeAtual;

                // Mantem os valores pequenos: o cenario se repete
                if (deslocPredios >= LARGURA) {
                    deslocPredios = deslocPredios - LARGURA;
                }
                if (deslocFaixas >= 80) {
                    deslocFaixas = deslocFaixas - 80;
                }
            } finally {
                semCenario.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public double getDeslocPredios() {
        double valor = 0;
        try {
            semCenario.acquire();
            try {
                valor = deslocPredios;
            } finally {
                semCenario.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return valor;
    }

    public double getDeslocFaixas() {
        double valor = 0;
        try {
            semCenario.acquire();
            try {
                valor = deslocFaixas;
            } finally {
                semCenario.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return valor;
    }

    public Personagem getPersonagem() {
        return personagem;
    }
}