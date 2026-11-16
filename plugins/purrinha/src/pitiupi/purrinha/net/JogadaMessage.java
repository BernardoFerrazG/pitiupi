package pitiupi.purrinha.net;

import pitiupi.models.Messages.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Mandada por cada jogador, a cada segundo: tudo o que ele já fez na partida.
 *
 * <p>{@code maos.get(r)} é quantos palitos ele botou na rodada r, e {@code chutes.get(r)} o chute.
 * Mandar o histórico inteiro resolve a perda de pacote do UDP: se uma mensagem se perde,
 * a próxima traz tudo de novo. Com as listas vazias, a mensagem é só um pedido para entrar na mesa.</p>
 */
public class JogadaMessage extends Message {

    private String mesaId;
    private String jogadorId;
    private String nome;
    private ArrayList<Integer> maos;
    private ArrayList<Integer> chutes;

    public JogadaMessage() {
    }

    public JogadaMessage(String mesaId, String jogadorId, String nome, List<Integer> maos, List<Integer> chutes) {
        this.mesaId = mesaId;
        this.jogadorId = jogadorId;
        this.nome = nome;
        this.maos = new ArrayList<>(maos);
        this.chutes = new ArrayList<>(chutes);
    }

    public String getMesaId() {
        return mesaId;
    }

    public String getJogadorId() {
        return jogadorId;
    }

    public String getNome() {
        return nome;
    }

    public List<Integer> getMaos() {
        return maos;
    }

    public List<Integer> getChutes() {
        return chutes;
    }
}
