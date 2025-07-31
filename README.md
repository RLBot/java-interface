# RLBot Java Interface

A library to create Rocket League bots by interfacing with the [RLBot](https://rlbot.org/) v5 framework through Java.

## Example Usage

For general info about RLBot, see https://wiki.rlbot.org/.

To depend on this project, add this to your project's `pom.xml`:

```
<dependencies>
    <dependency>
        <groupId>org.rlbot.commons</groupId>
        <artifactId>java-interface</artifactId>
        <version>...</version>
    </dependency>
</dependencies>
```

Find the newest version [here](https://central.sonatype.com/artifact/org.rlbot.commons/java-interface).

### Running a bot

```java
package org.rlbot.commons;

import rlbot.commons.agents.BotManager;
import rlbot.commons.protocol.RLBotInterface;

public class Main {
    public static void main(String[] args) {
        var rlbot = new RLBotInterface();
        var botManager = new BotManager(rlbot, "myname/examplebot/v0.1", ExampleBot::new);
        botManager.run();
    }
}
```

```java
package org.rlbot.commons;

import rlbot.commons.agents.Bot;
import rlbot.flat.*;
import rlbot.commons.protocol.RLBotInterface;

public class ExampleBot implements Bot {

    public static final float PI = (float) Math.PI;

    private final RLBotInterface rlbot;
    public final int index;
    public final int team;
    public final String name;
    public final String agentId;
    public final MatchConfigurationT matchConfig;
    public final FieldInfoT fieldInfo;

    public ExampleBot(RLBotInterface rlbot, int index, int team, String name, String agentId, MatchConfigurationT matchConfig, FieldInfoT fieldInfo) {
        this.rlbot = rlbot;
        this.index = index;
        this.team = team;
        this.name = name;
        this.agentId = agentId;
        this.matchConfig = matchConfig;
        this.fieldInfo = fieldInfo;
    }

    @Override
    public PlayerLoadoutT getInitialLoadout() {
        // Returning null means we will use the loadout_file from the bot.toml config.
        // Alternatively, we could programmatically define our loadout here.
        return null;
    }

    @Override
    public ControllerStateT getOutput(GamePacketT packet, BallPredictionT ballPrediction) {
        // Decide what to do this tick

        var controller = new ControllerStateT();
        controller.setThrottle(1f);

        if (packet.getBalls().length > 0) {

            var player = packet.getPlayers()[index];
            var playerLoc = player.getPhysics().getLocation();
            var ballLoc = packet.getBalls()[0].getPhysics().getLocation();
            var curYaw = player.getPhysics().getRotation().getYaw();
            var desiredYaw = (float) Math.atan2(ballLoc.getY() - playerLoc.getY(), ballLoc.getX() - playerLoc.getX());
            var correction = (desiredYaw - curYaw + 2 * PI) % (2 * PI) - PI;
            controller.setSteer(-correction);
        }

        return controller;
    }

    @Override
    public void onMatchCommReceived(MatchCommT comm) {
        // Somebody sent us a message
    }

    @Override
    public void onRetire() {
        // We are shutting down
    }
}
```

Remember to create a [`bot.toml`](https://wiki.rlbot.org/v5/botmaking/config-files/#bot-script-config-files) file for you bot.

Use a `HivemindManager` for finer control over hivemind bots.

### Starting a match

```java
package org.rlbot;

import rlbot.flat.GamePacketT;
import rlbot.flat.MatchPhase;
import rlbot.protocol.ConnectSettings;
import rlbot.commons.protocol.RLBotListenerAdapter;
import rlbot.commons.protocol.RLBotInterface;

import java.io.FileNotFoundException;
import java.nio.file.Paths;

public class Main extends RLBotListenerAdapter {

    int lastMatchPhase = MatchPhase.Inactive;

    public static void main(String[] args) throws FileNotFoundException {
        var rlbot = new RLBotInterface();
        // Ensure the rlbot server is running
        rlbot.tryLaunchRLBotServer();
        rlbot.connectAsMatchHost();
        // Start a match and listen to messages
        rlbot.startMatch(Paths.get("match.toml"));
        rlbot.addListener(new Main());
        rlbot.run();
    }

    @Override
    public void onGamePacket(GamePacketT packet) {
        // Process incoming packets here
        if (lastMatchPhase != packet.getMatchInfo().getMatchPhase()) {
            lastMatchPhase = packet.getMatchInfo().getMatchPhase();
            var name = MatchPhase.name(lastMatchPhase);
            System.out.println("MatchPhase changed: " + name);
        }
    }
}
```

Remember to define create a [`match.toml`](https://wiki.rlbot.org/v5/botmaking/config-files/#match-config-files) or alternatively construct a `MatchConfigurationT` programmatically and pass that to `rlbot.startMatch(..)`.

## Maintenance

### Setup

Use `git submodule update --init` to update/initialize the https://github.com/RLBot/flatbuffers-schema submodule.

The flatbuffer classes will be generated automatically when building.

### Testing

You can use `mvn clean install` to install the interface in your local maven repo.
This allows you to use it in a different project.

### Deployment

Prerequisites:
- Have publisher status for the `org.rlbot.commons` namespace at [MavenCentral](https://central.sonatype.com/).
- Have your generated user token setup in `%UserProfile%/.m2/settings.xml`:
```xml
<settings>
    <servers>
        <server>
            <id>central</id>
            <username>...</username>
            <password>...</password>
        </server>
    </servers>
</settings>
```

Steps:
- Update `<version>` in `pom.xml`. Use postfix `-SNAPSHOT` to upload a beta version.
- Run `mvn deploy`.
