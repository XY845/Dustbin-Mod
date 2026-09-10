package com.minciallo.dustbin.storage;

import com.minciallo.dustbin.MinCialloDustbin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.List;

/**
 * Global, per-world shared storage for the trash bin.
 * Holds the 54-slot inventory and the configurable collection threshold.
 */
public class DustbinStorage extends SavedData {
	public static final String NAME = "dustbin_storage";
	public static final int DEFAULT_COLLECTION_TICKS = 10 * 60 * 20; // 10 minutes
	public static final int MIN_COLLECTION_TICKS = 1 * 60 * 20; // 1 minute
	public static final int MAX_COLLECTION_TICKS = 1440 * 60 * 20; // 1 day (1440 minutes)

	private final DustbinInventory inventory = new DustbinInventory();
	private int collectionTicks = DEFAULT_COLLECTION_TICKS;

	private static final Codec<DustbinStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(storage -> storage.inventory.getItems()),
			Codec.INT.optionalFieldOf("collection_ticks", DEFAULT_COLLECTION_TICKS)
					.forGetter(storage -> storage.collectionTicks)
	).apply(instance, DustbinStorage::fromData));

	public static final SavedDataType<DustbinStorage> TYPE = new SavedDataType<>(
			MinCialloDustbin.id(NAME),
			DustbinStorage::new,
			CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	public DustbinStorage() {
	}

	private static DustbinStorage fromData(List<ItemStack> items, int collectionTicks) {
		DustbinStorage storage = new DustbinStorage();
		for (int i = 0; i < items.size() && i < DustbinInventory.SLOT_COUNT; i++) {
			storage.inventory.setItem(i, items.get(i));
		}
		storage.collectionTicks = collectionTicks;
		return storage;
	}

	public static DustbinStorage get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	public DustbinInventory getInventory() {
		return inventory;
	}

	public int getCollectionTicks() {
		return collectionTicks;
	}

	public void setCollectionTicks(int ticks) {
		this.collectionTicks = ticks;
		setDirty();
	}

	/**
	 * Attempts to collect the dropped stack into the shared storage.
	 *
	 * <p>收集规则：每种物品只占用一个格子，每格按物品自身堆叠上限；超出上限的多余
	 * 数量会被直接丢弃。仅当存在空位、或存在同种物品且未满的格子时才收集。
	 *
	 * @return true if the stack was collected (overflow discarded); false if there is
	 *         no slot available for this item type (the item then uses vanilla despawn).
	 */
	public boolean tryCollect(ItemStack stack) {
		if (stack.isEmpty() || !canCollect(stack)) {
			return false;
		}
		inventory.addItem(stack.copy());
		setDirty();
		return true;
	}

	/**
	 * Whether the storage can accept this item type. Matches {@code DustbinInventory.addItem}
	 * exactly: it can be collected if there is already a slot holding this same item type
	 * (whether full or not — overflow is discarded) or any empty slot.
	 */
	private boolean canCollect(ItemStack stack) {
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack slot = inventory.getItem(i);
			if (slot.isEmpty()) {
				return true;
			}
			if (ItemStack.isSameItemSameComponents(slot, stack)) {
				return true;
			}
		}
		return false;
	}

	/** Counts and clears all stored items. Returns the number of stacks removed. */
	public int clearAll() {
		int count = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (!inventory.getItem(i).isEmpty()) {
				count++;
			}
		}
		inventory.clearContent();
		setDirty();
		return count;
	}
}
