package com.minciallo.dustbin.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

public class DustbinBlock extends Block implements EntityBlock {
	/**
	 * 盖子是否处于打开状态。
	 *
	 * <p>该属性只承担"把开/关同步到客户端"的职责：方块模型本身永远只画桶身，
	 * 盖子由 {@code DustbinBlockEntityRenderer} 读取本属性后逐帧插值渲染，
	 * 从而得到平滑的开盖动画。
	 */
	public static final BooleanProperty OPEN = BooleanProperty.create("open");

	/**
	 * 垃圾桶不是满方块，碰撞箱与选中框必须跟模型一致（桶身 3~13、含盖子到 12.5），
	 * 否则会出现"隔空挡住玩家"的违和感。顶部小提手不参与碰撞。
	 */
	private static final VoxelShape SHAPE = Block.box(2.5, 0.0, 2.5, 13.5, 12.5, 13.5);

	public DustbinBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(OPEN, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(OPEN);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DustbinBlockEntity blockEntity) {
			// 打开界面的同时掀开盖子；关闭界面时由 DustbinMenu#removed 合上。
			blockEntity.setOpen(true);
			player.openMenu(blockEntity);
		}
		return InteractionResult.SUCCESS;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DustbinBlockEntity(pos, state);
	}
}
