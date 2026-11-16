package pitiupi.purrinha.net;

import pitiupi.models.Messages.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Mandada por quem abriu a mesa, a cada segundo: quem está na mesa e se o jogo já começou.
 */
public class MesaMessage extends Message {

    private String mesaId;
    private String donoId;
    private String donoNome;
    private boolean comecou;
    private boolean encerrada;
    private ArrayList<String> ids;
    private ArrayList<String> nomes;

    public MesaMessage() {
    }

    public MesaMessage(String mesaId, String donoId, String donoNome, boolean comecou, boolean encerrada,
                       List<String> ids, List<String> nomes) {
        this.mesaId = mesaId;
        this.donoId = donoId;
        this.donoNome = donoNome;
        this.comecou = comecou;
        this.encerrada = encerrada;
        this.ids = new ArrayList<>(ids);
        this.nomes = new ArrayList<>(nomes);
    }

    public String getMesaId() {
        return mesaId;
    }

    public String getDonoId() {
        return donoId;
    }

    public String getDonoNome() {
        return donoNome;
    }

    public boolean isComecou() {
        return comecou;
    }

    public boolean isEncerrada() {
        return encerrada;
    }

    public List<String> getIds() {
        return ids;
    }

    public List<String> getNomes() {
        return nomes;
    }
}
