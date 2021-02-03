package de.keksuccino.fancymenu.menu.fancy.gameintro;

import java.awt.Color;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

public class GameIntroScreen extends GuiScreen {
	
	private IAnimationRenderer renderer;
	private GuiMainMenu main;
	private boolean pre;
	private boolean loop;
	private boolean stretched;
	private boolean skipable = false;
	private int keypress;
	
	public GameIntroScreen(IAnimationRenderer intro, GuiMainMenu main) {
		this.renderer = intro;
		this.main = main;
	}
	
	@Override
	public void initGui() {
		super.initGui();
		
		this.pre = false;
		this.loop = false;
		
		this.keypress = KeyboardHandler.addKeyPressedListener((c) -> {
			if ((Minecraft.getMinecraft().currentScreen == this) && this.skipable) {
				if (c.keycode == 57) {
					this.renderer.setLooped(this.loop);
					this.renderer.setStretchImageToScreensize(this.stretched);
					this.renderer.resetAnimation();
					GameIntroHandler.introDisplayed = true;
					Minecraft.getMinecraft().displayGuiScreen(this.main);
					KeyboardHandler.removeKeyPressedListener(this.keypress);
				}
			}
		});
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		
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
				this.renderer.render();
				if (FancyMenu.config.getOrDefault("allowgameintroskip", true)) {
					this.skipable = true;
				}
			} else {
				this.renderer.setLooped(this.loop);
				this.renderer.setStretchImageToScreensize(this.stretched);
				this.renderer.resetAnimation();
				GameIntroHandler.introDisplayed = true;
				Minecraft.getMinecraft().displayGuiScreen(this.main);
			}
		}

		if (this.skipable) {
			GlStateManager.enableBlend();
			GlStateManager.pushMatrix();
			GlStateManager.scale(1.05F, 1.05F, 1.05F);
			String text = Locals.localize("gameintro.skip");
			String customtext = StringUtils.convertFormatCodes(FancyMenu.config.getOrDefault("customgameintroskiptext", ""), "&", "§");
			if ((customtext != null) && !customtext.equals("")) {
				text = customtext;
			}
			this.drawCenteredString(Minecraft.getMinecraft().fontRendererObj, text, (int) ((this.width / 2) / 1.05), (int) ((this.height - 30) / 1.05), new Color(255, 255, 255, 180).getRGB());
			GlStateManager.popMatrix();
		}
	}

}
