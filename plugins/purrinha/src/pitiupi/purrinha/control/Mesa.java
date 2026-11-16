package pitiupi.purrinha.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Regras da purrinha.
 *
 * <p>Recebe a ordem dos jogadores e o que cada um fez (mãos e chutes de cada rodada) e
 * calcula a situação do jogo do zero. Todo computador tem os mesmos dados, então todo
 * computador chega na mesma situação, sem precisar de juiz.</p>
 *
 * <p>Regras: cada um começa com 3 palitos. Na rodada, cada um bota de 0 até os palitos que
 * tem. Depois cada um chuta o total, na sua vez, sem repetir chute. Quem acerta descarta
 * um palito. Quem fica sem palitos sai ganhando. O último que sobrar perde.</p>
 */
public class Mesa {

    public static final int PALITOS_INICIAIS = 3;
    public static final int MAX_JOGADORES = 4;

    public enum Fase {
        BOTAR,
        CHUTAR,
        FIM
    }

    /** Como terminou uma rodada, para mostrar na tela. */
    public static class Resultado {
        public int rodada;
        public Map<String, Integer> maos = new LinkedHashMap<>();
        public Map<String, Integer> chutes = new LinkedHashMap<>();
        public int total;
        public String acertou;
    }

    private final List<String> ordem = new ArrayList<>();
    private final Map<String, String> nomes = new HashMap<>();
    private final Map<String, List<Integer>> maos = new HashMap<>();
    private final Map<String, List<Integer>> chutes = new HashMap<>();

    private int rodada;
    private Fase fase = Fase.BOTAR;
    private String vez;
    private final Map<String, Integer> palitos = new HashMap<>();
    private final List<String> ativos = new ArrayList<>();
    private final List<String> ordemDaVez = new ArrayList<>();
    private final List<String> ganhadores = new ArrayList<>();
    private String perdedor;
    private Resultado ultimoResultado;

    public Mesa(List<String> ids, List<String> nomesDosJogadores) {
        for (int i = 0; i < ids.size(); i++) {
            ordem.add(ids.get(i));
            nomes.put(ids.get(i), nomesDosJogadores.get(i));
            maos.put(ids.get(i), new ArrayList<>());
            chutes.put(ids.get(i), new ArrayList<>());
        }
        calcular();
    }

    /**
     * Guarda o que um jogador fez. Só aceita listas iguais ou maiores que as que já temos:
     * um pacote velho que chegou atrasado não apaga jogada nova.
     */
    public void atualizarJogador(String id, List<Integer> maosDele, List<Integer> chutesDele) {
        if (!ordem.contains(id)) {
            return;
        }
        if (maosDele.size() >= maos.get(id).size()) {
            maos.put(id, new ArrayList<>(maosDele));
        }
        if (chutesDele.size() >= chutes.get(id).size()) {
            chutes.put(id, new ArrayList<>(chutesDele));
        }
        calcular();
    }

    /** Refaz a partida inteira, rodada por rodada, a partir das jogadas. */
    private void calcular() {
        palitos.clear();
        ativos.clear();
        ganhadores.clear();
        ordemDaVez.clear();
        perdedor = null;
        vez = null;
        ultimoResultado = null;
        for (String id : ordem) {
            palitos.put(id, PALITOS_INICIAIS);
            ativos.add(id);
        }

        for (int r = 0; ; r++) {
            rodada = r;
            if (ativos.size() <= 1) {
                fase = Fase.FIM;
                perdedor = ativos.isEmpty() ? null : ativos.get(0);
                return;
            }

            // Quem começa a chutar muda a cada rodada.
            ordemDaVez.clear();
            int comeca = r % ativos.size();
            for (int i = 0; i < ativos.size(); i++) {
                ordemDaVez.add(ativos.get((comeca + i) % ativos.size()));
            }

            // Fase 1: todo mundo bota.
            for (String id : ativos) {
                if (maos.get(id).size() <= r) {
                    fase = Fase.BOTAR;
                    return;
                }
            }

            // Fase 2: cada um chuta na sua vez.
            for (String id : ordemDaVez) {
                if (chutes.get(id).size() <= r) {
                    fase = Fase.CHUTAR;
                    vez = id;
                    return;
                }
            }

            // Fase 3: revela.
            Resultado resultado = new Resultado();
            resultado.rodada = r;
            for (String id : ordemDaVez) {
                int mao = maos.get(id).get(r);
                resultado.maos.put(id, mao);
                resultado.chutes.put(id, chutes.get(id).get(r));
                resultado.total += mao;
            }
            for (String id : ordemDaVez) {
                if (resultado.chutes.get(id) == resultado.total) {
                    resultado.acertou = id;
                    break;
                }
            }
            if (resultado.acertou != null) {
                int sobrou = palitos.get(resultado.acertou) - 1;
                palitos.put(resultado.acertou, sobrou);
                if (sobrou == 0) {
                    ativos.remove(resultado.acertou);
                    ganhadores.add(resultado.acertou);
                }
            }
            ultimoResultado = resultado;
        }
    }

    public boolean podeBotar(String id, int quantos) {
        return fase == Fase.BOTAR
                && ativos.contains(id)
                && maos.get(id).size() == rodada
                && quantos >= 0
                && quantos <= palitos.get(id);
    }

    /** retorna nulo se pode chutar, senão retorna o motivo */
    public String motivoParaNaoChutar(String id, int chute) {
        if (fase != Fase.CHUTAR || !id.equals(vez)) {
            return "Não é a sua vez de chutar.";
        }
        if (chute < 0 || chute > maiorChutePossivel()) {
            return "O chute vai de 0 a " + maiorChutePossivel() + ".";
        }
        if (chutesDaRodada().containsValue(chute)) {
            return "Alguém já chutou " + chute + ". Escolha outro número.";
        }
        return null;
    }

    public int maiorChutePossivel() {
        int soma = 0;
        for (String id : ativos) {
            soma += palitos.get(id);
        }
        return soma;
    }

    /** Chutes já feitos na rodada atual, na ordem da vez. */
    public Map<String, Integer> chutesDaRodada() {
        Map<String, Integer> feitos = new LinkedHashMap<>();
        for (String id : ordemDaVez) {
            if (chutes.get(id).size() > rodada) {
                feitos.put(id, chutes.get(id).get(rodada));
            }
        }
        return feitos;
    }

    public boolean jaBotou(String id) {
        return maos.containsKey(id) && maos.get(id).size() > rodada;
    }

    public List<Integer> maosDe(String id) {
        return maos.getOrDefault(id, List.of());
    }

    public List<String> getOrdem() {
        return ordem;
    }

    public String nome(String id) {
        return nomes.getOrDefault(id, "?");
    }

    public int palitosDe(String id) {
        return palitos.getOrDefault(id, 0);
    }

    public boolean estaJogando(String id) {
        return ativos.contains(id);
    }

    public int getRodada() {
        return rodada;
    }

    public Fase getFase() {
        return fase;
    }

    public String getVez() {
        return vez;
    }

    public List<String> getGanhadores() {
        return ganhadores;
    }

    public String getPerdedor() {
        return perdedor;
    }

    public Resultado getUltimoResultado() {
        return ultimoResultado;
    }
}
