package rlbot;

import rlbot.flat.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.logging.Logger;

public class RLBotInterface {

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

    private final ArrayList<CorePacketListener> listeners = new ArrayList<>();

    private final int connectionTimeout;

    private boolean isConnected = false;
    private boolean isRunning = false;

    public RLBotInterface() {
        this(120);
    }

    public RLBotInterface(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void addListener(CorePacketListener listener) {
        listeners.add(listener);
    }

    public void removeListener(CorePacketListener listener) {
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

    public void sendStopMatch(boolean shutdownRLBot) {
        var stop = new StopCommandT();
        stop.setShutdownServer(shutdownRLBot);
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.StopCommand);
        msg.setValue(stop);
        sendFlatbufferMsg(msg);
    }

    public void sendStartMatch(MatchConfigurationT matchConfig) {
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.MatchConfiguration);
        msg.setValue(matchConfig);
        sendFlatbufferMsg(msg);
    }

    public void sendStartMatch(Path matchConfigPath) throws FileNotFoundException {
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
    }

    public void connect(String agentId, boolean wantsMatchComms, boolean wantsBallPrediction, boolean closeBetweenMatches, int rlbotServerPort) {
        if (isConnected) {
            throw new RuntimeException("Already connected");
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
        connectionSettings.setWantsBallPredictions(wantsBallPrediction);
        connectionSettings.setWantsComms(wantsMatchComms);
        connectionSettings.setCloseBetweenMatches(closeBetweenMatches);

        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.ConnectionSettings);
        msg.setValue(connectionSettings);
        sendFlatbufferMsg(msg);

        for (var listener : listeners) {
            listener.onConnectCallback();
        }
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
                isRunning = handleIncomingMsgs(true) != MsgHandlingResult.Terminated;
            }
            isRunning = false;
        }
    }

    public enum MsgHandlingResult
    {
        Terminated,
        NoIncomingMsgs,
        MoreMsgsQueued,
    }

    public MsgHandlingResult handleIncomingMsgs(boolean blocking) {
        if (!isConnected) {
            throw new RuntimeException("Connection has not been established.");
        }

        try {
            if (!blocking && !in.anyAvailable()) {
                return MsgHandlingResult.NoIncomingMsgs;
            }
            var packet = in.readOne().unpack();

            try {
                return handleIncomingMsg(packet) ? MsgHandlingResult.MoreMsgsQueued : MsgHandlingResult.Terminated;
            } catch (Exception e) {
                var typeIndex = packet.getMessage().getType();
                var typeName = InterfaceMessage.name(typeIndex);
                logger.severe("Unexpected error while handling message of type " + typeName);
                e.printStackTrace();
                return MsgHandlingResult.Terminated;
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
            listener.onAnyMessageCallback(packet);
        }
        switch (msg.getType()) {
            case CoreMessage.NONE:
            case CoreMessage.DisconnectSignal:
                for (var listener : listeners) {
                    listener.onDisconnectCallback();
                }
                return false;
            case CoreMessage.GamePacket:
                var gp = msg.asGamePacket();
                for (var listener : listeners) {
                    listener.onGamePacketCallback(gp);
                }
                break;
            case CoreMessage.FieldInfo:
                var fieldInfo = msg.asFieldInfo();
                for (var listener : listeners) {
                    listener.onFieldInfoCallback(fieldInfo);
                }
                break;
            case CoreMessage.MatchConfiguration:
                var config = msg.asMatchConfiguration();
                for (var listener : listeners) {
                    listener.onMatchConfigCallback(config);
                }
                break;
            case CoreMessage.MatchComm:
                var comm = msg.asMatchComm();
                for (var listener : listeners) {
                    listener.onMatchCommsCallback(comm);
                }
                break;
            case CoreMessage.BallPrediction:
                var prediction = msg.asBallPrediction();
                for (var listener : listeners) {
                    listener.onBallPredictionCallback(prediction);
                }
                break;
            case CoreMessage.ControllableTeamInfo:
                var teamInfo = msg.asControllableTeamInfo();
                for (var listener : listeners) {
                    listener.onControllableTeamInfoCallback(teamInfo);
                }
                break;
            case CoreMessage.RenderingStatus:
                var status = msg.asRenderingStatus();
                for (var listener : listeners) {
                    listener.onRenderingStatusCallback(status);
                }
                break;
            default:
                logger.warning("Received message of unknown type: " + msg.getType());
                break;
        }
        return true;
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
            listener.onDisconnectCallback();
        }
    }

    public boolean isConnected() {
        return isConnected;
    }
}
