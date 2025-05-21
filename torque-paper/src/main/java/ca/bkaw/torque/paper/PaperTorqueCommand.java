package ca.bkaw.torque.paper;

import ca.bkaw.torque.TorqueCommand;
import ca.bkaw.torque.paper.platform.PaperPlatform;
import ca.bkaw.torque.paper.platform.PaperWorld;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

public class PaperTorqueCommand {
    private final @NotNull PaperPlatform platform;

    public PaperTorqueCommand(@NotNull PaperPlatform platform) {
        this.platform = platform;
    }

    private @NotNull TorqueCommand handler() {
        TorqueCommand torqueCommand = this.platform.getTorqueCommand();
        if (torqueCommand == null) {
            throw new IllegalStateException("TorqueCommand has not been setup");
        }
        return torqueCommand;
    }

    public void register(Commands commands) {
        commands.register(
            Commands.literal("torque")
                .then(
                    Commands.literal("summon")
                        .executes(ctx -> {
                            Location location = ctx.getSource().getLocation();
                            this.handler().summon(
                                new PaperWorld(location.getWorld()),
                                new Vector3d(location.getX(), location.getY(), location.getZ())
                            );
                            return 1;
                        })
                )
                .build()
        );
    }
}
