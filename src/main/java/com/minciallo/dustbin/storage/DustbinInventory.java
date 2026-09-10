package com.minciallo.dustbin.storage;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * 共享垃圾桶库存：54 格，每种物品只占用一个格子。
 *
 * <p>堆叠规则（覆写 {@link #addItem(ItemStack)} 实现）：
 * <ul>
 *   <li>每格堆叠上限 = 物品自身的 {@code maxStackSize}（鸡蛋 16、石头 64 等）。</li>
 *   <li>同种物品（相同 id + 组件）永远只合并进 <b>一个</b> 格子，绝不跨格。</li>
 *   <li>超过该格上限的多余数量<b>直接丢弃</b>（如 65 个石头 → 保留 64、丢弃 1）。</li>
 * </ul>
 *
 * <p>注：26.2 引擎硬约束物品 count ≤ {@code Item.ABSOLUTE_MAX_STACK_SIZE}=99，
 * 默认堆叠上限为 64，本实现不会产生超限 count，序列化安全。
 */
public class DustbinInventory extends SimpleContainer {
	public static final int SLOT_COUNT = 54;

	public DustbinInventory() {
		super(SLOT_COUNT);
	}

	@Override
	public ItemStack addItem(ItemStack stack) {
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		int maxStack = stack.getMaxStackSize();

		// 1) 先找同种物品的现有格，尽可能合并（多余部分会被丢弃）。
		for (int i = 0; i < getContainerSize(); i++) {
			ItemStack slot = getItem(i);
			if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
				int space = maxStack - slot.getCount();
				if (space > 0) {
					slot.grow(Math.min(space, stack.getCount()));
					setChanged();
				}
				// 该格是这种物品的唯一格；剩余部分直接丢弃（不跨格、不返回）。
				return ItemStack.EMPTY;
			}
		}

		// 2) 没有现有格：放入第一个空位，数量截断到堆叠上限，多余丢弃。
		for (int i = 0; i < getContainerSize(); i++) {
			ItemStack slot = getItem(i);
			if (slot.isEmpty()) {
				int toAdd = Math.min(maxStack, stack.getCount());
				setItem(i, stack.copyWithCount(toAdd));
				setChanged();
				return ItemStack.EMPTY;
			}
		}

		// 3) 没有空位且没有同类格：装不下，返回原样（由调用方决定走 vanilla 消失）。
		return stack.copy();
	}
}
