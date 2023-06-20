package de.keksuccino.fancymenu.customization.gameintro;

import java.awt.Color;

import com.mojang.blaze3d.systems.RenderSystem;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public class GameIntroScreen extends Screen {

	private final IAnimationRenderer renderer;
	private final TitleScreen main;
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
					ScreenCustomization.reloadCurrentScreen();
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
	public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
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
				this.skipable = FancyMenu.getOptions().allowGameIntroSkip.getValue();
			} else {
				this.renderer.setLooped(this.loop);
				this.renderer.setStretchImageToScreensize(this.stretched);
				this.renderer.resetAnimation();
				GameIntroHandler.introDisplayed = true;
				Minecraft.getInstance().setScreen(this.main);
				ScreenCustomization.reloadCurrentScreen();
			}
		}
		
		if (this.skipable) {
			RenderSystem.enableBlend();
			matrix.pushPose();
			matrix.scale(1.05F, 1.05F, 1.05F);
			String text = I18n.get("fancymenu.gameintro.skip");
			String customText = StringUtils.convertFormatCodes(FancyMenu.getOptions().customGameIntroSkipText.getValue(), "&", "§");
			if ((customText != null) && !customText.equals("")) {
				text = customText;
			}
			GuiComponent.drawCenteredString(matrix, Minecraft.getInstance().font, text, (int) ((this.width / 2) / 1.05), (int) ((this.height - 30) / 1.05), new Color(255, 255, 255, 180).getRGB());
			matrix.popPose();
		}
	}

}
