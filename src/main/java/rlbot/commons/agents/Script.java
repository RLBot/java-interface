package rlbot.commons.agents;

import rlbot.flat.*;

/**
 * An interface for a script managed by a {@link ScriptManager}.
 * A script is a car-less observer of a match.
 * It can also do debug rendering, send match comms, and do state-setting.
 *
 * @see ScriptManager
 */
public interface Script {

    /**
     * Process the latest game state.
     * @param packet the latest game packet.
     * @param ballPrediction the latest ball prediction. May be null if ball prediction was not requested.
     */
    void process(GamePacketT packet, BallPredictionT ballPrediction);

    /**
     * Process an incoming {@link MatchComm} message.
     * See the index and team field to determine the sender.
     * @param comm the received message.
     */
    void onMatchCommReceived(MatchCommT comm);

    /**
     * Invoked when the script is shut down. Use this to dispose of resources.
     */
    void onRetire();
}
