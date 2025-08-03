package rlbot.commons.agents;

import rlbot.flat.FieldInfoT;
import rlbot.flat.MatchConfigurationT;
import rlbot.commons.protocol.RLBotInterface;

/**
 * A functional interface for factories that create instances of {@link Script}.
 */
@FunctionalInterface
public interface ScriptFactory {

    /**
     * Create an instance of {@link Script} to be managed by {@link ScriptManager}.
     * @param rlbot the rlbot interface connection wrapper.
     * @param index the index of the bot.
     * @param agentId the agent id of the bot.
     * @param matchConfig the match configuration describing the current match, its participants, mutators, and more.
     * @param fieldInfo the static objects on the map like boost pads and goals.
     * @return a new instance of {@link Script}.
     */
    Script create(RLBotInterface rlbot, int index, String agentId, MatchConfigurationT matchConfig, FieldInfoT fieldInfo);
}
