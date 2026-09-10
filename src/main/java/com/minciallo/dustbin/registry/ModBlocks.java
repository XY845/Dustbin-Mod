package com.minciallo.dustbin.registry;

import com.minciallo.dustbin.MinCialloDustbin;
import com.minciallo.dustbin.block.DustbinBlock;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {
	// 26.2 中 Block 构造链会强制要求 id 已设置（effectiveDrops 内部 requireNonNull(blockId)），
	// 因此必须在构造方块前先用 setId 预置 ResourceKey，否则会抛 "Block id not set"。
	private static final ResourceKey<Block> DUSTBIN_KEY = ResourceKey.create(
			Registries.BLOCK, MinCialloDustbin.id("dustbin"));

	public static final Block DUSTBIN = Registry.register(
			BuiltInRegistries.BLOCK,
			DUSTBIN_KEY,
			new DustbinBlock(BlockBehaviour.Properties.of()
					.setId(DUSTBIN_KEY)
					.strength(2.0f, 6.0f)
					.sound(SoundType.METAL))
	);

	// Item 与 Block 同理：26.2 中 Item 构造链也要求 id 已设置（effectiveDescriptionId 内部 itemIdOrThrow）。
	private static final ResourceKey<Item> DUSTBIN_ITEM_KEY = ResourceKey.create(
			Registries.ITEM, MinCialloDustbin.id("dustbin"));

	public static final Item DUSTBIN_ITEM = Registry.register(
			BuiltInRegistries.ITEM,
			DUSTBIN_ITEM_KEY,
			new BlockItem(DUSTBIN, new Item.Properties().setId(DUSTBIN_ITEM_KEY))
	);

	public static void register() {
		// Static initializers above perform the actual registration.
	}
}
