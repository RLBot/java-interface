package rlbot;

import rlbot.flat.FieldInfoT;
import rlbot.flat.MatchConfigurationT;

import java.util.List;
import java.util.Map;

public interface HivemindFactory {

    Hivemind create(List<Integer> indices, int team, Map<Integer, String> names, String agentId, MatchConfigurationT matchConfig, FieldInfoT fieldInfo);
}
