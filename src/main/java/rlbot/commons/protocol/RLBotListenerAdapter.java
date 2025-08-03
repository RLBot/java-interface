package rlbot.commons.protocol;

import rlbot.flat.*;


/**
 * RLBotListenerAdapter is an abstract convenience class that provides empty implementations
 * for all methods defined in the {@link RLBotListener} interface. This allows subclasses
 * to override only the methods they are interested in, without being required to implement
 * all the methods of the interface.
 *
 * @see RLBotListener
 */
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
