package com.minciallo.dustbin.client;

import com.minciallo.dustbin.client.render.DustbinBlockEntityRenderer;
import com.minciallo.dustbin.client.screen.DustbinScreen;
import com.minciallo.dustbin.registry.ModBlockEntities;
import com.minciallo.dustbin.registry.ModMenuTypes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

import net.minecraft.client.gui.screens.MenuScreens;

public class MinCialloDustbinClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMenuTypes.DUSTBIN, DustbinScreen::new);
		// The lid is animated geometry, which vanilla block models cannot express.
		BlockEntityRendererRegistry.register(ModBlockEntities.DUSTBIN, DustbinBlockEntityRenderer::new);
	}
}
