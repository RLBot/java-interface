package rlbot.commons.desired;

import rlbot.flat.DesiredMatchInfoT;
import rlbot.flat.FloatT;

/**
 * A builder for a {@link DesiredMatchInfoT} used for game state setting.
 * <p>
 * Components that are not set will keep their current value in-game.
 */
public class DesiredMatchInfoBuilder {

    private final DesiredMatchInfoT matchInfo;

    public DesiredMatchInfoBuilder(DesiredMatchInfoT matchInfo) {
        this.matchInfo = matchInfo;
    }

    /**
     * Set the world gravity z.
     */
    public DesiredMatchInfoBuilder gravityZ(float value) {
        matchInfo.setWorldGravityZ(floatT(value));
        return this;
    }

    /**
     * Set the game speed modifier.
     */
    public DesiredMatchInfoBuilder gameSpeed(float value) {
        matchInfo.setGameSpeed(floatT(value));
        return this;
    }

    private static FloatT floatT(float value) {
        var f = new FloatT();
        f.setVal(value);
        return f;
    }
}
