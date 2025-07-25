package rlbot.agents;

import rlbot.flat.FieldInfoT;
import rlbot.flat.MatchConfigurationT;
import rlbot.protocol.RLBotInterface;

import java.util.List;
import java.util.Map;

public interface HivemindFactory {

    Hivemind create(RLBotInterface rlbot, List<Integer> indices, int team, Map<Integer, String> names, String agentId, MatchConfigurationT matchConfig, FieldInfoT fieldInfo);
}
