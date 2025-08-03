package rlbot.commons.agents;

import rlbot.flat.FieldInfoT;
import rlbot.flat.MatchConfigurationT;
import rlbot.commons.protocol.RLBotInterface;

import java.util.List;
import java.util.Map;

/**
 * A functional interface for factories that create instances of {@link Hivemind}.
 */
@FunctionalInterface
public interface HivemindFactory {

    /**
     * Create an instance of {@link Hivemind} to be managed by {@link HivemindManager}.
     * @param rlbot the rlbot interface connection wrapper.
     * @param indices the indices of the hivemind's cars.
     * @param team the team of the hivemind (blue is 0, orange is 1).
     * @param names the in-game names of the hivemind's cars, fx. Nexto (2).
     * @param agentId the agent id of the hivemind's cars.
     * @param matchConfig the match configuration describing the current match, its participants, mutators, and more.
     * @param fieldInfo the static objects on the map like boost pads and goals.
     * @return a new instance of {@link Hivemind}.
     */
    Hivemind create(RLBotInterface rlbot, List<Integer> indices, int team, Map<Integer, String> names, String agentId, MatchConfigurationT matchConfig, FieldInfoT fieldInfo);
}
