package com.minciallo.dustbin.client.render;

import com.minciallo.dustbin.MinCialloDustbin;
import com.minciallo.dustbin.block.DustbinBlock;
import com.minciallo.dustbin.block.DustbinBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the trash-bin lid and animates it on open/close.
 *
 * <p>Why a block entity renderer at all: vanilla block models cannot animate their
 * vertices. Only a block entity renderer can rebuild geometry per frame, so the lid
 * lives here while the (static) body stays a regular block model.
 *
 * <p>The block itself only reports a boolean "open" flag through its block state. The
 * renderer keeps a per-position progress value in {@code [0,1]} and eases it towards
 * the flag every frame, which produces the smooth swing instead of a hard snap.
 *
 * <p>Geometry mirrors {@code models/block/dustbin.json} exactly, so the item form and
 * the in-world form are the same bin. The lid pivots on its back-bottom edge.
 */
public class DustbinBlockEntityRenderer implements BlockEntityRenderer<DustbinBlockEntity, DustbinRenderState> {
	/** Top plate of the lid. */
	private static final Identifier LID_TOP = MinCialloDustbin.id("entity/dustbin/lid_top");
	/** Sides and underside of the lid. */
	private static final Identifier LID_SIDE = MinCialloDustbin.id("entity/dustbin/lid_side");
	/** The ribbed lift tab sitting on top of the lid. */
	private static final Identifier LID_HANDLE = MinCialloDustbin.id("entity/dustbin/lid_handle");

	/** How far the lid swings open, in degrees. */
	private static final float MAX_OPEN_ANGLE = 95.0f;
	/** Opening/closing speed in progress-units per second. */
	private static final float ANIMATION_SPEED = 5.0f;
	/** Largest time step honoured, so a stutter cannot make the lid teleport. */
	private static final float MAX_STEP_SECONDS = 0.1f;

	/** Lid box, in block units (0..16). Kept in sync with the block model. */
	private static final float LID_X0 = 2.5f / 16.0f;
	private static final float LID_X1 = 13.5f / 16.0f;
	private static final float LID_Z0 = 2.5f / 16.0f;
	private static final float LID_Z1 = 13.5f / 16.0f;
	private static final float LID_Y0 = 11.0f / 16.0f;
	private static final float LID_Y1 = 12.5f / 16.0f;

	/** Lift tab on the lid. */
	private static final float TAB_X0 = 6.0f / 16.0f;
	private static final float TAB_X1 = 10.0f / 16.0f;
	private static final float TAB_Z0 = 7.0f / 16.0f;
	private static final float TAB_Z1 = 9.0f / 16.0f;
	private static final float TAB_Y0 = 12.5f / 16.0f;
	private static final float TAB_Y1 = 13.5f / 16.0f;

	/** Current animation progress per block position. */
	private final Map<BlockPos, Float> progress = new HashMap<>();
	/** Wall-clock timestamp of the previous update per block position. */
	private final Map<BlockPos, Long> lastUpdate = new HashMap<>();

	public DustbinBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
		// No resources to bake: the lid is built from raw vertices.
	}

	@Override
	public DustbinRenderState createRenderState() {
		return new DustbinRenderState();
	}

	@Override
	public void extractRenderState(DustbinBlockEntity blockEntity, DustbinRenderState state, float partialTick,
			Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, crumblingOverlay);

		BlockPos pos = blockEntity.getBlockPos();
		float target = blockEntity.getBlockState().getValue(DustbinBlock.OPEN) ? 1.0f : 0.0f;

		long now = Util.getMillis();
		float current = progress.getOrDefault(pos, target);
		long previous = lastUpdate.getOrDefault(pos, now);
		lastUpdate.put(pos, now);

		float step = ANIMATION_SPEED * Math.min((now - previous) / 1000.0f, MAX_STEP_SECONDS);
		float delta = target - current;

		if (Math.abs(delta) <= step) {
			current = target;
		} else {
			current += Math.copySign(step, delta);
		}

		if (current == target && target == 0.0f) {
			// Fully closed: drop the bookkeeping so the maps do not grow forever.
			progress.remove(pos);
			lastUpdate.remove(pos);
		} else {
			progress.put(pos, current);
		}

		state.openness = current;
	}

	@Override
	public void submit(DustbinRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lightCoords;
		int overlay = OverlayTexture.NO_OVERLAY;

		poseStack.pushPose();

		// Hinge on the back-bottom edge of the lid, so it tips up and back like a real bin.
		poseStack.translate(0.0f, LID_Y0, LID_Z0);
		poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(-MAX_OPEN_ANGLE * state.openness)));
		poseStack.translate(0.0f, -LID_Y0, -LID_Z0);

		emitBox(poseStack, collector, LID_X0, LID_Y0, LID_Z0, LID_X1, LID_Y1, LID_Z1, LID_TOP, LID_SIDE, LID_SIDE, light, overlay);
		emitBox(poseStack, collector, TAB_X0, TAB_Y0, TAB_Z0, TAB_X1, TAB_Y1, TAB_Z1, LID_HANDLE, LID_HANDLE, LID_HANDLE, light, overlay);

		poseStack.popPose();
	}

	/** Emits all six faces of an axis-aligned box, one submission per texture. */
	private static void emitBox(PoseStack poseStack, SubmitNodeCollector collector,
			float x0, float y0, float z0, float x1, float y1, float z1,
			Identifier top, Identifier side, Identifier bottom, int light, int overlay) {
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(top), (pose, consumer) -> quad(
				consumer, pose,
				new float[][] {
						{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}
				},
				0.0f, 1.0f, 0.0f, light, overlay));

		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(bottom), (pose, consumer) -> quad(
				consumer, pose,
				new float[][] {
						{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}
				},
				0.0f, -1.0f, 0.0f, light, overlay));

		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(side), (pose, consumer) -> {
			// North (-Z) and south (+Z).
			quad(consumer, pose, new float[][] {
					{x1, y1, z0}, {x0, y1, z0}, {x0, y0, z0}, {x1, y0, z0}
			}, 0.0f, 0.0f, -1.0f, light, overlay);
			quad(consumer, pose, new float[][] {
					{x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}, {x0, y0, z1}
			}, 0.0f, 0.0f, 1.0f, light, overlay);
			// West (-X) and east (+X).
			quad(consumer, pose, new float[][] {
					{x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}, {x0, y0, z0}
			}, -1.0f, 0.0f, 0.0f, light, overlay);
			quad(consumer, pose, new float[][] {
					{x1, y1, z1}, {x1, y1, z0}, {x1, y0, z0}, {x1, y0, z1}
			}, 1.0f, 0.0f, 0.0f, light, overlay);
		});
	}

	/**
	 * Emits a single quad; the four corners map to the texture corners
	 * (0,0) - (1,0) - (1,1) - (0,1) in the order given.
	 */
	private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float[][] corners,
			float normalX, float normalY, float normalZ, int light, int overlay) {
		float[][] uvs = {{0.0f, 0.0f}, {1.0f, 0.0f}, {1.0f, 1.0f}, {0.0f, 1.0f}};

		for (int i = 0; i < 4; i++) {
			float[] corner = corners[i];
			consumer.addVertex(pose, corner[0], corner[1], corner[2])
					.setColor(255, 255, 255, 255)
					.setUv(uvs[i][0], uvs[i][1])
					.setOverlay(overlay)
					.setLight(light)
					.setNormal(pose, normalX, normalY, normalZ);
		}
	}
}
