package de.keksuccino.fancymenu.menu.fancy.gameintro;

import java.awt.Color;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public class GameIntroScreen extends Screen {

	private IAnimationRenderer renderer;
	private TitleScreen main;
	private boolean pre;
	private boolean loop;
	private boolean stretched;
	private boolean skipable = false;
	private int keypress;

	public GameIntroScreen(IAnimationRenderer intro, TitleScreen main) {
		super(Component.literal(""));
		this.renderer = intro;
		this.main = main;
		
		this.keypress = KeyboardHandler.addKeyPressedListener((c) -> {
			if ((Minecraft.getInstance().screen == this) && this.skipable) {
				if (c.keycode == 32) {
					this.renderer.setLooped(this.loop);
					this.renderer.setStretchImageToScreensize(this.stretched);
					this.renderer.resetAnimation();
					GameIntroHandler.introDisplayed = true;
					Minecraft.getInstance().setScreen(this.main);
					MenuCustomization.reloadCurrentMenu();
					KeyboardHandler.removeKeyPressedListener(this.keypress);
				}
			}
		});
	}
	
	@Override
	protected void init() {
		super.init();
		
		this.pre = false;
		this.loop = false;
	}
	
	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.render(graphics, mouseX, mouseY, partialTicks);
		
		if (this.renderer != null) {
			if (!this.renderer.isReady()) {
				this.renderer.prepareAnimation();
			}
			if (!pre) {
				this.renderer.resetAnimation();
				this.loop = this.renderer.isGettingLooped();
				this.stretched = this.renderer.isStretchedToStreensize();
				this.renderer.setStretchImageToScreensize(true);
				this.renderer.setLooped(false);
				this.pre = true;
			}
			
			if (!this.renderer.isFinished()) {
				this.renderer.render(graphics);
				if (FancyMenu.getConfig().getOrDefault("allowgameintroskip", true)) {
					this.skipable = true;
				}
			} else {
				this.renderer.setLooped(this.loop);
				this.renderer.setStretchImageToScreensize(this.stretched);
				this.renderer.resetAnimation();
				GameIntroHandler.introDisplayed = true;
				Minecraft.getInstance().setScreen(this.main);
				MenuCustomization.reloadCurrentMenu();
			}
		}
		
		if (this.skipable) {
			RenderSystem.enableBlend();
			graphics.pose().pushPose();
			graphics.pose().scale(1.05F, 1.05F, 1.05F);
			String text = Locals.localize("gameintro.skip");
			String customtext = StringUtils.convertFormatCodes(FancyMenu.getConfig().getOrDefault("customgameintroskiptext", ""), "&", "§");
			if ((customtext != null) && !customtext.equals("")) {
				text = customtext;
			}
			graphics.drawCenteredString(Minecraft.getInstance().font, text, (int) ((this.width / 2) / 1.05), (int) ((this.height - 30) / 1.05), new Color(255, 255, 255, 180).getRGB());
			graphics.pose().popPose();
		}
	}

}
