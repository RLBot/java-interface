package rlbot.commons.agents;

import rlbot.flat.*;
import rlbot.commons.protocol.RLBotInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A standard manager for RLBot bots, handling bot lifecycle including initialization, packet reading,
 * and retirement on disconnect.
 * The manager must be constructed with an {@link RLBotInterface}, a default agent ID, and a {@link BotFactory}
 * that is used to initialize new bot instances once all the required information has arrived.
 * <p>
 * Each bot controlled by this manager is run on a dedicated thread
 * (there may be more than one bot/thread if the manager controls a hivemind).
 * Use the {@link HivemindManager} for custom handling of the hivemind's agents.
 * <p>
 * Example usage:
 * <pre>
 *     {@code
 *         var rlbot = new RLBotInterface();
 *         var botManager = new BotManager(rlbot, "myname/examplebot/v0.1", ExampleBot::new);
 *         botManager.run();
 *     }
 * </pre>
 */
public class BotManager extends AgentBaseManager {

    private record GameTickData(GamePacketT packet, BallPredictionT ballPred) {}

    /**
     * A bot process managed by this manager.
     * The queue is a single-message queue holding the latest game packet and ball prediction.
     * Ball prediction may be {@code null}.
     */
    private record BotProcess(Bot bot, String name, int index, AtomicBoolean running,
                              BlockingQueue<GameTickData> queue) {}

    private final BotFactory botFactory;
    private List<BotProcess> botProcesses;

    /**
     * Construct a BotManager. The manager must be constructed with an {@link RLBotInterface},
     * a default agent ID that should match the player configuration agent ID, and a {@link BotFactory}
     * that is used to initialize new bot instances once all the required information has arrived.
     * @param rlbot an {@link RLBotInterface} connection to use.
     * @param defaultAgentId a default agent ID used to identify what agent this connection controls if the process
     *                       is not started by RLBot. It should match the agent ID in the player configuration.
     * @param botFactory a {@link BotFactory} to create {@link Bot} instances.
     */
    public BotManager(RLBotInterface rlbot, String defaultAgentId, BotFactory botFactory) {
        super(rlbot, defaultAgentId);
        this.botFactory = botFactory;
    }

    @Override
    void initialize() {
        // Create bot processes
        var playerConfs = getMatchConfig().getPlayerConfigurations();
        var info = getTeamInfo();
        var team = (int) info.getTeam();
        var agents = info.getControllables();
        botProcesses = new ArrayList<>();
        for (var agent : agents) {
            var index = (int) agent.getIndex();
            var name = playerConfs[index].getVariety().asCustomBot().getName();
            var bot = botFactory.create(getRlbotInterface(), index, team, name, getAgentId(), getMatchConfig(), getFieldInfo());
            var running = new AtomicBoolean(true);
            var queue = new ArrayBlockingQueue<GameTickData>(1);
            var process = new BotProcess(bot, name, index, running, queue);
            botProcesses.add(process);
            new Thread(() -> botLoop(process)).start();
        }

        // Set their loadouts
        for (var botProcess : botProcesses) {
            var loadout = botProcess.bot.getInitialLoadout();
            if (loadout != null) {
                var cmd = new SetLoadoutT();
                cmd.setIndex(botProcess.index);
                cmd.setLoadout(loadout);
                getRlbotInterface().sendSetLoadout(cmd);
            }
        }
    }

    /**
     * The main loop of the bot processes.
     */
    private void botLoop(BotProcess process) {
        try {
            while (process.running.get()) {
                var tick = process.queue.take(); // Blocking
                ControllerStateT controller;
                try {
                    controller = process.bot.getOutput(tick.packet, tick.ballPred);
                } catch (RuntimeException e) {
                    logger.severe(process.name + " encountered an error while processing game packet: " + e.getMessage());
                    return;
                }
                if (controller != null) {
                    var input = new PlayerInputT();
                    input.setPlayerIndex(process.index);
                    input.setControllerState(controller);
                    getRlbotInterface().sendPlayerInput(input);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            process.bot.onRetire();
        }
    }

    @Override
    protected void processPacket() {
        if (botProcesses == null) {
            return;
        }
        for (var process : botProcesses) {
            try {
                // There can only be 1 element in the queue,
                // so we clear in case the bot process is slow and missed the previous packet
                process.queue.clear();
                process.queue.put(new GameTickData(latestGamePacket, latestBallPrediction));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void retire() {
        if (botProcesses == null) {
            return;
        }
        for (var process : botProcesses) {
            process.running.set(false);
            // Bot loop calls retire method
        }
    }

    @Override
    public void onMatchComms(MatchCommT comm) {
        if (botProcesses == null) {
            return;
        }
        for (var process : botProcesses) {
            try {
                process.bot.onMatchCommReceived(comm);
            } catch (Exception e) {
                logger.severe(process.name + " encountered an error while processing match comms: " + e.getMessage());
            }
        }
    }
}
