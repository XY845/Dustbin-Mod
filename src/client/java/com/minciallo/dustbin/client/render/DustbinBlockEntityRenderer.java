package com.minciallo.dustbin.client.render;

import com.minciallo.dustbin.MinCialloDustbin;
import com.minciallo.dustbin.block.DustbinBlock;
import com.minciallo.dustbin.block.DustbinBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

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
	/**
	 * Lid textures. The lid is drawn twice, by two different mechanisms, and the two
	 * mechanisms impose <em>different</em> rules on the same files:
	 *
	 * <ul>
	 * <li><b>Here (block entity renderer)</b> — {@link RenderTypes#entityCutout} hands the
	 * identifier straight to {@code TextureManager.getTexture}, with no prefixing or
	 * suffixing. So the path must be <em>full</em>: a {@code textures/} prefix and a
	 * {@code .png} suffix are both required. A short path such as
	 * {@code block/dustbin_lid_top} resolves to nothing and renders as the
	 * missing-texture checkerboard.
	 * <li><b>The block model</b> ({@code models/block/dustbin.json}), used for the item
	 * icon — model textures are stitched into the {@code minecraft:blocks} atlas, whose
	 * only directory source is {@code textures/block}. A file anywhere else (for example
	 * {@code textures/entity/...}) is never stitched, so the model cannot resolve it and
	 * the item form renders as the checkerboard too.
	 * </ul>
	 *
	 * <p>Placing the files under {@code textures/block/} is therefore load-bearing in both
	 * worlds: it satisfies the atlas, and this full path satisfies the direct binding.
	 * Moving them out of {@code textures/block/} breaks the item icon even though the
	 * in-world lid keeps working.
	 */
	private static final Identifier LID_TOP = MinCialloDustbin.id("textures/block/dustbin_lid_top.png");
	/** Sides and underside of the lid. */
	private static final Identifier LID_SIDE = MinCialloDustbin.id("textures/block/dustbin_lid_side.png");
	/** The ribbed lift tab sitting on top of the lid. */
	private static final Identifier LID_HANDLE = MinCialloDustbin.id("textures/block/dustbin_lid_handle.png");

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
		state.facing = blockEntity.getBlockState().getValue(DustbinBlock.FACING);
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

		// 1) 先按方块朝向摆正整个盖子。
		//
		// 枢轴必须是方块中心 (0.5, 0.5, 0.5)：方块模型的 y 旋转就是这个枢轴，
		// 若改用方块原点，盖子会整体平移半格，和桶身错位。
		//
		// 符号：blockstate 里写 y = facing.toYRot()，而 y 值 N 等价于代码中的
		// rotationDegrees(-N)（原版 ChestRenderer 即如此）。两处必须成对修改，
		// 只改一边会出现"桶身转了、盖子没转"。
		poseStack.translate(0.5f, 0.5f, 0.5f);
		poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot()));
		poseStack.translate(-0.5f, -0.5f, -0.5f);

		// 2) 再绕铰链掀盖：铰链在盖子背面的下沿，于是盖子向远离玩家的一侧翻起。
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
