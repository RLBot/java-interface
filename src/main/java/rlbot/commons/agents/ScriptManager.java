package rlbot.commons.agents;

import rlbot.flat.MatchCommT;
import rlbot.commons.protocol.RLBotInterface;

/**
 * A manager for an RLBot script, handling the script's lifecycle including initialization, packet reading,
 * and retirement on disconnect.
 * The manager must be constructed with an {@link RLBotInterface}, a default agent ID, and a {@link ScriptFactory}
 * that is used to initialize the script once all the required information has arrived.
 * <p>
 * A script is a car-less observer of a match.
 * It can also do debug rendering, send match comms, and do state-setting.
 * <p>
 * Example usage:
 * <pre>
 *     {@code
 *         var rlbot = new RLBotInterface();
 *         var scriptManager = new ScriptManager(rlbot, "myname/examplescript/v0.1", ExampleScript::new);
 *         scriptManager.run();
 *     }
 * </pre>
 *
 * @see BotManager
 */
public class ScriptManager extends AgentBaseManager {

    private final ScriptFactory scriptFactory;
    private Script script;
    private int index;
    private String name;

    /**
     * Construct a ScriptManager. The manager must be constructed with an {@link RLBotInterface},
     * a default agent ID that should match the player configuration agent ID, and a {@link ScriptFactory}
     * that is used to instantiate the Script once all the required information has arrived.
     * @param rlbot an {@link RLBotInterface} connection to use.
     * @param defaultAgentId a default agent ID used to identify what agent this connection controls if the process
     *                       is not started by RLBot. It should match the agent ID in the player configuration.
     * @param scriptFactory a {@link ScriptFactory} to instantiate the {@link Script}.
     */
    public ScriptManager(RLBotInterface rlbot, String defaultAgentId, ScriptFactory scriptFactory) {
        super(rlbot, defaultAgentId);
        this.scriptFactory = scriptFactory;
    }

    @Override
    void initialize() {
        var agent = getTeamInfo().getControllables()[0];
        index = (int) agent.getIndex();
        name = getMatchConfig().getScriptConfigurations()[index].getName();

        script = scriptFactory.create(getRlbotInterface(), index, getAgentId(), getMatchConfig(), getFieldInfo());
    }

    @Override
    protected void processPacket() {
        if (script == null) {
            return;
        }
        try {
            script.process(latestGamePacket, latestBallPrediction);
        } catch (Exception e) {
            logger.severe(name + " encountered an error while processing game packet: " + e.getMessage());
        }
    }

    @Override
    protected void retire() {
        if (script != null) {
            script.onRetire();
        }
    }

    @Override
    public void onMatchComms(MatchCommT comm) {
        if (script == null) {
            return;
        }
        try {
            script.onMatchCommReceived(comm);
        } catch (Exception e) {
            logger.severe(name + " encountered an error while processing match comms: " + e.getMessage());
        }
    }
}
