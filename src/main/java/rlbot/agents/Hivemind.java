package rlbot.agents;

import rlbot.flat.ControllerStateT;
import rlbot.flat.GamePacketT;
import rlbot.flat.MatchCommT;
import rlbot.flat.PlayerLoadoutT;

import java.util.Map;

public interface Hivemind {

    Map<Integer, PlayerLoadoutT> getInitialLoadouts();

    Map<Integer, ControllerStateT> getOutputs(GamePacketT packet);

    void onMatchCommReceived(MatchCommT comm);

    void onRetire();
}
