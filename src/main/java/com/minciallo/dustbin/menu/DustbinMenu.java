package com.minciallo.dustbin.menu;

import com.minciallo.dustbin.block.DustbinBlockEntity;
import com.minciallo.dustbin.registry.ModMenuTypes;
import com.minciallo.dustbin.storage.DustbinInventory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * Trash-bin menu: a 6x9 grid that everyone can take from but nobody can place into.
 */
public class DustbinMenu extends AbstractContainerMenu {
	public static final int SLOT_COUNT = DustbinInventory.SLOT_COUNT; // 54

	private final Container container;
	/** Position of the owning block, used to close the lid when the GUI is dismissed. */
	@Nullable
	private final BlockPos pos;

	/** Client-side / fallback constructor using a fresh inventory. */
	public DustbinMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new DustbinInventory(), null);
	}

	public DustbinMenu(int containerId, Inventory playerInventory, Container container) {
		this(containerId, playerInventory, container, null);
	}

	public DustbinMenu(int containerId, Inventory playerInventory, Container container, @Nullable BlockPos pos) {
		super(ModMenuTypes.DUSTBIN, containerId);
		checkContainerSize(container, SLOT_COUNT);
		this.container = container;
		this.pos = pos;

		// Trash-bin grid (take-only), 6 rows x 9 columns.
		for (int row = 0; row < 6; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new TakeOnlySlot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
			}
		}

		// Player inventory, positioned below the 6-row grid (matches vanilla chest layout).
		addStandardInventorySlots(playerInventory, 8, 18 + 6 * 18 + 13);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack itemStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack current = slot.getItem();
			itemStack = current.copy();
			if (index < SLOT_COUNT) {
				// Shift-clicking a trash-bin item moves it into the player's inventory.
				if (!this.moveItemStackTo(current, SLOT_COUNT, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else {
				// Shift-clicking a player item cannot enter the trash bin (take-only slots).
				if (!this.moveItemStackTo(current, 0, SLOT_COUNT, false)) {
					return ItemStack.EMPTY;
				}
			}

			if (current.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}

			if (current.getCount() == itemStack.getCount()) {
				return ItemStack.EMPTY;
			}

			slot.onTake(player, current);
		}
		return itemStack;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.container.stillValid(player);
	}

	/**
	 * Called when the GUI is dismissed (ESC, walking away, death, dimension change...).
	 * Folds the lid back down on the server; the block state change then reaches the
	 * client, where the renderer plays the closing animation.
	 */
	@Override
	public void removed(Player player) {
		super.removed(player);
		if (pos != null && !player.level().isClientSide()
				&& player.level().getBlockEntity(pos) instanceof DustbinBlockEntity blockEntity) {
			blockEntity.setOpen(false);
		}
	}

	public Container getContainer() {
		return container;
	}
}
