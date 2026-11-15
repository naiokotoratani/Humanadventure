package dinoadventure;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;

// Cria a interface e desenha tudo: cenario, personagem, obstaculos e textos.
// Esta classe roda na EDT (thread da interface grafica do Swing).
// IMPORTANTE: as 3 threads do jogo NUNCA mexem nos componentes Swing.
// Elas so alteram dados (protegidos por semaforo); a tela le esses dados
// e desenha a cada quadro. Assim a interface continua responsiva.
public class TelaJogo extends JPanel implements ActionListener {

    private ControleJogo controle;
    private JButton botaoIniciar;
    private JButton botaoReiniciar;
    private Timer timer;      // Timer do Swing: dispara na EDT, a cada 16 ms
    private int quadro = 0;   // usado para animar as pernas

    // Predios que se repetem (largura total = 800)
    private int[] larguraPredios = { 80, 100, 70, 110, 90, 100, 120, 130 };
    private int[] alturaPredios = { 150, 200, 120, 230, 170, 210, 140, 190 };
    private Color[] corPredios = {
            new Color(90, 90, 120), new Color(110, 100, 130),
            new Color(80, 100, 110), new Color(120, 110, 110),
            new Color(95, 85, 115), new Color(100, 115, 125),
            new Color(115, 95, 100), new Color(85, 95, 130) };

    public TelaJogo(ControleJogo controle) {
        this.controle = controle;

        setPreferredSize(new Dimension(ControleJogo.LARGURA, ControleJogo.ALTURA));
        setLayout(null); // posicionamos os botoes manualmente
        setFocusable(true);

        // Botao de iniciar
        botaoIniciar = new JButton("INICIAR");
        botaoIniciar.setBounds(350, 230, 100, 35);
        botaoIniciar.setFocusable(false); // para o ESPACO continuar indo para a tela
        botaoIniciar.addActionListener(this);
        add(botaoIniciar);

        // Botao de reiniciar
        botaoReiniciar = new JButton("REINICIAR");
        botaoReiniciar.setBounds(340, 260, 120, 35);
        botaoReiniciar.setFocusable(false);
        botaoReiniciar.addActionListener(this);
        botaoReiniciar.setVisible(false);
        add(botaoReiniciar);

        // Tecla ESPACO faz o personagem pular
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE && TelaJogo.this.controle.estaJogando()) {
                    TelaJogo.this.controle.getPersonagem().pular();
                }
            }
        });

        // Timer do Swing: chama actionPerformed a cada 16 ms (~60 quadros/s)
        timer = new Timer(16, this);
        timer.start();
    }

    // Chamado pelos botoes E pelo Timer
    @Override
    public void actionPerformed(ActionEvent e) {
        Object origem = e.getSource();

        if (origem == botaoIniciar || origem == botaoReiniciar) {
            // Cria as threads e comeca a partida
            controle.iniciarPartida();
            requestFocusInWindow();
        } else {
            atualizarQuadro();
        }
    }

    // Executado a cada 16 ms na EDT
    private void atualizarQuadro() {
        int estado = controle.getEstado();

        if (estado == ControleJogo.JOGANDO) {
            controle.getPersonagem().atualizar(); // gravidade
            quadro++;
        }

        // Mostra o botao certo para cada estado
        botaoIniciar.setVisible(estado == ControleJogo.INICIO);
        botaoReiniciar.setVisible(estado == ControleJogo.GAME_OVER);

        repaint(); // pede para redesenhar (a EDT chama paintComponent)
    }

    // =====================================================
    //  DESENHO
    // =====================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        desenharCenario(g);
        desenharObstaculos(g);
        desenharPersonagem(g);
        desenharPlacar(g);

        int estado = controle.getEstado();
        if (estado == ControleJogo.INICIO) {
            desenharTelaInicial(g);
        } else if (estado == ControleJogo.GAME_OVER) {
            desenharGameOver(g);
        }
    }

    private void desenharCenario(Graphics g) {
        // Ceu
        g.setColor(new Color(150, 200, 230));
        g.fillRect(0, 0, ControleJogo.LARGURA, ControleJogo.ALTURA);

        // Sol quadrado (pixel art)
        g.setColor(new Color(255, 230, 90));
        g.fillRect(650, 30, 50, 50);

        // Predios: desenhamos duas copias lado a lado para o cenario
        // parecer infinito enquanto se move
        int inicio = -(int) controle.getDeslocPredios();
        for (int copia = 0; copia < 2; copia++) {
            int x = inicio + copia * ControleJogo.LARGURA;
            for (int i = 0; i < larguraPredios.length; i++) {
                desenharPredio(g, x, i);
                x = x + larguraPredios[i];
            }
        }

        // Calcada
        g.setColor(new Color(170, 170, 170));
        g.fillRect(0, ControleJogo.CHAO, ControleJogo.LARGURA, 20);

        // Rua
        g.setColor(new Color(60, 60, 65));
        g.fillRect(0, ControleJogo.CHAO + 20, ControleJogo.LARGURA, 60);

        // Faixas da rua
        g.setColor(Color.WHITE);
        int deslocFaixa = -(int) controle.getDeslocFaixas();
        for (int x = deslocFaixa; x < ControleJogo.LARGURA; x = x + 80) {
            g.fillRect(x, 368, 40, 6);
        }
    }

    private void desenharPredio(Graphics g, int x, int i) {
        int largura = larguraPredios[i];
        int altura = alturaPredios[i];
        int topo = ControleJogo.CHAO - altura;

        g.setColor(corPredios[i]);
        g.fillRect(x, topo, largura, altura);

        // Janelas
        for (int wy = topo + 12; wy + 10 < ControleJogo.CHAO - 8; wy = wy + 20) {
            for (int wx = x + 8; wx + 8 < x + largura - 4; wx = wx + 16) {
                if ((wx + wy) % 3 == 0) {
                    g.setColor(new Color(255, 240, 150)); // janela acesa
                } else {
                    g.setColor(new Color(50, 60, 80));
                }
                g.fillRect(wx, wy, 8, 10);
            }
        }
    }

    private void desenharObstaculos(Graphics g) {
        // Copia da lista (feita dentro do semaforo) para desenhar sem conflito
        ArrayList<Obstaculo> lista = controle.copiarObstaculos();

        for (int i = 0; i < lista.size(); i++) {
            Obstaculo o = lista.get(i);
            int x = o.getX();
            int y = o.getY();

            if (o.getTipo() == Obstaculo.CONE) {
                desenharCone(g, x, y);
            } else if (o.getTipo() == Obstaculo.CAIXA) {
                desenharCaixa(g, x, y);
            } else {
                desenharBarreira(g, x, y);
            }
        }
    }

    private void desenharCone(Graphics g, int x, int y) {
        g.setColor(new Color(60, 60, 60));
        g.fillRect(x, y + 34, 25, 6);          // base
        g.setColor(new Color(255, 120, 0));
        g.fillRect(x + 4, y + 20, 17, 14);     // parte de baixo
        g.fillRect(x + 7, y + 6, 11, 14);      // parte de cima
        g.setColor(Color.WHITE);
        g.fillRect(x + 6, y + 24, 13, 4);      // faixa branca
        g.fillRect(x + 8, y + 12, 9, 3);       // faixa branca
    }

    private void desenharCaixa(Graphics g, int x, int y) {
        g.setColor(new Color(160, 110, 50));
        g.fillRect(x, y, 40, 40);
        g.setColor(new Color(100, 65, 25));
        g.drawRect(x, y, 39, 39);
        g.drawLine(x, y, x + 39, y + 39);
        g.drawLine(x + 39, y, x, y + 39);
    }

    private void desenharBarreira(Graphics g, int x, int y) {
        g.setColor(new Color(90, 90, 90));
        g.fillRect(x + 4, y + 14, 6, 36);      // perna esquerda
        g.fillRect(x + 40, y + 14, 6, 36);     // perna direita

        // Tabua listrada vermelha e branca
        for (int i = 0; i < 5; i++) {
            if (i % 2 == 0) {
                g.setColor(Color.RED);
            } else {
                g.setColor(Color.WHITE);
            }
            g.fillRect(x + i * 10, y, 10, 16);
        }
        g.setColor(Color.BLACK);
        g.drawRect(x, y, 49, 15);
    }

    // Pessoa em pixel art feita so com retangulos
    private void desenharPersonagem(Graphics g) {
        int x = Personagem.X;
        int y = controle.getPersonagem().getY();
        boolean noChao = controle.getPersonagem().estaNoChao();

        // Cabelo
        g.setColor(new Color(80, 45, 20));
        g.fillRect(x + 6, y, 18, 6);
        // Cabeca
        g.setColor(new Color(240, 190, 150));
        g.fillRect(x + 6, y + 6, 18, 12);
        // Olho
        g.setColor(Color.BLACK);
        g.fillRect(x + 18, y + 9, 3, 3);
        // Camisa
        g.setColor(new Color(40, 90, 200));
        g.fillRect(x + 3, y + 18, 24, 15);
        // Braco (balanca enquanto corre)
        g.setColor(new Color(240, 190, 150));
        if (noChao && (quadro / 6) % 2 == 0) {
            g.fillRect(x, y + 20, 6, 12);
        } else {
            g.fillRect(x + 24, y + 20, 6, 12);
        }
        // Calca
        g.setColor(new Color(40, 40, 70));
        g.fillRect(x + 6, y + 33, 18, 6);

        // Pernas: alternam enquanto corre; juntas e abertas no ar
        if (!noChao) {
            g.fillRect(x + 4, y + 39, 9, 9);
            g.fillRect(x + 17, y + 39, 9, 7);
        } else if ((quadro / 6) % 2 == 0) {
            g.fillRect(x + 6, y + 39, 9, 9);
            g.fillRect(x + 15, y + 39, 9, 6);
        } else {
            g.fillRect(x + 6, y + 39, 9, 6);
            g.fillRect(x + 15, y + 39, 9, 9);
        }
    }

    private void desenharPlacar(Graphics g) {
        g.setFont(new Font("Monospaced", Font.BOLD, 18));
        g.setColor(Color.BLACK);
        g.drawString("PONTOS: " + controle.getPontuacao(), 20, 28);
        g.drawString("TEMPO: " + controle.getTempo() + "s", 20, 50);
        g.drawString("RECORDE: " + controle.getRecorde(), 600, 28);
    }

    private void desenharTelaInicial(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, ControleJogo.LARGURA, ControleJogo.ALTURA);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 48));
        desenharCentralizado(g, "HUMAN ADVENTURE", 140);
        g.setFont(new Font("Monospaced", Font.PLAIN, 18));
        desenharCentralizado(g, "Pressione ESPACO para pular os obstaculos", 190);
    }

    private void desenharGameOver(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, ControleJogo.LARGURA, ControleJogo.ALTURA);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 48));
        desenharCentralizado(g, "GAME OVER", 150);
        g.setFont(new Font("Monospaced", Font.PLAIN, 20));
        desenharCentralizado(g, "Pontuacao: " + controle.getPontuacao(), 195);
    }

    private void desenharCentralizado(Graphics g, String texto, int y) {
        FontMetrics medidas = g.getFontMetrics();
        int x = (ControleJogo.LARGURA - medidas.stringWidth(texto)) / 2;
        g.drawString(texto, x, y);
    }
}