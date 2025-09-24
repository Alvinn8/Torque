package ca.bkaw.torque.paper;

import ca.bkaw.torque.TorqueCommand;
import ca.bkaw.torque.paper.platform.PaperPlatform;
import ca.bkaw.torque.paper.platform.PaperPlayer;
import ca.bkaw.torque.paper.platform.PaperWorld;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.entity.Player;
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
                .then(
                    Commands.literal("test")
                        .then(
                            Commands.argument("number", IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> {
                                    int number = IntegerArgumentType.getInteger(ctx, "number");
                                    if (ctx.getSource().getSender() instanceof Player player) {
                                        this.handler().test(
                                            new PaperPlayer(player),
                                            number
                                        );
                                    }
                                    return 1;
                                })
                        )
                )
                .then(
                    Commands.literal("resourcepack")
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            if (source.getSender() instanceof Player player) {
                                this.handler().resourcePack(new PaperPlayer(player));
                            }
                            return 1;
                        })
                )
                .then(
                    Commands.literal("reload")
                        .executes(ctx -> {
                            this.handler().reload();
                            return 1;
                        })
                )
                .then(
                    Commands.literal("tick")
                        .then(
                            Commands.literal("freeze")
                                .executes(ctx -> {
                                    this.handler().tickFreeze();
                                    ctx.getSource().getSender().sendMessage("Vehicle ticking has been frozen.");
                                    return 1;
                                })
                        )
                        .then(
                            Commands.literal("step")
                                .executes(ctx -> {
                                    this.handler().tickStep(1);
                                    ctx.getSource().getSender().sendMessage("Stepped 1 tick.");
                                    return 1;
                                })
                                .then(
                                    Commands.argument("amount", IntegerArgumentType.integer(1, 1000))
                                        .executes(ctx -> {
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            this.handler().tickStep(amount);
                                            ctx.getSource().getSender().sendMessage("Stepped " + amount + " ticks.");
                                            return 1;
                                        })
                                )
                        )
                        .then(
                            Commands.literal("unfreeze")
                                .executes(ctx -> {
                                    this.handler().tickUnfreeze();
                                    ctx.getSource().getSender().sendMessage("Vehicle ticking has been unfrozen.");
                                    return 1;
                                })
                        )
                        .then(
                            Commands.literal("status")
                                .executes(ctx -> {
                                    String status = this.handler().getTickStatus();
                                    ctx.getSource().getSender().sendMessage(status);
                                    return 1;
                                })
                        )
                )
                .then(
                    Commands.literal("debug")
                        .then(
                            Commands.literal("enable")
                                .executes(ctx -> {
                                    this.handler().debugEnable();
                                    ctx.getSource().getSender().sendMessage("Enabled debug mode.");
                                    return 1;
                                })
                        )
                        .then(
                            Commands.literal("disable")
                                .executes(ctx -> {
                                    this.handler().debugDisable();
                                    ctx.getSource().getSender().sendMessage("Disabled debug mode.");
                                    return 1;
                                })
                        )
                )
                .build()
        );
    }
}
