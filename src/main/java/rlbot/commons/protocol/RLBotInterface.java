package rlbot.commons.protocol;

import rlbot.flat.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.logging.Logger;

public class RLBotInterface implements Runnable {

    public static final int DEFAULT_SERVER_PORT = 23234;

    private final Logger logger = Logger.getLogger(RLBotInterface.class.getName());

    private final Socket socket = new Socket() {{
        try {
            setTcpNoDelay(false);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }};
    private SpecReader in;
    private SpecWriter out;

    private final ArrayList<RLBotListener> listeners = new ArrayList<>();

    private final int connectionTimeout;

    private boolean isConnected = false;
    private boolean isRunning = false;
    private ProcessHandle serverProcess;

    public RLBotInterface() {
        this(120);
    }

    public RLBotInterface(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void addListener(RLBotListener listener) {
        listeners.add(listener);
    }

    public void removeListener(RLBotListener listener) {
        listeners.remove(listener);
    }

    public void sendFlatbufferMsg(InterfaceMessageUnion msg) {
        if (!isConnected) {
            throw new RuntimeException("Connection has not been established");
        }

        try {
            out.write(msg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendInitComplete() {
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.InitComplete);
        msg.setValue(new InitCompleteT());
        sendFlatbufferMsg(msg);
    }

    public void sendSetLoadout(SetLoadoutT setLoadout) {
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.SetLoadout);
        msg.setValue(setLoadout);
        sendFlatbufferMsg(msg);
    }

    public void sendMatchComm(MatchCommT matchComm) {
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.MatchComm);
        msg.setValue(matchComm);
        sendFlatbufferMsg(msg);
    }

    public void sendPlayerInput(PlayerInputT playerInput) {
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.PlayerInput);
        msg.setValue(playerInput);
        sendFlatbufferMsg(msg);
    }

    public void sendGameState(DesiredGameStateT gameState) {
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.DesiredGameState);
        msg.setValue(gameState);
        sendFlatbufferMsg(msg);
    }

    public void sendRenderGroup(RenderGroupT renderGroup) {
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.RenderGroup);
        msg.setValue(renderGroup);
        sendFlatbufferMsg(msg);
    }

    public void sendRemoveRenderGroup(RemoveRenderGroupT renderGroup) {
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.RemoveRenderGroup);
        msg.setValue(renderGroup);
        sendFlatbufferMsg(msg);
    }

    public void stopMatch(boolean shutdownRLBot) {
        var stop = new StopCommandT();
        stop.setShutdownServer(shutdownRLBot);
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.StopCommand);
        msg.setValue(stop);
        sendFlatbufferMsg(msg);
    }

    public void startMatch(MatchConfigurationT matchConfig) {
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.MatchConfiguration);
        msg.setValue(matchConfig);
        sendFlatbufferMsg(msg);
        logger.info("Starting match");
        sendInitComplete();
    }

    public void startMatch(Path matchConfigPath) throws FileNotFoundException {
        if (!Files.exists(matchConfigPath)) {
            throw new FileNotFoundException(matchConfigPath.toString());
        } else if (!Files.isRegularFile(matchConfigPath)) {
            throw new IllegalArgumentException("Match config path is not a file (maybe a directory?): " + matchConfigPath);
        }
        var start = new StartCommandT();
        start.setConfigPath(matchConfigPath.toString());
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.StartCommand);
        msg.setValue(start);
        sendFlatbufferMsg(msg);
        logger.info("Starting match");
        sendInitComplete();
    }

    public boolean tryLaunchRLBotServer() {
        return tryLaunchRLBotServer(true);
    }

    public boolean tryLaunchRLBotServer(boolean lookInCwd) {
        var binName = RLBotServerUtils.defaultBinName();

        var optionalServerProcess = RLBotServerUtils.findServerProcess(binName);
        if (optionalServerProcess.isPresent()) {
            this.serverProcess = optionalServerProcess.get();
            logger.info("RLBotServer is already running");
            return true;
        }

        if (lookInCwd) {
            var cwd = Paths.get("").toAbsolutePath();
            try (var stream = Files.find(cwd, 10, (path, attr) -> Files.isRegularFile(path) && path.endsWith(binName))) {
                var optionalBin = stream.findAny();
                if (optionalBin.isPresent()) {
                    logger.info("Launching " + binName + " in cwd");
                    this.serverProcess = new ProcessBuilder("cmd", "/c", "start", "cmd", "/k", optionalBin.get().toAbsolutePath().toString())
                            .start()
                            .toHandle();
                    Thread.sleep(50);
                    return true;
                }

            } catch (IOException e) {
                logger.severe("Error while trying to find and launch " + binName + " in cwd: " + e.getMessage());
                return false;
            } catch (InterruptedException ignored) {
                return true;
            }
        }

        try {
            var binPath = RLBotServerUtils.defaultInstallDir().resolve(binName);
            if (!Files.exists(binPath)) {
                return false;
            }

            logger.info("Launching installed " + binName);
            this.serverProcess = new ProcessBuilder("cmd", "/c", "start", "cmd", "/k", binPath.toAbsolutePath().toString())
                    .start()
                    .toHandle();
            Thread.sleep(50);
            return true;
        } catch (IOException e) {
            logger.severe("Error while trying to launch installed " + binName + ": " + e.getMessage());
            return false;
        } catch (InterruptedException ignored) {
            return true;
        }
    }

    public void shutdownRLBotServer(boolean force) {
        stopMatch(true);
        if (serverProcess != null && force) {
            serverProcess.destroy();
        }
        serverProcess = null;
    }

    @Override
    public void run() {
        run(false);
    }

    public void runInBackground() {
        run(true);
    }

    public void run(boolean inBackgroundThread) {
        if (!isConnected) {
            throw new RuntimeException("Connection has not been established.");
        }

        if (isRunning) {
            throw new RuntimeException("Message handling is already running.");
        }

        if (inBackgroundThread) {
            new Thread(() -> run(false)).start();
        } else {
            isRunning = true;
            while (isRunning && isConnected) {
                isRunning = handleNextIncomingMsg(true) != MsgHandlingResult.Termination;
            }
            isRunning = false;
        }
    }

    public enum MsgHandlingResult
    {
        Termination,
        NoIncomingMsgs,
        MoreMsgsQueued

    }
    public MsgHandlingResult handleNextIncomingMsg(boolean blocking) {
        if (!isConnected) {
            throw new RuntimeException("Connection has not been established.");
        }

        try {
            if (!blocking && !in.anyAvailable()) {
                return MsgHandlingResult.NoIncomingMsgs;
            }
            var packet = in.readOne().unpack();

            try {
                return handleIncomingMsg(packet) ? MsgHandlingResult.MoreMsgsQueued : MsgHandlingResult.Termination;
            } catch (Exception e) {
                var typeIndex = packet.getMessage().getType();
                var typeName = InterfaceMessage.name(typeIndex);
                logger.severe("Unexpected error while handling message of type " + typeName);
                e.printStackTrace();
                return MsgHandlingResult.Termination;
            }

        } catch (SocketTimeoutException e) {
            return MsgHandlingResult.NoIncomingMsgs;
        } catch (IOException e) {
            logger.severe("IO error while reading messages from rlbot.");
            throw new RuntimeException(e);
        }
    }
    private boolean handleIncomingMsg(CorePacketT packet) {
        CoreMessageUnion msg = packet.getMessage();
        for (var listener : listeners) {
            listener.onAnyMessage(packet);
        }
        switch (msg.getType()) {
            case CoreMessage.NONE:
            case CoreMessage.DisconnectSignal:
                for (var listener : listeners) {
                    listener.onDisconnect();
                }
                return false;
            case CoreMessage.GamePacket:
                var gp = msg.asGamePacket();
                for (var listener : listeners) {
                    listener.onGamePacket(gp);
                }
                break;
            case CoreMessage.FieldInfo:
                var fieldInfo = msg.asFieldInfo();
                for (var listener : listeners) {
                    listener.onFieldInfo(fieldInfo);
                }
                break;
            case CoreMessage.MatchConfiguration:
                var config = msg.asMatchConfiguration();
                for (var listener : listeners) {
                    listener.onMatchConfig(config);
                }
                break;
            case CoreMessage.MatchComm:
                var comm = msg.asMatchComm();
                for (var listener : listeners) {
                    listener.onMatchComms(comm);
                }
                break;
            case CoreMessage.BallPrediction:
                var prediction = msg.asBallPrediction();
                for (var listener : listeners) {
                    listener.onBallPrediction(prediction);
                }
                break;
            case CoreMessage.ControllableTeamInfo:
                var teamInfo = msg.asControllableTeamInfo();
                for (var listener : listeners) {
                    listener.onControllableTeamInfo(teamInfo);
                }
                break;
            case CoreMessage.RenderingStatus:
                var status = msg.asRenderingStatus();
                for (var listener : listeners) {
                    listener.onRenderingStatus(status);
                }
                break;
            default:
                logger.warning("Received message of unknown type: " + msg.getType());
                break;
        }
        return true;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void stopRunning() {
        isRunning = false;
    }

    public void connectAsMatchHost() {
        connect("", false, false, true);
    }

    public void connectAsMatchHost(boolean wantsBallPredictions, boolean wantsComms) {
        connect("", wantsBallPredictions, wantsComms, true);
    }

    public void connect(String agentId, boolean wantsBallPredictions, boolean wantsComms, boolean outliveMatches) {
        String portRaw = System.getenv("RLBOT_SERVER_PORT");
        int port = portRaw == null ? RLBotInterface.DEFAULT_SERVER_PORT : Integer.parseInt(portRaw);
        connect(agentId, wantsBallPredictions, wantsComms, outliveMatches, port);
    }

    public void connect(String agentId, boolean wantsBallPredictions, boolean wantsComms, boolean outliveMatches, int rlbotServerPort) {
        if (isConnected) {
            return;
        }

        var beginTime = System.currentTimeMillis();
        var nextWarning = 10_000;
        while (System.currentTimeMillis() < beginTime + connectionTimeout * 1000) {
            try {
                socket.connect(new InetSocketAddress("127.0.0.1", rlbotServerPort));
                socket.setSoTimeout(0);
                isConnected = true;
                break;
            } catch (IOException e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {

                }

                if (System.currentTimeMillis() > beginTime + nextWarning) {
                    nextWarning *= 2;
                    logger.warning("Failing to connect to RLBot on port " + rlbotServerPort + ". Trying again ...");
                }
            }
        }

        if (!isConnected) {
            throw new RuntimeException("Failed to establish connection. Ensure that the RLBotServer is running. If you are using the RLBotInterface directly, try calling ensureServerStarted() before connecting.");
        }

        try {
            in = new SpecReader(socket.getInputStream());
            out = new SpecWriter(socket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.info("Connected to RLBot on port " + rlbotServerPort + " from port " + socket.getLocalPort());

        var connectionSettings = new ConnectionSettingsT();
        connectionSettings.setAgentId(agentId);
        connectionSettings.setWantsBallPredictions(wantsBallPredictions);
        connectionSettings.setWantsComms(wantsComms);
        connectionSettings.setCloseBetweenMatches(!outliveMatches);

        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.ConnectionSettings);
        msg.setValue(connectionSettings);
        sendFlatbufferMsg(msg);

        for (var listener : listeners) {
            listener.onConnect();
        }
    }

    public void disconnect() {
        if (!isConnected) {
            logger.warning("Asked to disconnect but was already disconnected.");
            return;
        }

        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.DisconnectSignal);
        msg.setValue(new DisconnectSignal());
        sendFlatbufferMsg(msg);

        int timeout = 5_000;
        while (isRunning && timeout > 0) {
            try {
                Thread.sleep(100);
                timeout -= 100;
            } catch (InterruptedException ignored) {

            }
        }

        if (timeout <= 0) {
            logger.severe("RLBot is not responding to our disconnect request!");
            isRunning = false;
        }

        try {
            socket.close();
        } catch (IOException ignored) {

        }

        isConnected = false;

        for (var listener : listeners) {
            listener.onDisconnect();
        }
    }

    public boolean isConnected() {
        return isConnected;
    }
}
