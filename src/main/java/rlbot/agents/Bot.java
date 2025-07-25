package rlbot.agents;

import rlbot.flat.ControllerStateT;
import rlbot.flat.GamePacketT;
import rlbot.flat.MatchCommT;
import rlbot.flat.PlayerLoadoutT;

public interface Bot {

    PlayerLoadoutT getInitialLoadout();

    ControllerStateT getOutput(GamePacketT packet);

    void onMatchCommReceived(MatchCommT comm);

    void onRetire();
}
