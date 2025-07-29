package rlbot.commons.agents;

import rlbot.flat.FieldInfoT;
import rlbot.flat.MatchConfigurationT;
import rlbot.commons.protocol.RLBotInterface;

public interface ScriptFactory {

    Script create(RLBotInterface rlbot, int index, String agentId, MatchConfigurationT matchConfig, FieldInfoT fieldInfo);
}
