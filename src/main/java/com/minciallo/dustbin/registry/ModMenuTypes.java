package com.minciallo.dustbin.registry;

import com.minciallo.dustbin.MinCialloDustbin;
import com.minciallo.dustbin.block.DustbinBlockEntity;
import com.minciallo.dustbin.menu.DustbinMenu;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

public class ModMenuTypes {
	/**
	 * Extended menu type: the extra screen-opening data is the block position,
	 * used on the server to resolve the block entity (and therefore the shared storage).
	 */
	public static final MenuType<DustbinMenu> DUSTBIN = Registry.register(
			BuiltInRegistries.MENU,
			MinCialloDustbin.id("dustbin"),
			new ExtendedMenuType<>((syncId, playerInventory, pos) -> {
				if (playerInventory.player.level().getBlockEntity(pos) instanceof DustbinBlockEntity blockEntity) {
					return blockEntity.createMenu(syncId, playerInventory, playerInventory.player);
				}
				return new DustbinMenu(syncId, playerInventory);
			}, BlockPos.STREAM_CODEC)
	);

	public static void register() {
		// Static initializers above perform the actual registration.
	}
}
