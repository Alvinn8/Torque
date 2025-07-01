package ca.bkaw.torque.platform;

public interface BlockState {
    /**
     * Check if this block is water, or a waterlogged block.
     *
     * @return Whether this block is waterlogged or not.
     */
    boolean isWaterlogged();
}
