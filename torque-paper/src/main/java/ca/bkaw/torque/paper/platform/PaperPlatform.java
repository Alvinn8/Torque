package ca.bkaw.torque.paper.platform;

import ca.bkaw.torque.TorqueCommand;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.ItemStack;
import ca.bkaw.torque.platform.Platform;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PaperPlatform implements Platform {
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
        Material material = Material.STICK;
        org.bukkit.inventory.ItemStack itemStack = org.bukkit.inventory.ItemStack.of(material);
        NamespacedKey modelKey = new NamespacedKey(modelIdentifier.namespace(), modelIdentifier.key());
        itemStack.editMeta(meta -> meta.setItemModel(modelKey));
        return new PaperItemStack(itemStack);
    }
}
