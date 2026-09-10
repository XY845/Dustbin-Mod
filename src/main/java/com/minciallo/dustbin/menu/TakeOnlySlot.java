package com.minciallo.dustbin.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A slot that players can only take items from, never place items into.
 */
public class TakeOnlySlot extends Slot {
	public TakeOnlySlot(Container container, int slot, int x, int y) {
		super(container, slot, x, y);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return false;
	}
}
