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


/**
 * The RLBotInterface provides an abstraction over the direct communication with
 * the RLBotServer making it easy to send the various types of messages.
 * <p>
 * The RLBotInterface also offers methods to start the RLBotServer.
 * <p>
 * Calling one of the run methods will start an incoming message handling loop.
 * Add a {@link RLBotListener} listener to be notified about incoming messages.
 * <p>
 * Example usage:
 * <pre>
 *     {@code
 *         var rlbot = new RLBotInterface();
 *         // Ensure the rlbot server is running
 *         rlbot.tryLaunchRLBotServer();
 *         rlbot.connectAsMatchHost();
 *         // Start a match and listen to messages
 *         rlbot.startMatch(Paths.get("match.toml"));
 *         rlbot.addListener(new MyListener());
 *         rlbot.run();
 *     }
 * </pre>
 */
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

    /**
     * Whether the message handling loop is running (potentially in a background thread)
     */
    private boolean isRunning = false;

    /**
     * A handle to the RLBotServer process. May be null if we have not started it or attempted to find it yet
     */
    private ProcessHandle serverProcess;

    /**
     * The default constructor.
     */
    public RLBotInterface() {
        this(120);
    }

    /**
     * A constructor with a custom connection timeout.
     */
    public RLBotInterface(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Adds an {@link RLBotListener} to be notified about incoming messages.
     *
     * @see RLBotListenerAdapter
     */
    public void addListener(RLBotListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove an {@link RLBotListener} so it will no longer be notified about incoming messages.
     *
     * @return {@code true} if the given listener was subscribed.
     */
    public boolean removeListener(RLBotListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Send a FlatBuffer message to the RLBotServer using the RLBot socket protocol.
     *
     * @param msg the message to send.
     */
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

    /**
     * Sends an {@link InitComplete} message to indicate that this agent is ready to start
     * playing the game. A typical match will not start until all participants
     * have sent this message.
     */
    public void sendInitComplete() {
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.InitComplete);
        msg.setValue(new InitCompleteT());
        sendFlatbufferMsg(msg);
    }

    /**
     * Sends a {@link SetLoadout} message to configure a player's loadout in the game.
     * This can be sent during initialization to set the car's initial loadout, unless the
     * match has already started. If state-setting is enabled, this can also be used during a
     * match to change the car's loadout.
     *
     * @param setLoadout the {@link SetLoadoutT} message to send.
     */
    public void sendSetLoadout(SetLoadoutT setLoadout) {
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.SetLoadout);
        msg.setValue(setLoadout);
        sendFlatbufferMsg(msg);
    }

    /**
     * Sends a {@link MatchComm} message. RLBot will delegate it to the intended receivers.
     * The content of the display field will be displayed in quick chat.
     *
     * @param matchComm the {@link MatchCommT} message to send.
     */
    public void sendMatchComm(MatchCommT matchComm) {
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.MatchComm);
        msg.setValue(matchComm);
        sendFlatbufferMsg(msg);
    }

    /**
     * Sends a player's input message. The car will use these inputs until a new player
     * input message is sent. You can only set the input of players controlled by this process,
     * as indicated in the {@link ControllableInfoT} message received during setup -- unless
     * state-setting is enabled.
     *
     * @param playerInput the {@link PlayerInputT} message to send.
     */
    public void sendPlayerInput(PlayerInputT playerInput) {
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.PlayerInput);
        msg.setValue(playerInput);
        sendFlatbufferMsg(msg);
    }

    /**
     * Sends a {@link DesiredGameState} message to change the game state. This is commonly also
     * referred to as state-setting and must be enabled at the start of the match to be legal.
     * Values set to {@code null} in the desired game state will not be changed.
     * <p>
     * Use state-setting to debug your bot's behavior in specific scenarios or create a meme bot
     * that alters the rules of the game.
     *
     * @param gameState the {@link DesiredGameStateT} to send.
     */
    public void sendGameState(DesiredGameStateT gameState) {
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.DesiredGameState);
        msg.setValue(gameState);
        sendFlatbufferMsg(msg);
    }

    /**
     * Sends a {@link RenderGroup} message to render a group of visual elements in the game.
     * The render group may include various debug render messages, such as lines, rectangles, or text,
     * that will be displayed in-game. Reusing a render group id will remove the previous render group
     * with that id.
     *
     * @param renderGroup the {@link RenderGroupT} to send.
     */
    public void sendRenderGroup(RenderGroupT renderGroup) {
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.RenderGroup);
        msg.setValue(renderGroup);
        sendFlatbufferMsg(msg);
    }

    /**
     * Sends a {@link RemoveRenderGroup} message to remove the render group with the specified id.
     *
     * @param renderGroup the {@link RemoveRenderGroupT} to send.
     */
    public void sendRemoveRenderGroup(RemoveRenderGroupT renderGroup) {
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.RemoveRenderGroup);
        msg.setValue(renderGroup);
        sendFlatbufferMsg(msg);
    }

    /**
     * Sends a command to stop the match in the RLBot server, optionally shutting down the RLBot server.
     *
     * @param shutdownRLBot a boolean flag indicating whether to shut down the RLBot server after stopping the match.
     */
    public void stopMatch(boolean shutdownRLBot) {
        var stop = new StopCommandT();
        stop.setShutdownServer(shutdownRLBot);
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.StopCommand);
        msg.setValue(stop);
        sendFlatbufferMsg(msg);
    }

    /**
     * Starts a match using the provided match configuration.
     *
     * @param matchConfig the {@link MatchConfigurationT}.
     */
    public void startMatch(MatchConfigurationT matchConfig) {
        var msg = new InterfaceMessageUnion();
        msg.setType(InterfaceMessage.MatchConfiguration);
        msg.setValue(matchConfig);
        sendFlatbufferMsg(msg);
        logger.info("Starting match");
        sendInitComplete();
    }

    /**
     * Starts a match using a file path to a match configuration toml file.
     *
     * @param matchConfigPath the file path to the match configuration.
     * @throws FileNotFoundException    if the specified file does not exist.
     * @throws IllegalArgumentException if the specified path is not a regular file.
     */
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

    /**
     * Attempts to launch the RLBot server. If the RLBot server is already running, no new process is launched.
     * This method searches for the server executable first in the current working directory and then in the
     * default installation directory.
     *
     * @return {@code true} if the RLBot server is successfully running or started;
     * {@code false} if the server process cannot be found or launched.
     */
    public boolean tryLaunchRLBotServer() {
        return tryLaunchRLBotServer(true);
    }

    /**
     * Attempts to launch the RLBot server. If the RLBot server is already running, no new process is launched.
     * Otherwise, it searches for the server executable, first in the current working directory
     * if {@code lookInCwd} is true, and then in the default installation directory depending on the
     * provided parameter.
     *
     * @param lookInCwd a boolean flag indicating whether to search for the RLBot server in
     *                  the current working directory first.
     * @return {@code true} if the RLBot server is successfully running or started;
     * {@code false} if the server process cannot be found or launched.
     */
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

    /**
     * Shuts down the RLBot server. If the {@code force} parameter is set to {@code true},
     * it forcibly terminates the server process.
     *
     * @param force a boolean flag indicating whether the server should be forcibly terminated.
     */
    public void shutdownRLBotServer(boolean force) {
        stopMatch(true);
        if (serverProcess != null && force) {
            serverProcess.destroy();
        }
        serverProcess = null;
    }

    /**
     * Run the message handling loop for the RLBot interface on the current thread.
     * All registered {@link RLBotListener} listeners will be notified about incoming messages.
     *
     * @throws RuntimeException if the connection is not established, or if the message
     *                          handling loop is already running.
     */
    @Override
    public void run() {
        run(false);
    }

    /**
     * Starts executing the message handling loop on a background thread.
     * All registered {@link RLBotListener} listeners will be notified about incoming messages.
     *
     * @throws RuntimeException if the connection is not established, or if the message
     *                          handling loop is already running.
     */
    public void runInBackground() {
        run(true);
    }

    /**
     * Executes the message handling loop for the RLBot interface. It can
     * be executed either on the current thread or a background thread.
     * All registered {@link RLBotListener} listeners will be notified about incoming messages.
     *
     * @param inBackgroundThread whether the message handling should be executed on
     *                           a background thread or the calling thread.
     * @throws RuntimeException if the connection is not established, or if the message
     *                          handling loop is already running.
     */
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

    /**
     * The outcome of processing an incoming message.
     */
    public enum MsgHandlingResult {
        /**
         * We received a termination request or a fatal error occurred during processing. We should shut down.
         */
        Termination,
        /**
         * There were no incoming messages to handle.
         */
        NoIncomingMsgs,
        /**
         * A message was handle and more incoming messages may be queued.
         */
        MoreMsgsQueued
    }

    /**
     * Handles the next incoming message from the RLBot server. This method processes a single
     * message based on the specified blocking behavior.
     *
     * @param blocking a boolean flag indicating whether the method should block and wait for
     *                 incoming messages. If {@code true}, the method waits until a message
     *                 is available. If {@code false}, the method only processes available
     *                 messages, returning immediately if no message is available.
     * @return a {@link MsgHandlingResult} indicating the result of the message handling. Possible
     *         return values include:
     *         <ul>
     *             <li>{@code MsgHandlingResult.NoIncomingMsgs} - No messages were available to process.</li>
     *             <li>{@code MsgHandlingResult.MoreMsgsQueued} - A message was processed and more incoming messages may be ready for processing.</li>
     *             <li>{@code MsgHandlingResult.Termination} - A termination request was received or a fatal error occurred.</li>
     *         </ul>
     * @throws RuntimeException if the connection to the RLBot server has not been established.
     */
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

    /**
     * Handles an incoming message and routes it to the appropriate listeners
     * based on the message type.
     *
     * @param packet the message to handle.
     * @return true if the message was successfully processed and does not signal disconnection.
     */
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

    /**
     * @return {@code true} if the message handling loop is running.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Stop the message handling loop. If running in a background thread, the thread will end executing.
     */
    public void stopRunning() {
        isRunning = false;
    }

    /**
     * Connect to the RLBotServer using the standard settings for match hosts
     * that is not interested in ball prediction or match comms.
     * <p>
     * RLBotServer should be running.
     */
    public void connectAsMatchHost() {
        connect("", false, false, true);
    }

    /**
     * Connect to the RLBotServer using the standard settings for match hosts
     * and indicate interest in ball prediction and/or match comms.
     * <p>
     * RLBotServer should be running.
     */
    public void connectAsMatchHost(boolean wantsBallPredictions, boolean wantsComms) {
        connect("", wantsBallPredictions, wantsComms, true);
    }

    /**
     * Connect to the RLBotServer.
     * @param agentId the agent id that this connection represents.
     *                Should be empty for connections that just wants to start or observe matches.
     * @param wantsBallPredictions whether ball prediction messages should be sent to this connection.
     * @param wantsComms whether match comm messages should be sent to this connection.
     * @param outliveMatches whether this connection should be closed when a match ends.
     *                       Should not be used by match participants such as bots or scripts.
     */
    public void connect(String agentId, boolean wantsBallPredictions, boolean wantsComms, boolean outliveMatches) {
        String portRaw = System.getenv("RLBOT_SERVER_PORT");
        int port = portRaw == null ? RLBotInterface.DEFAULT_SERVER_PORT : Integer.parseInt(portRaw);
        connect(agentId, wantsBallPredictions, wantsComms, outliveMatches, port);
    }

    /**
     * Connect to the RLBotServer.
     * @param agentId the agent id that this connection represents.
     *                Should be empty for connections that just wants to start or observe matches.
     * @param wantsBallPredictions whether ball prediction messages should be sent to this connection.
     * @param wantsComms whether match comm messages should be sent to this connection.
     * @param outliveMatches whether this connection should be closed when a match ends.
     *                       Should not be used by match participants such as bots or scripts.
     * @param rlbotServerPort the RLBotServer connection port.
     */
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
                    // Wait a bit before trying again
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

    /**
     * Disconnect from the RLBotServer.
     */
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

    /**
     * @return true if we are connected to the RLBotServer.
     */
    public boolean isConnected() {
        return isConnected;
    }
}
