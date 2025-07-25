package rlbot.agents;

import rlbot.flat.FieldInfoT;
import rlbot.flat.MatchConfigurationT;
import rlbot.protocol.RLBotInterface;

public interface ScriptFactory {

    Script create(RLBotInterface rlbot, int index, String agentId, MatchConfigurationT matchConfig, FieldInfoT fieldInfo);
}
