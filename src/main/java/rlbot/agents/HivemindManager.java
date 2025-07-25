package rlbot.agents;

import rlbot.flat.ControllerStateT;
import rlbot.flat.MatchCommT;
import rlbot.flat.PlayerInputT;
import rlbot.flat.SetLoadoutT;
import rlbot.protocol.RLBotInterface;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HivemindManager extends AgentBaseManager {

    private final HivemindFactory hivemindFactory;
    private Hivemind hivemind;
    private List<Integer> indices;
    private int team;

    public HivemindManager(RLBotInterface rlbot, String defaultAgentId, HivemindFactory hivemindFactory) {
        super(rlbot, defaultAgentId);
        this.hivemindFactory = hivemindFactory;
    }

    @Override
    void initialize() {
        var playerConfs = getMatchConfig().getPlayerConfigurations();
        var info = getTeamInfo();
        team = (int) info.getTeam();
        indices = Arrays.stream(info.getControllables()).map(a -> (int) a.getIndex()).toList();
        var names = indices.stream().collect(Collectors.toMap(i -> i, i -> playerConfs[i].getVariety().asCustomBot().getName()));

        hivemind = hivemindFactory.create(getRlbotInterface(), indices, team, names, getAgentId(), getMatchConfig(), getFieldInfo());

        var loadouts = hivemind.getInitialLoadouts();
        if (loadouts != null) {
            for (var indexLoadout : loadouts.entrySet()) {
                var cmd = new SetLoadoutT();
                cmd.setIndex(indexLoadout.getKey());
                cmd.setLoadout(indexLoadout.getValue());
                getRlbotInterface().sendSetLoadout(cmd);
            }
        }
    }

    @Override
    protected void processPacket() {
        if (hivemind == null) {
            return;
        }
        Map<Integer, ControllerStateT> controllers;
        try {
            controllers = hivemind.getOutputs(latestGamePacket);
        } catch (Exception e) {
            logger.severe("Hivemind '" + getAgentId() + "' (team " + team + ") encountered an error while processing game packet: " + e.getMessage());
            return;
        }

        for (var indexController : controllers.entrySet()) {
            if (indexController.getValue() == null) {
                continue;
            }
            var input = new PlayerInputT();
            input.setPlayerIndex(indexController.getKey());
            input.setControllerState(indexController.getValue());
            getRlbotInterface().sendPlayerInput(input);
        }
    }

    @Override
    protected void retire() {
        if (hivemind != null) {
            hivemind.onRetire();
        }
    }

    @Override
    public void onMatchCommsCallback(MatchCommT comm) {
        if (hivemind == null) {
            return;
        }
        try {
            hivemind.onMatchCommReceived(comm);
        } catch (RuntimeException e) {
            logger.severe("Hivemind '" + getAgentId() + "' (team " + team + ") encountered an error while processing match comms: " + e.getMessage());
        }
    }
}
