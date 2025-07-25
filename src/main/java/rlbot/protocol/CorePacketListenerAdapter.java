package rlbot.protocol;

import rlbot.flat.*;

public abstract class CorePacketListenerAdapter implements CorePacketListener {

    @Override
    public void onConnectCallback() {

    }

    @Override
    public void onDisconnectCallback() {

    }

    @Override
    public void onAnyMessageCallback(CorePacketT packet) {

    }

    @Override
    public void onGamePacketCallback(GamePacketT packet) {

    }

    @Override
    public void onFieldInfoCallback(FieldInfoT fieldInfo) {

    }

    @Override
    public void onMatchConfigCallback(MatchConfigurationT config) {

    }

    @Override
    public void onMatchCommsCallback(MatchCommT comm) {

    }

    @Override
    public void onBallPredictionCallback(BallPredictionT prediction) {

    }

    @Override
    public void onControllableTeamInfoCallback(ControllableTeamInfoT teamInfo) {

    }

    @Override
    public void onRenderingStatusCallback(RenderingStatusT status) {

    }
}
