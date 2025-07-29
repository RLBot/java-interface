package rlbot.commons.protocol;

import java.nio.file.Path;
import java.util.Optional;

public class RLBotServerUtils {

    public static Optional<ProcessHandle> findServerProcess(String name) {
        return ProcessHandle
                .allProcesses()
                .filter(p -> p.info().command().isPresent())
                .filter(p -> p.info().command().orElse("").endsWith(name))
                .findAny();
    }

    public static Path defaultInstallDir() {
        // TODO: Support Linux
        return Path.of(System.getenv("%LOCALAPPDATA%"), "RLBot5", "bin");
    }

    public static String defaultBinName() {
        // TODO: Support Linux
        return "RLBotServer.exe";
    }
}
