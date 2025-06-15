package ca.bkaw.torque.paper.platform;

import ca.bkaw.torque.platform.ItemStack;
import org.joml.Matrix4f;

public record PaperItemDisplay(org.bukkit.entity.ItemDisplay entity) implements ca.bkaw.torque.platform.ItemDisplay {
    @Override
    public void setItem(ItemStack item) {
        this.entity.setItemStack(((PaperItemStack) item).itemStack());
    }

    @Override
    public void setTransformation(Matrix4f affineTransformMatrix) {
        this.entity.setTransformationMatrix(affineTransformMatrix);
    }
}
