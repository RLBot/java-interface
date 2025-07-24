package rlbot;

import rlbot.flat.FieldInfoT;
import rlbot.flat.MatchConfigurationT;

public interface ScriptFactory {

    Script create(int index, String agentId, MatchConfigurationT matchConfig, FieldInfoT fieldInfo);
}
