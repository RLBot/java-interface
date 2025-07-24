package rlbot;

import rlbot.flat.FieldInfoT;
import rlbot.flat.MatchConfigurationT;

@FunctionalInterface
public interface BotFactory {

    Bot create(int index, int team, String name, String agentId, MatchConfigurationT matchConfig, FieldInfoT fieldInfo);
}
