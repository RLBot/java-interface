package rlbot;

import rlbot.flat.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class BotManager extends AgentBaseManager {

    private record BotProcess(Bot bot, String name, int index, AtomicBoolean running,
                              BlockingQueue<GamePacketT> queue) {}

    private final BotFactory botFactory;
    private List<BotProcess> botProcesses;

    public BotManager(RLBotInterface rlbot, String defaultAgentId, BotFactory botFactory) {
        super(rlbot, defaultAgentId);
        this.botFactory = botFactory;
    }

    @Override
    void initialize() {
        var playerConfs = getMatchConfig().getPlayerConfigurations();
        var info = getTeamInfo();
        var team = (int) info.getTeam();
        var agents = info.getControllables();
        botProcesses = new ArrayList<>();
        for (var agent : agents) {
            var index = (int) agent.getIndex();
            var name = playerConfs[index].getVariety().asCustomBot().getName();
            var bot = botFactory.create(index, team, name, getAgentId(), getMatchConfig(), getFieldInfo());
            var running = new AtomicBoolean(true);
            var queue = new ArrayBlockingQueue<GamePacketT>(1);
            var process = new BotProcess(bot, name, index, running, queue);
            botProcesses.add(process);
            new Thread(() -> botLoop(process)).start();
        }

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

    private void botLoop(BotProcess process) {
        try {
            while (process.running.get()) {
                var packet = process.queue.take(); // Blocking
                ControllerStateT controller;
                try {
                    controller = process.bot.getOutput(packet);
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
                process.queue.clear();
                process.queue.put(latestGamePacket);
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
    public void onMatchCommsCallback(MatchCommT comm) {
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
