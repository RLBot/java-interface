package rlbot.commons.protocol;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Helper functions to find the RLBotServer.
 */
public class RLBotServerUtils {

    /**
     * Finds a process with the specified name, if it exists.
     *
     * @param name the name of the process to search for.
     * @return an {@code Optional<ProcessHandle>} containing the process handle if a matching process is found,
     *         or an empty {@code Optional} if no matching process is found.
     */
    public static Optional<ProcessHandle> findServerProcess(String name) {
        return ProcessHandle
                .allProcesses()
                .filter(p -> p.info().command().isPresent())
                .filter(p -> p.info().command().orElse("").endsWith(name))
                .findAny();
    }

    /**
     * @return the default installation directory where the RLBotServer binary can be found on this platform.
     */
    public static Path defaultInstallDir() {
        // TODO: Support Linux
        return Path.of(System.getenv("%LOCALAPPDATA%"), "RLBot5", "bin");
    }

    /**
     * @return the default name of the RLBotServer binary on this platform.
     */
    public static String defaultBinName() {
        // TODO: Support Linux
        return "RLBotServer.exe";
    }
}
