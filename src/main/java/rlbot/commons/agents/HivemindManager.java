package rlbot.commons.agents;

import rlbot.flat.ControllerStateT;
import rlbot.flat.MatchCommT;
import rlbot.flat.PlayerInputT;
import rlbot.flat.SetLoadoutT;
import rlbot.commons.protocol.RLBotInterface;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A manager for RLBot hiveminds, handling hivemind lifecycle including initialization, packet reading,
 * and retirement on disconnect.
 * The manager must be constructed with an {@link RLBotInterface}, a default agent ID, and a {@link HivemindFactory}
 * that is used to initialize the hivemind instance once all the required information has arrived.
 * <p>
 * The hivemind is a single object handled by a single thread, allowing for custom orchestration
 * of the hivemind's bots.
 * Use the {@link BotManager} if you simply want one thread per car.
 * <p>
 * Example usage:
 * <pre>
 *     {@code
 *         var rlbot = new RLBotInterface();
 *         var hivemindManager = new HivemindManager(rlbot, "myname/examplebot/v0.1", ExampleHivemind::new);
 *         hivemindManager.run();
 *     }
 * </pre>
 */
public class HivemindManager extends AgentBaseManager {

    private final HivemindFactory hivemindFactory;
    private Hivemind hivemind;
    private List<Integer> indices;
    private int team;

    /**
     * Construct a HivemindManager. The manager must be constructed with an {@link RLBotInterface},
     * a default agent ID that should match the player configuration agent ID, and a {@link HivemindFactory}
     * that is used to initialize instantiate the hivemind once all the required information has arrived.
     * @param rlbot an {@link RLBotInterface} connection to use.
     * @param defaultAgentId a default agent ID used to identify what agent this connection controls if the process
     *                       is not started by RLBot. It should match the agent ID in the player configuration.
     * @param hivemindFactory a {@link HivemindFactory} to instantiate the {@link Hivemind}.
     */
    public HivemindManager(RLBotInterface rlbot, String defaultAgentId, HivemindFactory hivemindFactory) {
        super(rlbot, defaultAgentId);
        this.hivemindFactory = hivemindFactory;
    }

    @Override
    void initialize() {
        // Create hivemind
        var playerConfs = getMatchConfig().getPlayerConfigurations();
        var info = getTeamInfo();
        team = (int) info.getTeam();
        indices = Arrays.stream(info.getControllables()).map(a -> (int) a.getIndex()).toList();
        var names = indices.stream().collect(Collectors.toMap(i -> i, i -> playerConfs[i].getVariety().asCustomBot().getName()));

        hivemind = hivemindFactory.create(getRlbotInterface(), indices, team, names, getAgentId(), getMatchConfig(), getFieldInfo());

        // Set their loadouts
        var loadouts = hivemind.getInitialLoadouts();
        if (loadouts != null) {
            for (var indexLoadout : loadouts.entrySet()) {
                if (indexLoadout.getValue() == null) {
                    continue;
                }
                var cmd = new SetLoadoutT();
                cmd.setIndex(indexLoadout.getKey());
                cmd.setLoadout(indexLoadout.getValue());
                getRlbotInterface().sendSetLoadout(cmd);
            }
        }
    }

    @Override
    protected void processPacket() {
        if (hivemind == null) {
            // We have not initialized yet
            return;
        }
        Map<Integer, ControllerStateT> controllers;
        try {
            controllers = hivemind.getOutputs(latestGamePacket, latestBallPrediction);
        } catch (Exception e) {
            logger.severe("Hivemind '" + getAgentId() + "' (team " + team + ") encountered an error while processing game packet: " + e.getMessage());
            return;
        }

        if (controllers != null) {
            for (var indexController : controllers.entrySet()) {
                if (indexController.getValue() == null) {
                    continue;
                }
                var input = new PlayerInputT();
                input.setPlayerIndex(indexController.getKey());
                input.setControllerState(indexController.getValue());
                getRlbotInterface().sendPlayerInput(input);
            }
        }
    }

    @Override
    protected void retire() {
        if (hivemind != null) {
            hivemind.onRetire();
        }
    }

    @Override
    public void onMatchComms(MatchCommT comm) {
        if (hivemind == null) {
            // We have not initialized yet
            return;
        }
        try {
            hivemind.onMatchCommReceived(comm);
        } catch (RuntimeException e) {
            logger.severe("Hivemind '" + getAgentId() + "' (team " + team + ") encountered an error while processing match comms: " + e.getMessage());
        }
    }
}
