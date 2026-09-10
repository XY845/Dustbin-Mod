package com.minciallo.dustbin.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

/**
 * Per-frame snapshot handed to {@link DustbinBlockEntityRenderer}.
 */
public class DustbinRenderState extends BlockEntityRenderState {
	/** Lid opening progress: 0 = fully closed, 1 = fully open. */
	public float openness;

	/**
	 * Horizontal facing of the block, copied from {@code DustbinBlock.FACING}.
	 * The lid hinge sits on the opposite side, so the bin always opens away from
	 * whoever placed it.
	 */
	public Direction facing = Direction.SOUTH;
}
