package ca.bkaw.torque.fabric.platform;

import ca.bkaw.torque.platform.ItemDisplay;
import ca.bkaw.torque.platform.ItemStack;
import net.minecraft.world.entity.Display;

public record FabricItemDisplay(Display.ItemDisplay entity) implements ItemDisplay {
    @Override
    public void setItem(ItemStack item) {
        this.entity.getSlot(0).set(((FabricItemStack) item).itemStack());
    }
}
