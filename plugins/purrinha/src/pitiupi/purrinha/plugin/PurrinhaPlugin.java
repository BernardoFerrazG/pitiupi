package pitiupi.purrinha.plugin;

import pitiupi.api.plugin.PeerActionsRegistrar;
import pitiupi.api.plugin.PluginWorkspace;
import pitiupi.application.messaging.MessageDispatcher;
import pitiupi.application.messaging.SendTarget;
import pitiupi.application.peeractions.PeerAction;
import pitiupi.application.peeractions.PeerActionTarget;
import pitiupi.control.ConnectionManager;
import pitiupi.models.Messages.Message;
import pitiupi.models.Plugin;
import pitiupi.purrinha.GUI.PurrinhaPanel;
import pitiupi.purrinha.control.PurrinhaController;
import pitiupi.purrinha.net.JogadaMessage;
import pitiupi.purrinha.net.MesaMessage;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.List;

/**
 * Purrinha (palitinho) para até 4 pessoas da sala.
 */
public class PurrinhaPlugin extends Plugin {

    private PluginWorkspace workspace;
    private PurrinhaController controller;
    private PurrinhaPanel panel;

    @Override
    public String getName() {
        return "Purrinha";
    }

    @Override
    public String getDisplayName() {
        return "Purrinha";
    }

    @Override
    public String getAuthor() {
        return "Bernardo";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getIdentifier() {
        return "pitiupi.purrinha";
    }

    @Override
    public void createPlugin(ConnectionManager connectionManager, PluginWorkspace workspace) {
        this.workspace = workspace;
        String meuId = connectionManager.getHeartbeatManager().getInstanceId();
        this.controller = new PurrinhaController(
                meuId,
                connectionManager::getUserName,
                message -> MessageDispatcher.send(
                        connectionManager,
                        message,
                        SendTarget.broadcastAll(),
                        panel.transporteEscolhido()
                )
        );
        this.panel = new PurrinhaPanel(controller);
        bindShellWorkspaceContent(panel);
        installPeerActions(workspace.peerActions());

        // A cada segundo: reenvia o estado (UDP pode perder pacote) e atualiza a tela.
        new Timer(1000, e -> {
            controller.aCadaSegundo();
            panel.atualizar();
        }).start();
    }

    @Override
    public void installPeerActions(PeerActionsRegistrar registrar) {
        registrar.register(PeerAction.of(
                "purrinha.room",
                "Abrir a purrinha",
                PeerActionTarget.ALL_PEERS,
                target -> workspace.openOrFocusTab(getIdentifier(), getDisplayName())
        ));
    }

    @Override
    public List<Class<? extends Message>> getMessages() {
        return List.of(MesaMessage.class, JogadaMessage.class);
    }

    @Override
    public void receiveMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            if (message instanceof MesaMessage mesa) {
                controller.receberMesa(mesa);
            } else if (message instanceof JogadaMessage jogada) {
                controller.receberJogada(jogada);
            }
            panel.atualizar();
        });
    }
}
