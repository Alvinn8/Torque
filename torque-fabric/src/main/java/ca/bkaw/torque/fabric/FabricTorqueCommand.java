package ca.bkaw.torque.fabric;

import ca.bkaw.torque.TorqueCommand;
import ca.bkaw.torque.fabric.platform.FabricPlatform;
import ca.bkaw.torque.fabric.platform.FabricPlayer;
import ca.bkaw.torque.fabric.platform.FabricWorld;
import ca.bkaw.torque.platform.Identifier;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
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
                        .then(
                            Commands.argument("vehicle", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                    this.handler().getVehicleTypeIdentifiers().stream()
                                        .map(identifier -> ResourceLocation.fromNamespaceAndPath(identifier.namespace(), identifier.key())),
                                    builder
                                ))
                                .executes(ctx -> {
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "vehicle");
                                    Identifier identifier;
                                    try {
                                        identifier = new Identifier(id.getNamespace(), id.getPath());
                                    } catch (IllegalArgumentException e) {
                                        ctx.getSource().sendFailure(
                                            net.minecraft.network.chat.Component.literal("Unknown vehicle type: " + id)
                                        );
                                        return 0;
                                    }
                                    Vec3 position = ctx.getSource().getPosition();
                                    boolean success = this.handler().summon(
                                        new FabricWorld(ctx.getSource().getLevel()),
                                        new Vector3d(position.x(), position.y(), position.z()),
                                        identifier
                                    );
                                    if (!success) {
                                        ctx.getSource().sendFailure(
                                            net.minecraft.network.chat.Component.literal("Unknown vehicle type: " + identifier)
                                        );
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
                                    this.handler().test(
                                        new FabricPlayer(ctx.getSource().getPlayerOrException()),
                                        number
                                    );
                                    return 1;
                                })
                        )
                )
                .then(
                    Commands.literal("resourcepack")
                        .executes(ctx -> {
                            this.handler().resourcePack(new FabricPlayer(ctx.getSource().getPlayerOrException()));
                            return 1;
                        })
                ).then(
                    Commands.literal("reload")
                        .executes(ctx -> {
                            this.handler().reload();
                            return 1;
                        })
                ).then(
                    Commands.literal("tick")
                        .then(
                            Commands.literal("freeze")
                                .executes(ctx -> {
                                    this.handler().tickFreeze();
                                    ctx.getSource().sendSystemMessage(
                                        net.minecraft.network.chat.Component.literal("Vehicle ticking has been frozen.")
                                    );
                                    return 1;
                                })
                        )
                        .then(
                            Commands.literal("step")
                                .executes(ctx -> {
                                    this.handler().tickStep(1);
                                    ctx.getSource().sendSystemMessage(
                                        net.minecraft.network.chat.Component.literal("Stepped 1 tick.")
                                    );
                                    return 1;
                                })
                                .then(
                                    Commands.argument("amount", IntegerArgumentType.integer(1, 1000))
                                        .executes(ctx -> {
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            this.handler().tickStep(amount);
                                            ctx.getSource().sendSystemMessage(
                                                net.minecraft.network.chat.Component.literal("Stepped " + amount + " ticks.")
                                            );
                                            return 1;
                                        })
                                )
                        )
                        .then(
                            Commands.literal("unfreeze")
                                .executes(ctx -> {
                                    this.handler().tickUnfreeze();
                                    ctx.getSource().sendSystemMessage(
                                        net.minecraft.network.chat.Component.literal("Vehicle ticking has been unfrozen.")
                                    );
                                    return 1;
                                })
                        )
                        .then(
                            Commands.literal("status")
                                .executes(ctx -> {
                                    String status = this.handler().getTickStatus();
                                    ctx.getSource().sendSystemMessage(
                                        net.minecraft.network.chat.Component.literal(status)
                                    );
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
                                    ctx.getSource().sendSystemMessage(
                                        net.minecraft.network.chat.Component.literal("Enabled debug mode.")
                                    );
                                    return 1;
                                })
                        )
                        .then(
                            Commands.literal("disable")
                                .executes(ctx -> {
                                    this.handler().debugDisable();
                                    ctx.getSource().sendSystemMessage(
                                        net.minecraft.network.chat.Component.literal("Disabled debug mode.")
                                    );
                                    return 1;
                                })
                        )
                )
        );
    }
}
