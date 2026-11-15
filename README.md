# HUMAN ADVENTURE — Documentação para apresentação (Sistemas Operacionais)

## 1. Objetivo e funcionamento
Jogo em Java inspirado no Dino do Google Chrome. Uma pessoa em pixel art corre numa cidade e pula (ESPAÇO) obstáculos (cones, caixas, barreiras). Ao colidir, aparece Game Over. A pontuação sobe com o tempo e a velocidade aumenta a cada 5 segundos.
O foco acadêmico é mostrar **threads rodando ao mesmo tempo** e **semáforos protegendo dados compartilhados**.

## 2. Bibliotecas
- `javax.swing` (JFrame, JPanel, JButton, Timer, SwingUtilities): interface.
- `java.awt` (Graphics, Color, Rectangle, eventos de teclado): desenho e teclado.
- `java.util.concurrent.Semaphore`: sincronização.
- `java.util` (ArrayList, Random).

## 3. Responsabilidade de cada classe
| Classe | Função |
|---|---|
| Main | Cria a janela na EDT (`invokeLater`) e fecha as threads ao sair. |
| TelaJogo | Desenha tudo, trata ESPAÇO e botões. Usa um `Timer` do Swing (16 ms) na EDT. |
| Personagem | Pulo e gravidade; tem semáforo próprio. |
| Obstaculo | Dados e movimento de um obstáculo. |
| ControleJogo | Guarda os recursos compartilhados, os semáforos e cria/encerra as threads. |
| ThreadObstaculos | Cria e move obstáculos; verifica colisão. |
| ThreadPontuacao | Soma pontos e tempo; aumenta a velocidade. |
| ThreadCenario | Move prédios e faixas da rua. |

## 4. Como as threads funcionam ao mesmo tempo
Em `ControleJogo.iniciarPartida()` as três threads recebem `start()`. Cada uma tem o seu `run()` com um `while` e um `Thread.sleep()`:
- ThreadObstaculos: `sleep(30)` → move obstáculos → às vezes cria um → testa colisão.
- ThreadPontuacao: `sleep(100)` → +1 ponto; a cada 10 voltas +1 segundo; a cada 50 voltas aumenta a velocidade.
- ThreadCenario: `sleep(30)` → desloca prédios (30% da velocidade) e faixas (100%).

Elas **compartilham dados reais**: a ThreadPontuacao escreve a velocidade; as outras duas a leem. A tela (EDT) lê tudo para desenhar. `sleep()` faz a thread "dormir" e liberar o processador para as demais.

## 5. Como os semáforos controlam os recursos
Cada recurso tem um `Semaphore(1)` (exclusão mutua):
| Semáforo | Protege | Quem usa |
|---|---|---|
| semEstado | estado da partida | todas as threads, tela |
| semPontuacao | pontos, tempo, recorde | ThreadPontuacao, tela |
| semVelocidade | velocidade | ThreadPontuacao (escreve); ThreadObstaculos e ThreadCenario (leem) |
| semObstaculos | lista de obstáculos | ThreadObstaculos, tela |
| semCenario | posições do cenário | ThreadCenario, tela |
| (Personagem) semaforo | posição do personagem | tela (pulo/gravidade), ThreadObstaculos (colisão) |

Padrão usado em todo o código:
```java
semObstaculos.acquire();   // pede a permissão (espera se outra thread estiver usando)
try {
    obstaculos.add(novo);  // região crítica
} finally {
    semObstaculos.release(); // devolve SEMPRE, mesmo se der erro
}
```

## 6. Exemplos de sincronização no código
1. **Lista de obstáculos**: `moverObstaculos()` (ThreadObstaculos) altera a lista enquanto `copiarObstaculos()` (tela) a lê. O semáforo faz um esperar o outro. A tela desenha uma *cópia*, então segura o semáforo só por um instante.
2. **Game Over uma única vez**: em `fimDeJogo()` a verificação `if (estado == JOGANDO)` e a mudança para `GAME_OVER` ficam dentro do mesmo `acquire/release`.
3. **Velocidade**: `aumentarVelocidade()` (ThreadPontuacao) e `getVelocidade()` (outras threads) usam `semVelocidade`.
4. **Personagem**: `pular()`/`atualizar()` (EDT) e `getAreaColisao()` (ThreadObstaculos) usam o semáforo do personagem.

## 7. Problemas de concorrência evitados
- **Condição de corrida**: duas threads mudando/lendo o mesmo dado ao mesmo tempo (ex.: pontuação, lista).
- **Inconsistência**: tela desenhando uma lista pela metade.
- **Deadlock**: nenhum método segura dois semáforos ao mesmo tempo (regra escrita em `ControleJogo`).
- **Threads duplicadas**: `iniciarPartida()` chama `encerrarThreads()` (`interrupt()` + `join()`) antes de criar novas.
- **Congelamento da interface**: as threads nunca tocam em componentes Swing; só alteram dados. A EDT desenha.
- **Game Over duplicado**: tratado no exemplo 2.

## 8. Como executar e testar
1. Eclipse → projeto Java → `src` → pacote `dinoadventure` (já criado).
2. Copie as 8 classes `.java` para o pacote (não use `module-info.java`).
3. Clique em `Main.java` → Run As → Java Application.
4. Testes: (a) clique INICIAR; (b) ESPAÇO pula e não há pulo duplo no ar; (c) pontos e tempo sobem; (d) a velocidade aumenta (cenário e obstáculos mais rápidos) a cada 5 s; (e) bata num obstáculo → GAME OVER; (f) REINICIAR várias vezes — o jogo continua com a mesma fluidez (sem threads duplicadas).
5. Demonstração para a banca: no `ControleJogo`, comente temporariamente o `acquire/release` de `semObstaculos` em `moverObstaculos` e `copiarObstaculos` para discutir o risco da condição de corrida (depois desfaça).
