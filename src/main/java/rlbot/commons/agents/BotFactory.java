package rlbot.commons.agents;

import rlbot.flat.FieldInfoT;
import rlbot.flat.MatchConfigurationT;
import rlbot.commons.protocol.RLBotInterface;

/**
 * A functional interface for factories that create instances of {@link Bot}.
 */
@FunctionalInterface
public interface BotFactory {

    /**
     * Create an instance of {@link Bot} to be managed by {@link BotManager}.
     * @param rlbot the rlbot interface connection wrapper.
     * @param index the index of the bot.
     * @param team the team of the bot (blue is 0, orange is 1).
     * @param name the in-game name of the bot, fx. Nexto (2).
     * @param agentId the agent id of the bot.
     * @param matchConfig the match configuration describing the current match, its participants, mutators, and more.
     * @param fieldInfo the static objects on the map like boost pads and goals.
     * @return a new instance of {@link Bot}.
     */
    Bot create(RLBotInterface rlbot, int index, int team, String name, String agentId, MatchConfigurationT matchConfig, FieldInfoT fieldInfo);
}
