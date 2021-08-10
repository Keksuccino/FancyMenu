package de.keksuccino.fancymenu.menu.fancy.gameintro;

import java.awt.Color;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

public class GameIntroScreen extends Screen {

	private IAnimationRenderer renderer;
	private MainMenuScreen main;
	private boolean pre;
	private boolean loop;
	private boolean stretched;
	private boolean skipable = false;
	private int keypress;

	public GameIntroScreen(IAnimationRenderer intro, MainMenuScreen main) {
		super(new StringTextComponent(""));
		this.renderer = intro;
		this.main = main;
		
		this.keypress = KeyboardHandler.addKeyPressedListener((c) -> {
			if ((Minecraft.getInstance().currentScreen == this) && this.skipable) {
				if (c.keycode == 32) {
					this.renderer.setLooped(this.loop);
					this.renderer.setStretchImageToScreensize(this.stretched);
					this.renderer.resetAnimation();
					GameIntroHandler.introDisplayed = true;
					Minecraft.getInstance().displayGuiScreen(this.main);
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
	public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
		super.render(matrix, mouseX, mouseY, partialTicks);
		
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
				this.renderer.render(matrix);
				if (FancyMenu.config.getOrDefault("allowgameintroskip", true)) {
					this.skipable = true;
				}
			} else {
				this.renderer.setLooped(this.loop);
				this.renderer.setStretchImageToScreensize(this.stretched);
				this.renderer.resetAnimation();
				GameIntroHandler.introDisplayed = true;
				Minecraft.getInstance().displayGuiScreen(this.main);
				MenuCustomization.reloadCurrentMenu();
			}
		}
		
		if (this.skipable) {
			RenderSystem.enableBlend();
			matrix.push();
			matrix.scale(1.05F, 1.05F, 1.05F);
			String text = Locals.localize("gameintro.skip");
			String customtext = StringUtils.convertFormatCodes(FancyMenu.config.getOrDefault("customgameintroskiptext", ""), "&", "§");
			if ((customtext != null) && !customtext.equals("")) {
				text = customtext;
			}
			AbstractGui.drawCenteredString(matrix, Minecraft.getInstance().fontRenderer, text, (int) ((this.width / 2) / 1.05), (int) ((this.height - 30) / 1.05), new Color(255, 255, 255, 180).getRGB());
			matrix.pop();
		}
	}

}
