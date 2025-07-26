package rlbot.protocol;

import rlbot.flat.*;

public abstract class RLBotListenerAdapter implements RLBotListener {

    @Override
    public void onConnect() {

    }

    @Override
    public void onDisconnect() {

    }

    @Override
    public void onAnyMessage(CorePacketT packet) {

    }

    @Override
    public void onGamePacket(GamePacketT packet) {

    }

    @Override
    public void onFieldInfo(FieldInfoT fieldInfo) {

    }

    @Override
    public void onMatchConfig(MatchConfigurationT config) {

    }

    @Override
    public void onMatchComms(MatchCommT comm) {

    }

    @Override
    public void onBallPrediction(BallPredictionT prediction) {

    }

    @Override
    public void onControllableTeamInfo(ControllableTeamInfoT teamInfo) {

    }

    @Override
    public void onRenderingStatus(RenderingStatusT status) {

    }
}
