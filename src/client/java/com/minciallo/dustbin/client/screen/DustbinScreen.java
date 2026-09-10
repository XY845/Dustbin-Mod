package com.minciallo.dustbin.client.screen;

import com.minciallo.dustbin.menu.DustbinMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI for the trash bin, reusing the vanilla 6-row (generic_54) container texture.
 */
public class DustbinScreen extends AbstractContainerScreen<DustbinMenu> {
	private static final Identifier CONTAINER_BACKGROUND =
			Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
	private static final int ROWS = 6;

	public DustbinScreen(DustbinMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, 176, 114 + ROWS * 18);
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		int x = (this.width - this.imageWidth) / 2;
		int y = (this.height - this.imageHeight) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, x, y, 0.0f, 0.0f,
				this.imageWidth, ROWS * 18 + 17, 256, 256);
		graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, x, y + ROWS * 18 + 17, 0.0f, 126.0f,
				this.imageWidth, 96, 256, 256);
	}
}
