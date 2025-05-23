package ca.bkaw.torque.fabric;

import ca.bkaw.torque.TorqueCommand;
import ca.bkaw.torque.fabric.platform.FabricPlatform;
import ca.bkaw.torque.fabric.platform.FabricWorld;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

public class FabricTorqueCommand {
    private final @NotNull FabricPlatform platform;

    public FabricTorqueCommand(@NotNull FabricPlatform platform) {
        this.platform = platform;
    }

    private @NotNull TorqueCommand handler() {
        TorqueCommand torqueCommand = this.platform.getTorqueCommand();
        if (torqueCommand == null) {
            throw new IllegalStateException("TorqueCommand has not been setup");
        }
        return torqueCommand;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("torque")
                .then(
                    Commands.literal("summon")
                        .executes(ctx -> {
                            Vec3 position = ctx.getSource().getPosition();
                            this.handler().summon(
                                new FabricWorld(ctx.getSource().getLevel()),
                                new Vector3d(position.x(), position.y(), position.z())
                            );
                            return 1;
                        })
                )
        );
    }
}
