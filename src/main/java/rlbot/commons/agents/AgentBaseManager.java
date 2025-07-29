package rlbot.commons.agents;

import rlbot.flat.*;
import rlbot.commons.protocol.RLBotListenerAdapter;
import rlbot.commons.protocol.RLBotInterface;

import java.util.logging.Logger;

public abstract class AgentBaseManager extends RLBotListenerAdapter {

    protected final Logger logger = Logger.getLogger(AgentBaseManager.class.getName());

    private final RLBotInterface rlbot;
    private final String agentId;

    private ControllableTeamInfoT teamInfo;
    private MatchConfigurationT matchConfig;
    private FieldInfoT fieldInfo;
    private boolean initialized = false;

    protected GamePacketT latestGamePacket;
    protected BallPredictionT latestBallPrediction;

    public AgentBaseManager(RLBotInterface rlbot, String defaultAgentId) {

        String agentId = System.getenv("RLBOT_AGENT_ID");

        this.rlbot = rlbot;
        this.agentId = agentId == null ? defaultAgentId : agentId;

        if (this.agentId == null) {
            throw new RuntimeException("Environment variable RLBOT_AGENT_ID is not set and no default agent id is passed to the BotManager.");
        }

        rlbot.addListener(this);
    }

    private void tryInitialize() {
        if (initialized || matchConfig == null || fieldInfo == null || teamInfo == null) {
            return;
        }

        try {
            logger.info("Initializing agent: " + agentId);
            initialize();
        } catch (Exception e) {
            logger.severe("Failed to initialize agent: " + agentId);
            e.printStackTrace();
            return;
        }

        rlbot.sendInitComplete();
        initialized = true;
    }

    abstract void initialize();

    @Override
    public void onGamePacket(GamePacketT packet) {
        latestGamePacket = packet;
    }

    @Override
    public void onFieldInfo(FieldInfoT fieldInfo) {
        this.fieldInfo = fieldInfo;
        logger.info("Received field info!");
        tryInitialize();
    }

    @Override
    public void onMatchConfig(MatchConfigurationT config) {
        this.matchConfig = config;
        logger.info("Received match config!");
        tryInitialize();
    }

    @Override
    public void onBallPrediction(BallPredictionT prediction) {
        this.latestBallPrediction = prediction;
    }

    @Override
    public void onControllableTeamInfo(ControllableTeamInfoT teamInfo) {
        this.teamInfo = teamInfo;
        logger.info("Received team info!");
        tryInitialize();
    }

    public void run() {
        run(true, true);
    }

    public void run(boolean wantsBallPrediction, boolean wantsComms) {
        rlbot.connect(agentId, wantsBallPrediction, wantsComms, false);

        try {
            while (true) {
                var res = rlbot.handleNextIncomingMsg(latestGamePacket == null);

                switch (res) {
                    case Termination:
                        return;
                    case MoreMsgsQueued:
                        continue;
                    case NoIncomingMsgs:
                        if (latestGamePacket != null) {
                            processPacket();
                            latestGamePacket = null;
                        }
                }
            }
        } catch (Exception e) {
            logger.severe("An error occurred while handling a game packet.");
            e.printStackTrace();
        } finally {
            retire();
        }
    }

    protected abstract void processPacket();

    protected abstract void retire();

    public ControllableTeamInfoT getTeamInfo() {
        return teamInfo;
    }

    public MatchConfigurationT getMatchConfig() {
        return matchConfig;
    }

    public FieldInfoT getFieldInfo() {
        return fieldInfo;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public String getAgentId() {
        return agentId;
    }

    public RLBotInterface getRlbotInterface() {
        return rlbot;
    }
}
