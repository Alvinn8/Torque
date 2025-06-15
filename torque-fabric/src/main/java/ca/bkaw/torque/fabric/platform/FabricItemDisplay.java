package ca.bkaw.torque.fabric.platform;

import ca.bkaw.torque.platform.ItemDisplay;
import ca.bkaw.torque.platform.ItemStack;
import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.joml.Matrix4f;

public record FabricItemDisplay(Display.ItemDisplay entity) implements ItemDisplay {
    @Override
    public void setItem(ItemStack item) {
        this.entity.getSlot(0).set(((FabricItemStack) item).itemStack());
    }

    @Override
    public void setTransformation(Matrix4f affineTransformMatrix) {
        this.entity.setTransformation(new Transformation(affineTransformMatrix));
    }
}
