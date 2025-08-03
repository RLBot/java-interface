package rlbot.commons.agents;

import rlbot.flat.*;
import rlbot.commons.protocol.RLBotListenerAdapter;
import rlbot.commons.protocol.RLBotInterface;

import java.util.logging.Logger;

/**
 * The AgentBaseManager class is an abstract base class for managing agents in RLBot and
 * implements the common initialization and packet reading behavior. That is, the
 * AgentBaseManager will wait for the match configuration, field information, and team information
 * before initializing the agent(s) of this process. Once initialization is complete,
 * the AgentBaseManager handles messages from RLBot discarding any outdated game packets,
 * finally passing the latest packet and ball prediction to the implementer.
 *
 * @see BotManager
 * @see HivemindManager
 * @see ScriptManager
 */
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

    /**
     * AgentBaseManager constructor.
     *
     * @param rlbot the RLBotInterface instance used to interact with RLBot functionalities.
     * @param defaultAgentId the default agent ID to use if the "RLBOT_AGENT_ID" environment variable is not set.
     * @throws RuntimeException if the agent ID cannot be determined from the environment variable or the provided default.
     */
    public AgentBaseManager(RLBotInterface rlbot, String defaultAgentId) {

        String agentId = System.getenv("RLBOT_AGENT_ID");

        this.rlbot = rlbot;
        this.agentId = agentId == null ? defaultAgentId : agentId;

        if (this.agentId == null) {
            throw new RuntimeException("Environment variable RLBOT_AGENT_ID is not set and no default agent id is passed to the BotManager.");
        }

        rlbot.addListener(this);
    }

    /**
     * Try to initialize the agents of this manager if the required messages have been received, including
     * the match configuration, the field information, and the team information.
     * Once initialization is complete, an {@link InitComplete} is sent to RLBot.
     *
     */
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

    /**
     * Initialize the agent manager and its agents.
     * Implementers are expected to send {@link SetLoadout} defining bot(s) initial loadout, if relevant.
     */
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

    /**
     * Connects to RLBotServer and starts the agent manager's main loop, handling messages.
     * Once all required info has been received, the agent manager will initialize the
     * agent(s) of this process. Outdated game packets are discarded, while the latest packet
     * and ball prediction are continuously passed to the agent(s).
     * <p>
     * Use this if both ball prediction and match comms should be sent to this connection.
     * Use {@link AgentBaseManager#run(boolean, boolean)} otherwise.
     */
    public void run() {
        run(true, true);
    }

    /**
     * Connects to RLBotServer and starts the agent manager's main loop, handling messages.
     * Once all required info has been received, the agent manager will initialize the
     * agent(s) of this process. Outdated game packets are discarded, while the latest packet
     * and ball prediction are continuously passed to the agent(s).
     *
     * @param wantsBallPrediction whether ball prediction messages should be sent to this agent.
     * @param wantsComms whether match communication messages should be sent to this agent.
     */
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

    /**
     * Invoked when a new game packet is ready to be processed.
     * See {@link AgentBaseManager#latestGamePacket} and {@link AgentBaseManager#latestBallPrediction}.
     * Ball prediction may be {@code null} if the user did not request ball prediction messages.
     * Game packets can also arrive before initialization is complete.
     */
    protected abstract void processPacket();

    /**
     * Invoked when the connection is terminating.
     * Use this to dispose of resources.
     */
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
