package rlbot.commons.agents;

import rlbot.flat.BallPredictionT;
import rlbot.flat.ControllerStateT;
import rlbot.flat.GamePacketT;
import rlbot.flat.MatchCommT;

public interface Script {

    ControllerStateT process(GamePacketT packet, BallPredictionT ballPrediction);

    void onMatchCommReceived(MatchCommT comm);

    void onRetire();
}
