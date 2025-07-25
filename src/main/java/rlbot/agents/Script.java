package rlbot.agents;

import rlbot.flat.ControllerStateT;
import rlbot.flat.GamePacketT;
import rlbot.flat.MatchCommT;

public interface Script {

    ControllerStateT process(GamePacketT packet);

    void onMatchCommReceived(MatchCommT comm);

    void onRetire();
}
