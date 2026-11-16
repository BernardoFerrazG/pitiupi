package pitiupi.purrinha.control;

import pitiupi.models.Messages.Message;
import pitiupi.purrinha.net.JogadaMessage;
import pitiupi.purrinha.net.MesaMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Liga as regras ({@link Mesa}) à rede. Não desenha nada.
 *
 * <p>Quem abre a mesa manda {@link MesaMessage} a cada segundo (quem está na mesa, se começou).
 * Cada jogador manda {@link JogadaMessage} a cada segundo com tudo o que já fez.
 * Como é UDP, uma mensagem perdida é coberta pela do segundo seguinte.</p>
 *
 * <p>Todos os métodos rodam na thread do Swing.</p>
 */
public class PurrinhaController {

    private static final long SEM_NOTICIA_DO_DONO_MS = 30_000;

    private final String meuId;
    private final Supplier<String> meuNome;
    private final Consumer<Message> envio;
    private Consumer<String> narracao = texto -> {
    };

    private String mesaId;
    private String donoNome;
    private boolean souDono;
    private boolean comecou;
    private boolean pediEntrada;
    private final Map<String, String> participantes = new LinkedHashMap<>();
    private long ultimaMesaRecebida;

    private Mesa mesa;
    private final List<Integer> minhasMaos = new ArrayList<>();
    private final List<Integer> meusChutes = new ArrayList<>();
    private int rodadasNarradas;
    private int ganhadoresNarrados;
    private boolean fimNarrado;

    public PurrinhaController(String meuId, Supplier<String> meuNome, Consumer<Message> envio) {
        this.meuId = meuId;
        this.meuNome = meuNome;
        this.envio = envio;
    }

    public void setNarracao(Consumer<String> narracao) {
        this.narracao = narracao;
    }

    // ---------------------------------------------------------------- botões

    public String abrirMesa() {
        if (temMesaEmAndamento()) {
            return "Já tem uma mesa aberta na sala.";
        }
        limpar();
        mesaId = UUID.randomUUID().toString();
        donoNome = meuNome.get();
        souDono = true;
        participantes.put(meuId, meuNome.get());
        narracao.accept("Você abriu uma mesa. Esperando gente entrar (até " + Mesa.MAX_JOGADORES + ").");
        enviarMesa();
        return null;
    }

    public String entrar() {
        if (mesaId == null) {
            return "Nenhuma mesa aberta. Abra uma.";
        }
        if (souDono || participantes.containsKey(meuId)) {
            return "Você já está na mesa.";
        }
        if (comecou) {
            return "O jogo já começou.";
        }
        if (participantes.size() >= Mesa.MAX_JOGADORES) {
            return "A mesa está cheia.";
        }
        pediEntrada = true;
        narracao.accept("Pedido para entrar enviado para " + donoNome + ".");
        enviarJogada();
        return null;
    }

    public String comecar() {
        if (!souDono || comecou) {
            return "Só quem abriu a mesa pode começar.";
        }
        if (participantes.size() < 2) {
            return "Precisa de pelo menos 2 jogadores.";
        }
        comecou = true;
        mesa = new Mesa(new ArrayList<>(participantes.keySet()), new ArrayList<>(participantes.values()));
        narracao.accept("Jogo começou! Rodada 1: todo mundo bota os palitos.");
        enviarMesa();
        return null;
    }

    public String encerrar() {
        if (!souDono) {
            return "Só quem abriu a mesa pode encerrar.";
        }
        envio.accept(new MesaMessage(mesaId, meuId, donoNome, comecou, true,
                new ArrayList<>(participantes.keySet()), new ArrayList<>(participantes.values())));
        narracao.accept("Você encerrou a mesa.");
        limpar();
        return null;
    }

    public String botar(int quantos) {
        if (mesa == null || !mesa.podeBotar(meuId, quantos)) {
            return "Agora não dá para botar palitos.";
        }
        minhasMaos.add(quantos);
        mesa.atualizarJogador(meuId, minhasMaos, meusChutes);
        narracao.accept("Você botou " + quantos + (quantos == 1 ? " palito." : " palitos."));
        enviarJogada();
        narrarNovidades();
        return null;
    }

    public String chutar(int chute) {
        if (mesa == null) {
            return "Não tem jogo em andamento.";
        }
        String motivo = mesa.motivoParaNaoChutar(meuId, chute);
        if (motivo != null) {
            return motivo;
        }
        meusChutes.add(chute);
        mesa.atualizarJogador(meuId, minhasMaos, meusChutes);
        narracao.accept("Você chutou " + chute + ".");
        enviarJogada();
        narrarNovidades();
        return null;
    }

    /** Chamado a cada segundo: reenvia o estado, por causa do UDP. */
    public void aCadaSegundo() {
        if (mesaId == null) {
            return;
        }
        if (!souDono && System.currentTimeMillis() - ultimaMesaRecebida > SEM_NOTICIA_DO_DONO_MS) {
            narracao.accept("Faz tempo que " + donoNome + " não dá sinal. Mesa encerrada.");
            limpar();
            return;
        }
        if (souDono) {
            enviarMesa();
        }
        if (souDono || pediEntrada) {
            enviarJogada();
        }
    }

    // ---------------------------------------------------------------- rede

    public void receberMesa(MesaMessage m) {
        if (meuId.equals(m.getDonoId())) {
            return;
        }
        boolean mesmaMesa = m.getMesaId().equals(mesaId);
        if (mesmaMesa) {
            ultimaMesaRecebida = System.currentTimeMillis();
        }
        if (!mesmaMesa) {
            if (temMesaEmAndamento() || m.isEncerrada()) {
                return;
            }
            limpar();
            mesaId = m.getMesaId();
            donoNome = m.getDonoNome();
            ultimaMesaRecebida = System.currentTimeMillis();
            narracao.accept(donoNome + " abriu uma mesa de purrinha. Clique em Entrar para jogar.");
        }
        if (m.isEncerrada()) {
            narracao.accept(donoNome + " encerrou a mesa.");
            limpar();
            return;
        }
        if (!comecou) {
            boolean estavaNaMesa = participantes.containsKey(meuId);
            participantes.clear();
            for (int i = 0; i < m.getIds().size(); i++) {
                participantes.put(m.getIds().get(i), m.getNomes().get(i));
            }
            if (!estavaNaMesa && participantes.containsKey(meuId)) {
                narracao.accept("Você entrou na mesa de " + donoNome + ".");
            }
        }
        if (m.isComecou() && mesa == null) {
            comecou = true;
            mesa = new Mesa(m.getIds(), m.getNomes());
            if (participantes.containsKey(meuId)) {
                narracao.accept("Jogo começou! Rodada 1: todo mundo bota os palitos.");
            } else {
                narracao.accept("O jogo começou sem você. Dá para assistir.");
                pediEntrada = false;
            }
        }
    }

    public void receberJogada(JogadaMessage j) {
        if (meuId.equals(j.getJogadorId()) || !j.getMesaId().equals(mesaId)) {
            return;
        }
        if (souDono && !comecou && !participantes.containsKey(j.getJogadorId())) {
            if (participantes.size() < Mesa.MAX_JOGADORES) {
                participantes.put(j.getJogadorId(), j.getNome());
                narracao.accept(j.getNome() + " entrou na mesa.");
                enviarMesa();
            }
            return;
        }
        if (mesa != null) {
            mesa.atualizarJogador(j.getJogadorId(), j.getMaos(), j.getChutes());
            narrarNovidades();
        }
    }

    // ---------------------------------------------------------------- leitura para a tela

    public String getMeuId() {
        return meuId;
    }

    public String getMesaId() {
        return mesaId;
    }

    public String getDonoNome() {
        return donoNome;
    }

    public boolean isSouDono() {
        return souDono;
    }

    public boolean isComecou() {
        return comecou;
    }

    public Map<String, String> getParticipantes() {
        return participantes;
    }

    public Mesa getMesa() {
        return mesa;
    }

    // ---------------------------------------------------------------- internos

    private boolean temMesaEmAndamento() {
        return mesaId != null && (mesa == null || mesa.getFase() != Mesa.Fase.FIM);
    }

    private void enviarMesa() {
        envio.accept(new MesaMessage(mesaId, meuId, donoNome, comecou, false,
                new ArrayList<>(participantes.keySet()), new ArrayList<>(participantes.values())));
    }

    private void enviarJogada() {
        envio.accept(new JogadaMessage(mesaId, meuId, meuNome.get(), minhasMaos, meusChutes));
    }

    /** Conta na narração o que mudou: rodada revelada, alguém saiu ganhando, fim do jogo. */
    private void narrarNovidades() {
        Mesa.Resultado r = mesa.getUltimoResultado();
        if (r != null && r.rodada + 1 > rodadasNarradas) {
            rodadasNarradas = r.rodada + 1;
            // Uma linha por jogador, com os nomes alinhados em coluna.
            int largura = 0;
            for (String id : r.maos.keySet()) {
                largura = Math.max(largura, nomeCurto(id).length());
            }
            StringBuilder texto = new StringBuilder("Rodada " + (r.rodada + 1) + " revelada\n");
            for (Map.Entry<String, Integer> e : r.maos.entrySet()) {
                texto.append("    ").append(String.format("%-" + largura + "s", nomeCurto(e.getKey())))
                        .append("   botou ").append(e.getValue())
                        .append(" · chutou ").append(r.chutes.get(e.getKey())).append('\n');
            }
            texto.append("    Total: ").append(r.total).append(" → ");
            if (r.acertou != null) {
                texto.append(nomeCurto(r.acertou)).append(" acertou e descartou um palito!");
            } else {
                texto.append("ninguém acertou.");
            }
            texto.append('\n'); // linha em branco separando as rodadas
            narracao.accept(texto.toString());
        }
        List<String> ganhadores = mesa.getGanhadores();
        while (ganhadoresNarrados < ganhadores.size()) {
            narracao.accept(nomeCurto(ganhadores.get(ganhadoresNarrados)) + " ficou sem palitos e saiu ganhando!");
            ganhadoresNarrados++;
        }
        if (mesa.getFase() == Mesa.Fase.FIM && !fimNarrado) {
            fimNarrado = true;
            narracao.accept("Fim de jogo! " + nomeCurto(mesa.getPerdedor()) + " perdeu e paga a rodada.");
        }
    }

    private String nomeCurto(String id) {
        if (meuId.equals(id)) {
            return "Você";
        }
        return mesa != null ? mesa.nome(id) : participantes.getOrDefault(id, "?");
    }

    private void limpar() {
        mesaId = null;
        donoNome = null;
        souDono = false;
        comecou = false;
        pediEntrada = false;
        participantes.clear();
        mesa = null;
        minhasMaos.clear();
        meusChutes.clear();
        rodadasNarradas = 0;
        ganhadoresNarrados = 0;
        fimNarrado = false;
    }
}
