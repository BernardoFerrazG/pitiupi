package pitiupi.purrinha.GUI;

import pitiupi.application.messaging.PluginTransportCatalog;
import pitiupi.application.messaging.TransportMode;
import pitiupi.presentation.shell.PluginRoutingPanel;
import pitiupi.presentation.shell.PluginSessionKeys;
import pitiupi.presentation.shell.PluginSessionWorkspace;
import pitiupi.presentation.shell.SessionTabBar;
import pitiupi.presentation.shell.ui.ShellUi;
import pitiupi.purrinha.control.Mesa;
import pitiupi.purrinha.control.PurrinhaController;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Tela da purrinha, no mesmo formato do chat: "Destino e transporte" em cima,
 * barra de botões, aba de sessão "Mesa" e o corpo com a mesa, a narração e as jogadas.
 */
public class PurrinhaPanel extends JPanel {

    private final PurrinhaController controller;
    private final PluginRoutingPanel routingPanel;
    private final PluginSessionWorkspace workspace;
    private final SessionTabBar sessionTabs;

    private final JButton abrirButton = new JButton("Abrir mesa");
    private final JButton entrarButton = new JButton("Entrar");
    private final JButton comecarButton = new JButton("Começar");
    private final JButton encerrarButton = new JButton("Encerrar mesa");

    private final JEditorPane situacao = new JEditorPane();
    private final JTextArea narracao = new JTextArea();
    private final JLabel aviso = ShellUi.mutedLabel(" ");
    private final JButton[] botarButtons = new JButton[Mesa.PALITOS_INICIAIS + 1];
    private final SpinnerNumberModel chuteModel = new SpinnerNumberModel(0, 0, 99, 1);
    private final JSpinner chuteSpinner = new JSpinner(chuteModel);
    private final JButton chutarButton = new JButton("Chutar");
    private final SimpleDateFormat hora = new SimpleDateFormat("HH:mm:ss");
    private String ultimaSituacao = "";

    public PurrinhaPanel(PurrinhaController controller) {
        this.controller = controller;
        this.routingPanel = new PluginRoutingPanel(
                PluginTransportCatalog.PluginKind.PING,
                "<html><i>A purrinha é sempre com a sala inteira.</i></html>",
                false
        );
        this.workspace = new PluginSessionWorkspace(routingPanel, buildToolbar());
        this.sessionTabs = workspace.getSessionTabBar();
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        buildUi();
        initSessionTabs();
        controller.setNarracao(this::narrar);
        atualizar();
    }

    /** Transporte escolhido no combo (multicast ou uma cópia unicast para cada um). */
    public TransportMode transporteEscolhido() {
        TransportMode mode = routingPanel.snapshot().transport();
        return mode != null ? mode : TransportMode.MULTICAST_UDP;
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        for (JButton button : new JButton[]{abrirButton, entrarButton, comecarButton, encerrarButton}) {
            button.setFocusPainted(false);
            bar.add(button);
        }
        abrirButton.addActionListener(e -> mostrar(controller.abrirMesa()));
        entrarButton.addActionListener(e -> mostrar(controller.entrar()));
        comecarButton.addActionListener(e -> mostrar(controller.comecar()));
        encerrarButton.addActionListener(e -> mostrar(controller.encerrar()));
        return bar;
    }

    private void initSessionTabs() {
        sessionTabs.addOrSelectTab(PluginSessionKeys.ROOM, "Mesa", "Mesa da purrinha (Todos)", new JPanel(), false);
        sessionTabs.setCloseFallbackKey(PluginSessionKeys.ROOM);
        sessionTabs.getAddPeerButton().setVisible(false);
    }

    private void buildUi() {
        situacao.setContentType("text/html");
        situacao.setEditable(false);
        narracao.setEditable(false);
        narracao.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        narracao.setLineWrap(true);
        narracao.setWrapStyleWord(true);

        JSplitPane topo = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(situacao), new JScrollPane(narracao));
        topo.setResizeWeight(0.55);

        JPanel botar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        botar.add(new JLabel("Botar palitos:"));
        for (int i = 0; i < botarButtons.length; i++) {
            int quantos = i;
            botarButtons[i] = new JButton(String.valueOf(i));
            botarButtons[i].setFocusPainted(false);
            botarButtons[i].addActionListener(e -> mostrar(controller.botar(quantos)));
            botar.add(botarButtons[i]);
        }

        chuteSpinner.setPreferredSize(new Dimension(60, chuteSpinner.getPreferredSize().height));
        chutarButton.setFocusPainted(false);
        chutarButton.addActionListener(e -> mostrar(controller.chutar(((Number) chuteModel.getValue()).intValue())));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.add(new JLabel("Chute (total):"));
        actions.add(chuteSpinner);
        actions.add(chutarButton);

        JPanel compose = new JPanel(new BorderLayout(0, 4));
        compose.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        compose.add(aviso, BorderLayout.NORTH);
        compose.add(botar, BorderLayout.CENTER);
        compose.add(actions, BorderLayout.SOUTH);

        // Os botões ficam fixos no rodapé: aparecem inteiros em qualquer altura de janela.
        JPanel corpo = new JPanel(new BorderLayout());
        corpo.add(topo, BorderLayout.CENTER);
        corpo.add(compose, BorderLayout.SOUTH);
        workspace.getBodyHost().add(corpo, BorderLayout.CENTER);
        add(workspace, BorderLayout.CENTER);
    }

    private void mostrar(String erro) {
        aviso.setText(erro == null ? " " : erro);
        atualizar();
    }

    private void narrar(String texto) {
        narracao.append("[" + hora.format(new Date()) + "] " + texto + "\n");
        narracao.setCaretPosition(narracao.getDocument().getLength());
    }

    /** Atualiza botões e o quadro da mesa. O plugin chama a cada segundo e depois de cada mensagem. */
    public void atualizar() {
        Mesa mesa = controller.getMesa();
        String eu = controller.getMeuId();
        boolean temMesa = controller.getMesaId() != null;
        boolean fim = mesa != null && mesa.getFase() == Mesa.Fase.FIM;

        abrirButton.setEnabled(!temMesa || fim);
        entrarButton.setEnabled(temMesa && !controller.isComecou() && !controller.isSouDono()
                && !controller.getParticipantes().containsKey(eu));
        comecarButton.setEnabled(controller.isSouDono() && !controller.isComecou()
                && controller.getParticipantes().size() >= 2);
        encerrarButton.setEnabled(controller.isSouDono());

        for (int i = 0; i < botarButtons.length; i++) {
            botarButtons[i].setEnabled(mesa != null && mesa.podeBotar(eu, i));
        }
        boolean minhaVez = mesa != null && mesa.getFase() == Mesa.Fase.CHUTAR && eu.equals(mesa.getVez());
        chutarButton.setEnabled(minhaVez);
        chuteSpinner.setEnabled(minhaVez);
        if (mesa != null) {
            int maximo = mesa.maiorChutePossivel();
            chuteModel.setMaximum(maximo);
            if (((Number) chuteModel.getValue()).intValue() > maximo) {
                chuteModel.setValue(maximo);
            }
        }

        String html = montarSituacao(mesa, eu);
        if (!html.equals(ultimaSituacao)) {
            situacao.setText(html);
            situacao.setCaretPosition(0);
            ultimaSituacao = html;
        }
    }

    private String montarSituacao(Mesa mesa, String eu) {
        StringBuilder h = new StringBuilder("<html><body style='font-family:sans-serif'>");
        if (controller.getMesaId() == null) {
            h.append("<p><b>Nenhuma mesa aberta.</b> Clique em <b>Abrir mesa</b> ou espere alguém abrir.</p>");
            return h.append("</body></html>").toString();
        }
        String titulo = controller.isSouDono() ? "Sua mesa" : "Mesa de " + escape(controller.getDonoNome());
        if (mesa == null) {
            h.append("<p><b>").append(titulo).append("</b> · esperando jogadores (")
                    .append(controller.getParticipantes().size()).append("/").append(Mesa.MAX_JOGADORES).append(")</p><ul>");
            for (Map.Entry<String, String> p : controller.getParticipantes().entrySet()) {
                h.append("<li>").append(escape(p.getValue())).append(eu.equals(p.getKey()) ? " (você)" : "").append("</li>");
            }
            return h.append("</ul></body></html>").toString();
        }

        h.append("<p><b>").append(titulo).append("</b> · ");
        switch (mesa.getFase()) {
            case BOTAR -> h.append("Rodada ").append(mesa.getRodada() + 1).append(": todo mundo bota os palitos");
            case CHUTAR -> h.append("Rodada ").append(mesa.getRodada() + 1).append(": vez de <b>")
                    .append(nomeDe(mesa, mesa.getVez(), eu)).append("</b> chutar");
            case FIM -> h.append("<b>Fim de jogo! ").append(nomeDe(mesa, mesa.getPerdedor(), eu))
                    .append(" paga a rodada.</b>");
        }
        h.append("</p><table cellpadding='3'><tr><th align='left'>Jogador</th><th>Palitos</th>"
                + "<th>Botou?</th><th>Chute</th></tr>");
        Map<String, Integer> chutes = mesa.chutesDaRodada();
        for (String id : mesa.getOrdem()) {
            h.append("<tr><td>").append(id.equals(mesa.getVez()) ? "▶ " : "").append(nomeDe(mesa, id, eu)).append("</td>");
            if (!mesa.estaJogando(id)) {
                h.append("<td align='center'>0</td><td colspan='2'>saiu ganhando</td></tr>");
                continue;
            }
            h.append("<td align='center'>").append(mesa.palitosDe(id)).append("</td>");
            String botou;
            if (mesa.getFase() == Mesa.Fase.FIM) {
                botou = "—";
            } else if (id.equals(eu) && mesa.jaBotou(id)) {
                botou = "sim (" + mesa.maosDe(id).get(mesa.getRodada()) + ")";
            } else {
                botou = mesa.jaBotou(id) ? "sim" : "…";
            }
            h.append("<td align='center'>").append(botou).append("</td>");
            h.append("<td align='center'>").append(chutes.containsKey(id) ? chutes.get(id) : "—").append("</td></tr>");
        }
        h.append("</table>");

        Mesa.Resultado r = mesa.getUltimoResultado();
        if (r != null) {
            h.append("<p><i>Rodada ").append(r.rodada + 1).append(": ");
            boolean primeiro = true;
            for (Map.Entry<String, Integer> e : r.maos.entrySet()) {
                h.append(primeiro ? "" : ", ").append(nomeDe(mesa, e.getKey(), eu)).append(" botou ").append(e.getValue());
                primeiro = false;
            }
            h.append(" → total ").append(r.total).append(". ");
            h.append(r.acertou != null ? nomeDe(mesa, r.acertou, eu) + " acertou." : "Ninguém acertou.");
            h.append("</i></p>");
        }
        return h.append("</body></html>").toString();
    }

    private static String nomeDe(Mesa mesa, String id, String eu) {
        if (id == null) {
            return "?";
        }
        return escape(mesa.nome(id)) + (id.equals(eu) ? " (você)" : "");
    }

    private static String escape(String texto) {
        return texto == null ? "" : texto.replace("&", "&amp;").replace("<", "&lt;");
    }
}
