package com.minciallo.dustbin.registry;

import com.minciallo.dustbin.MinCialloDustbin;
import com.minciallo.dustbin.block.DustbinBlockEntity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

public class ModBlockEntities {
	public static final BlockEntityType<DustbinBlockEntity> DUSTBIN = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			MinCialloDustbin.id("dustbin"),
			new BlockEntityType<>(DustbinBlockEntity::new, Set.of(ModBlocks.DUSTBIN))
	);

	public static void register() {
		// Static initializers above perform the actual registration.
	}
}
