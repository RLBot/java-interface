package rlbot.commons.desired;

import rlbot.commons.protocol.RLBotInterface;
import rlbot.flat.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A builder for a {@link DesiredGameStateT} used for game state setting.
 * <p>
 * Builders for individual cars, balls, and match info are created lazily when accessed
 * so that higher indices automatically create any missing entries.
 * <p>
 * Example usage:
 * <pre>
 *     {@code
 *         new DesiredGameStateBuilder(rlbot)
 *                 .matchInfo(m -> m.gravityZ(-1000))
 *                 .car(0, c -> c.boost(100).location(0, 0, 50))
 *                 .ball(0, b -> b.location(-3000, 0, 100).velocity(0, 0, 0))
 *                 .buildAndSend();
 *     }
 * </pre>
 */
public class DesiredGameStateBuilder {

    private final RLBotInterface rlbot;
    private final DesiredGameStateT state;

    public DesiredGameStateBuilder(RLBotInterface rlbot) {
        this.rlbot = rlbot;
        this.state = new DesiredGameStateT();
        state.setBallStates(new DesiredBallStateT[0]);
        state.setCarStates(new DesiredCarStateT[0]);
    }

    /**
     * Modify the desired match info.
     *
     * @param build a function that configures the {@link DesiredMatchInfoBuilder}.
     */
    public DesiredGameStateBuilder matchInfo(Consumer<DesiredMatchInfoBuilder> build) {
        var matchInfo = state.getMatchInfo();
        if (matchInfo == null) {
            matchInfo = new DesiredMatchInfoT();
            matchInfo.setWorldGravityZ(null);
            matchInfo.setGameSpeed(null);
            state.setMatchInfo(matchInfo);
        }
        build.accept(new DesiredMatchInfoBuilder(matchInfo));
        return this;
    }

    /**
     * Modify the desired car state at the given index.
     *
     * @param index the index of the car.
     * @param build a function that configures the {@link DesiredCarStateBuilder}.
     */
    public DesiredGameStateBuilder car(int index, Consumer<DesiredCarStateBuilder> build) {
        build.accept(new DesiredCarStateBuilder(ensureCarState(index)));
        return this;
    }

    /**
     * Modify the desired cars at the given indices.
     *
     * @param indices the indices of the cars.
     * @param build   a function that receives each index and configures the associated {@link DesiredCarStateBuilder}.
     */
    public DesiredGameStateBuilder cars(Collection<Integer> indices, BiConsumer<Integer, DesiredCarStateBuilder> build) {
        for (var index : indices) {
            build.accept(index, new DesiredCarStateBuilder(ensureCarState(index)));
        }
        return this;
    }

    /**
     * Modify the desired ball state at the given index.
     *
     * @param index the index of the ball.
     * @param build a function that configures the {@link DesiredBallStateBuilder}.
     */
    public DesiredGameStateBuilder ball(int index, Consumer<DesiredBallStateBuilder> build) {
        build.accept(new DesiredBallStateBuilder(ensureBallState(index)));
        return this;
    }

    /**
     * Modify the desired balls at the given indices.
     *
     * @param indices the indices of the balls.
     * @param build   a function that receives each index and configures the associated {@link DesiredBallStateBuilder}.
     */
    public DesiredGameStateBuilder balls(Collection<Integer> indices, BiConsumer<Integer, DesiredBallStateBuilder> build) {
        for (var index : indices) {
            build.accept(index, new DesiredBallStateBuilder(ensureBallState(index)));
        }
        return this;
    }

    /**
     * Returns the constructed desired game state.
     */
    public DesiredGameStateT build() {
        return state;
    }

    /**
     * Sends the constructed desired game state to the RLBot server.
     */
    public void buildAndSend() {
        rlbot.sendGameState(build());
    }

    private DesiredCarStateT ensureCarState(int index) {
        var carStates = state.getCarStates();
        if (carStates.length <= index) {
            var oldLength = carStates.length;
            carStates = Arrays.copyOf(carStates, index + 1);
            for (int i = oldLength; i <= index; i++) {
                var carState = new DesiredCarStateT();
                carState.setBoostAmount(null);
                carStates[i] = carState;
            }
            state.setCarStates(carStates);
        }
        return state.getCarStates()[index];
    }

    private DesiredBallStateT ensureBallState(int index) {
        var ballStates = state.getBallStates();
        if (ballStates.length <= index) {
            var oldLength = ballStates.length;
            ballStates = Arrays.copyOf(ballStates, index + 1);
            for (int i = oldLength; i <= index; i++) {
                ballStates[i] = new DesiredBallStateT();
            }
            state.setBallStates(ballStates);
        }
        return state.getBallStates()[index];
    }
}
