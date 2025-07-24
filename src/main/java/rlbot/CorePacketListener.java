package rlbot;

import rlbot.flat.*;

public interface CorePacketListener {

    void onConnectCallback();
    void onDisconnectCallback();
    void onAnyMessageCallback(CorePacketT packet);
    void onGamePacketCallback(GamePacketT packet);
    void onFieldInfoCallback(FieldInfoT fieldInfo);
    void onMatchConfigCallback(MatchConfigurationT config);
    void onMatchCommsCallback(MatchCommT comm);
    void onBallPredictionCallback(BallPredictionT prediction);
    void onControllableTeamInfoCallback(ControllableTeamInfoT teamInfo);
    void onRenderingStatusCallback(RenderingStatusT status);
}
