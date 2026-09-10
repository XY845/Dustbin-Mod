package com.minciallo.dustbin.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/**
 * Per-frame snapshot handed to {@link DustbinBlockEntityRenderer}.
 */
public class DustbinRenderState extends BlockEntityRenderState {
	/** Lid opening progress: 0 = fully closed, 1 = fully open. */
	public float openness;
}
