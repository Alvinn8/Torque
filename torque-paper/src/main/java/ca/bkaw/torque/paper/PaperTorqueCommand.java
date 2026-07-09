package ca.bkaw.torque.paper;

import ca.bkaw.torque.TorqueCommand;
import ca.bkaw.torque.paper.platform.PaperPlatform;
import ca.bkaw.torque.paper.platform.PaperPlayer;
import ca.bkaw.torque.paper.platform.PaperWorld;
import ca.bkaw.torque.platform.Identifier;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
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
                        .then(
                            Commands.argument("vehicle", ArgumentTypes.namespacedKey())
                                .suggests((ctx, builder) -> {
                                    String remaining = builder.getRemainingLowerCase();
                                    for (Identifier identifier : this.handler().getVehicleTypeIdentifiers()) {
                                        if (identifier.toString().startsWith(remaining) || identifier.key().startsWith(remaining)) {
                                            builder.suggest(identifier.toString());
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    NamespacedKey key = ctx.getArgument("vehicle", NamespacedKey.class);
                                    Identifier identifier;
                                    try {
                                        identifier = new Identifier(key.getNamespace(), key.getKey());
                                    } catch (IllegalArgumentException e) {
                                        ctx.getSource().getSender().sendMessage("Unknown vehicle type: " + key);
                                        return 0;
                                    }
                                    Location location = ctx.getSource().getLocation();
                                    boolean success = this.handler().summon(
                                        new PaperWorld(location.getWorld()),
                                        new Vector3d(location.getX(), location.getY(), location.getZ()),
                                        identifier
                                    );
                                    if (!success) {
                                        ctx.getSource().getSender().sendMessage("Unknown vehicle type: " + identifier);
                                        return 0;
                                    }
                                    return 1;
                                })
                        )
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
