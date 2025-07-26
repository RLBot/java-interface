package rlbot.agents;

import rlbot.flat.*;

import java.util.Map;

public interface Hivemind {

    Map<Integer, PlayerLoadoutT> getInitialLoadouts();

    Map<Integer, ControllerStateT> getOutputs(GamePacketT packet, BallPredictionT ballPrediction);

    void onMatchCommReceived(MatchCommT comm);

    void onRetire();
}
