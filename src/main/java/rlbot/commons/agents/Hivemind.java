package rlbot.commons.agents;

import rlbot.flat.*;

import java.util.Map;

/**
 * An interface for hiveminds managed by a {@link HivemindManager}.
 * A hivemind is a single connection that controls multiple cars.
 * When using the {@link HivemindManager}, the entire hivemind is using
 * the same thread, and you can orchestrate as you wish from there.
 * You can use the {@link BotManager} instead, if you simply want one thread per car.
 *
 * @see HivemindManager
 * @see BotManager
 */
public interface Hivemind {

    /**
     * Defines the bots' initial loadout. Include no entry or a {@code null} value to use the {@code loadout_file}
     * specified in the bot toml file. Returning null or an empty map to use the loadout file for all cars.
     * The loadout from the bot toml file will always be used if the match has already started. To change loadout
     * mid-match, use the {@link rlbot.commons.protocol.RLBotInterface#sendSetLoadout(SetLoadoutT)} method.
     * State-setting must be enabled for that to work.
     * @return the overwritten initial loadouts.
     */
    Map<Integer, PlayerLoadoutT> getInitialLoadouts();

    /**
     * Process the latest game state and returns the next inputs for the cars controlled by this connection.
     * @param packet the latest game packet.
     * @param ballPrediction the latest ball prediction. May be null if ball prediction was not requested.
     * @return the bots' next input.
     */
    Map<Integer, ControllerStateT> getOutputs(GamePacketT packet, BallPredictionT ballPrediction);

    /**
     * Process an incoming {@link MatchComm} message.
     * See the index and team field to determine the sender.
     * @param comm the received message.
     */
    void onMatchCommReceived(MatchCommT comm);

    /**
     * Invoked when this hivemind is shut down. Use this to dispose of resources.
     */
    void onRetire();
}
