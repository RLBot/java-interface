package rlbot.protocol;

import rlbot.flat.*;

public interface RLBotListener {

    void onConnect();
    void onDisconnect();
    void onAnyMessage(CorePacketT packet);
    void onGamePacket(GamePacketT packet);
    void onFieldInfo(FieldInfoT fieldInfo);
    void onMatchConfig(MatchConfigurationT config);
    void onMatchComms(MatchCommT comm);
    void onBallPrediction(BallPredictionT prediction);
    void onControllableTeamInfo(ControllableTeamInfoT teamInfo);
    void onRenderingStatus(RenderingStatusT status);
}
