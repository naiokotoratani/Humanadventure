package dinoadventure;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

// Classe que inicia o programa.
public class Main {

    public static void main(String[] args) {

        final ControleJogo controle = new ControleJogo();

        // Componentes Swing devem ser criados na EDT (thread de eventos
        // da interface). invokeLater pede para a EDT executar este bloco.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame janela = new JFrame("HUMAN ADVENTURE");
                TelaJogo tela = new TelaJogo(controle);

                janela.add(tela);
                janela.pack();
                janela.setResizable(false);
                janela.setLocationRelativeTo(null);
                janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                // Ao fechar a janela, encerra as threads corretamente
                janela.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        controle.encerrarThreads();
                    }
                });

                janela.setVisible(true);
                tela.requestFocusInWindow(); // para a tecla ESPACO funcionar
            }
        });
    }
}