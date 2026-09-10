package com.minciallo.dustbin.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
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
	 * 垃圾桶的正面 —— 也就是开盖时朝向玩家的那一侧。
	 *
	 * <p>取值语义与箱子一致：放置时正面朝向玩家，即
	 * {@code getHorizontalDirection().getOpposite()}。盖子的铰链在背面的下沿，
	 * 所以掀盖时盖子朝远离玩家的一侧翻起，和现实里的翻盖垃圾桶一致。
	 *
	 * <p><b>模型空间的默认正面是南</b>（铰链在北，自由边在南）。由此推出两处必须成对修改：
	 * <ul>
	 * <li>blockstate 的 {@code y} 值 = {@code facing.toYRot()}（南 0、西 90、北 180、东 270）
	 * <li>渲染器里的旋转 = {@code Axis.YP.rotationDegrees(-facing.toYRot())}
	 * </ul>
	 * 且渲染器必须绕<b>方块中心</b>（0.5, 0.5, 0.5）旋转 —— 方块模型的 y 旋转就是这个枢轴，
	 * 若改用方块原点作枢轴，盖子会整体偏移半格。详见 {@code DustbinBlockEntityRenderer#submit}。
	 */
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	/**
	 * 垃圾桶不是满方块，碰撞箱与选中框必须跟模型一致（桶身 3~13、含盖子到 12.5），
	 * 否则会出现"隔空挡住玩家"的违和感。顶部小提手不参与碰撞。
	 *
	 * <p>形状关于 X/Z 轴对称，因此不需要按朝向分派不同的 VoxelShape。
	 */
	private static final VoxelShape SHAPE = Block.box(2.5, 0.0, 2.5, 13.5, 12.5, 13.5);

	public DustbinBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any()
				.setValue(OPEN, false)
				.setValue(FACING, Direction.SOUTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(OPEN, FACING);
	}

	/**
	 * 放置时让正面朝向玩家，与箱子同规则：玩家看向的方向取反，即"面朝我"。
	 * 于是放下去以后，盖子朝远离玩家的方向掀开。
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
