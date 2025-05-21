package ca.bkaw.torque.fabric.platform;

import ca.bkaw.torque.TorqueCommand;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.ItemStack;
import ca.bkaw.torque.platform.Platform;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FabricPlatform implements Platform {
    private @Nullable TorqueCommand torqueCommand;

    @Override
    public void setup(@NotNull TorqueCommand torqueCommand) {
        this.torqueCommand = torqueCommand;
    }

    public @Nullable TorqueCommand getTorqueCommand() {
        return this.torqueCommand;
    }

    @Override
    public ItemStack createModelItem(@NotNull Identifier modelIdentifier) {
        net.minecraft.world.item.ItemStack itemStack = new net.minecraft.world.item.ItemStack(Items.STICK);
        ResourceLocation modelKey = ResourceLocation.fromNamespaceAndPath(modelIdentifier.namespace(), modelIdentifier.key());
        itemStack.applyComponents(DataComponentMap.builder().set(DataComponents.ITEM_MODEL, modelKey).build());
        return new FabricItemStack(itemStack);
    }
}
