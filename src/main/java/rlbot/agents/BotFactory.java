package rlbot.agents;

import rlbot.flat.FieldInfoT;
import rlbot.flat.MatchConfigurationT;
import rlbot.protocol.RLBotInterface;

@FunctionalInterface
public interface BotFactory {

    Bot create(RLBotInterface rlbot, int index, int team, String name, String agentId, MatchConfigurationT matchConfig, FieldInfoT fieldInfo);
}
