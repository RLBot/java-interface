package rlbot.commons.protocol;

import rlbot.flat.*;

/**
 * The RLBotListener interface defines methods for reacting to various events and messages
 * from the {@link RLBotInterface}.
 *
 * @see RLBotInterface
 * @see RLBotListenerAdapter
 */
public interface RLBotListener {

    /**
     * Invoked when a connection to the {@link RLBotInterface} is established.
     */
    void onConnect();

    /**
     * Invoked when the connection to the {@link RLBotInterface} is closed.
     */
    void onDisconnect();

    /**
     * Invoked whenever any message is received from the {@link RLBotInterface}.
     *
     * @param packet The incoming CorePacket object containing the message data.
     */
    void onAnyMessage(CorePacketT packet);

    /**
     * Invoked whenever a game packet is received.
     * This packet contains various dynamic game state information, such as player data,
     * boost pad states, ball positions, match information, and scoreline.
     *
     * @param packet The received {@link GamePacketT}.
     */
    void onGamePacket(GamePacketT packet);

    /**
     * Invoked when field information is received. This provides static details about
     * the field, such as the locations and properties of boost pads and goals.
     *
     * @param fieldInfo The received {@link FieldInfoT}.
     */
    void onFieldInfo(FieldInfoT fieldInfo);

    /**
     * Invoked when a match configuration is received. This provides information
     * about the current or upcoming match, including settings like game mode, map,
     * player configurations, and mutators.
     *
     * @param config The received {@link MatchConfigurationT}.
     */
    void onMatchConfig(MatchConfigurationT config);

    /**
     * Invoked when a match communication message is received.
     * These messages contain an arbitrary byte string, a human-readable quick chat message,
     * and can be general or private to the team. See the index and team field to determine the sender.
     *
     * @param comm The received {@link MatchCommT}.
     */
    void onMatchComms(MatchCommT comm);

    /**
     * Invoked when a ball prediction is received. The prediction describes the ball's future
     * positions and velocities over the next few seconds at 120 Hz.
     *
     * @param prediction The received {@link BallPredictionT}.
     */
    void onBallPrediction(BallPredictionT prediction);

    /**
     * Invoked when controllable team information is received. This message is sent as part of
     * initialization to inform about what bots or scripts are controlled by this agent.
     *
     * @param teamInfo The received {@link ControllableTeamInfoT}.
     */
    void onControllableTeamInfo(ControllableTeamInfoT teamInfo);

    /**
     * Invoked when this agent's ability to debug render changes.
     *
     * @param status The received {@link RenderingStatusT} with the new state.
     */
    void onRenderingStatus(RenderingStatusT status);
}
