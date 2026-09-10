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

	// 硬度 2.0（与箱子同档），requiresCorrectToolForDrops 要求用对的工具才掉落。
	// "石镐以上"这一档不是靠这里设的，而是由数据包标签决定：
	//   data/minecraft/tags/block/mineable/pickaxe.json  -> 镐类工具才有速度加成
	//   data/minecraft/tags/block/needs_stone_tool.json  -> 木镐挖了不掉落
	// 两者缺一，方块要么没有挖掘加速（裸手速度，2.0 硬度要挖 10 秒），要么被木镐挖走。
	public static final Block DUSTBIN = Registry.register(
			BuiltInRegistries.BLOCK,
			DUSTBIN_KEY,
			new DustbinBlock(BlockBehaviour.Properties.of()
					.setId(DUSTBIN_KEY)
					.strength(2.0f, 6.0f)
					.requiresCorrectToolForDrops()
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
