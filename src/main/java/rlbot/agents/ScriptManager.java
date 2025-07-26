package rlbot.agents;

import rlbot.flat.MatchCommT;
import rlbot.protocol.RLBotInterface;

public class ScriptManager extends AgentBaseManager {

    private final ScriptFactory scriptFactory;
    private Script script;
    private int index;
    private String name;

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
