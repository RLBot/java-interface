package rlbot.commons.agents;

import rlbot.flat.*;

/**
 * An interface for individual bots managed by a {@link BotManager}.
 *
 * @see BotManager
 */
public interface Bot {

    /**
     * Defines the bot's initial loadout. Return {@code null} to use the {@code loadout_file} specified in the bot toml file.
     * The loadout from the bot toml file will always be used if the match has already started.
     * To change loadout mid-match, use the {@link rlbot.commons.protocol.RLBotInterface#sendSetLoadout(SetLoadoutT)} method.
     * State-setting must be enabled for that to work.
     * @return the overwritten initial loadout or null.
     */
    PlayerLoadoutT getInitialLoadout();

    /**
     * Process the latest game state and return this bot's next input.
     * @param packet the latest game packet.
     * @param ballPrediction the latest ball prediction. May be null if ball prediction was not requested.
     * @return the bot's next input.
     */
    ControllerStateT getOutput(GamePacketT packet, BallPredictionT ballPrediction);

    /**
     * Process an incoming {@link MatchComm} message.
     * See the index and team field to determine the sender.
     * @param comm the received message.
     */
    void onMatchCommReceived(MatchCommT comm);

    /**
     * Invoked when this bot is shut down. Use this to dispose of resources.
     */
    void onRetire();
}
