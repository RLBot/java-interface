package rlbot.agents;

import rlbot.flat.*;

public interface Bot {

    PlayerLoadoutT getInitialLoadout();

    ControllerStateT getOutput(GamePacketT packet, BallPredictionT ballPrediction);

    void onMatchCommReceived(MatchCommT comm);

    void onRetire();
}
