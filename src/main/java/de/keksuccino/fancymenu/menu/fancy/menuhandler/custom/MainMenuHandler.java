package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import java.awt.Color;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.events.SoftMenuReloadEvent;
import de.keksuccino.fancymenu.events.PlayWidgetClickSoundEvent;
import de.keksuccino.fancymenu.events.RenderGuiListBackgroundEvent;
import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.BackgroundDrawnEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.DrawScreenEvent.Post;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import de.keksuccino.konkrete.rendering.CurrentScreenHandler;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@SuppressWarnings("resource")
public class MainMenuHandler extends MenuHandlerBase {
	
	private static final CubeMapRenderer PANORAMA_CUBE_MAP = new CubeMapRenderer(new Identifier("textures/gui/title/background/panorama"));
	private static final Identifier PANORAMA_OVERLAY = new Identifier("textures/gui/title/background/panorama_overlay.png");
	private static final Identifier MINECRAFT_TITLE_TEXTURE = new Identifier("textures/gui/title/minecraft.png");
	private static final Identifier EDITION_TITLE_TEXTURE = new Identifier("textures/gui/title/edition.png");
	private static final Random RANDOM = new Random();
	
	private RotatingCubeMapRenderer panorama = new RotatingCubeMapRenderer(PANORAMA_CUBE_MAP);

	private String splash;
	
	public MainMenuHandler() {
		super(TitleScreen.class.getName());
	}

	@SubscribeEvent
	@Override
	public void onSoftReload(SoftMenuReloadEvent e) {
		super.onSoftReload(e);
	}
	
	@SubscribeEvent
	@Override
	public void onMenuReloaded(MenuReloadedEvent e) {
		super.onMenuReloaded(e);
		
		this.splash = getRandomSplashText();
	}

	@SubscribeEvent
	@Override
	public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		if (this.shouldCustomize(e.getGui())) {
			if (MenuCustomization.isMenuCustomizable(e.getGui())) {
				if (e.getGui() instanceof TitleScreen) {
					setShowFadeInAnimation(false, (TitleScreen) e.getGui());
				}
			}
		}
		super.onInitPre(e);
	}
	
	@SubscribeEvent
	@Override
	public void onButtonsCached(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui())) {
			if (MenuCustomization.isMenuCustomizable(e.getGui())) {

				if (this.splash == null) {
					this.splash = getRandomSplashText();
				}
				
				this.setWidthCopyrightRest(Integer.MAX_VALUE);
				
				super.onButtonsCached(e);
			}
		}
	}
	
	@SubscribeEvent
	@Override
	public void onRenderPost(Post e) {
		super.onRenderPost(e);
	}
	
	@SubscribeEvent
	public void onRender(GuiScreenEvent.DrawScreenEvent.Pre e) {
		if (this.shouldCustomize(e.getGui())) {
			if (MenuCustomization.isMenuCustomizable(e.getGui())) {
				e.setCanceled(true);
				e.getGui().renderBackground(e.getMatrixStack());
			}
		}
	}
	
	/**
	 * Mimic the original main menu to be able to customize it easier
	 */
	@SubscribeEvent
	@Override
	public void drawToBackground(BackgroundDrawnEvent e) {
		
		if (this.shouldCustomize(e.getGui())) {
			
			TextRenderer font = MinecraftClient.getInstance().textRenderer;
			int width = e.getGui().width;
			int height = e.getGui().height;
			int j = width / 2 - 137;
			float minecraftLogoSpelling = RANDOM.nextFloat();
			int mouseX = MouseInput.getMouseX();
			int mouseY = MouseInput.getMouseY();
			MatrixStack matrix = CurrentScreenHandler.getMatrixStack();
			
			RenderSystem.enableBlend();
			
			//Draw the panorama skybox and a semi-transparent overlay over it
			if (!this.canRenderBackground()) {
				this.panorama.render(MinecraftClient.getInstance().getLastFrameDuration(), 1.0F);
				RenderUtils.bindTexture(PANORAMA_OVERLAY);
				RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
				drawTexture(matrix, 0, 0, e.getGui().width, e.getGui().height, 0.0F, 0.0F, 16, 128, 16, 128);
			}
			
			super.drawToBackground(e);
			
			//Draw minecraft logo and edition textures if not disabled in the config
			if (!FancyMenu.config.getOrDefault("hidelogo", true)) {
				RenderUtils.bindTexture(MINECRAFT_TITLE_TEXTURE);
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
				if ((double) minecraftLogoSpelling < 1.0E-4D) {
					e.getGui().drawTexture(matrix, j + 0, 30, 0, 0, 99, 44);
					e.getGui().drawTexture(matrix, j + 99, 30, 129, 0, 27, 44);
					e.getGui().drawTexture(matrix, j + 99 + 26, 30, 126, 0, 3, 44);
					e.getGui().drawTexture(matrix, j + 99 + 26 + 3, 30, 99, 0, 26, 44);
					e.getGui().drawTexture(matrix, j + 155, 30, 0, 45, 155, 44);
				} else {
					e.getGui().drawTexture(matrix, j + 0, 30, 0, 0, 155, 44);
					e.getGui().drawTexture(matrix, j + 155, 30, 0, 45, 155, 44);
				}

				RenderUtils.bindTexture(EDITION_TITLE_TEXTURE);
				drawTexture(matrix, j + 88, 67, 0.0F, 0.0F, 98, 14, 128, 16);
			}

			//Draw branding strings to the main menu if not disabled in the config
			if (!FancyMenu.config.getOrDefault("hidebranding", false)) {
				String string = "Minecraft " + SharedConstants.getGameVersion().getName();
				if (MinecraftClient.getInstance().isDemo()) {
					string = string + " Demo";
				} else {
					string = string + ("release".equalsIgnoreCase(MinecraftClient.getInstance().getVersionType()) ? "" : "/" + MinecraftClient.getInstance().getVersionType());
				}

				if (MinecraftClient.getInstance().isModded()) {
					string = string + I18n.translate("menu.modded");
				}
				
				drawStringWithShadow(e.getMatrixStack(), font, string, 2, e.getGui().height - 10, -1);
			}

			//Draw and handle copyright
			String c = "Copyright Mojang AB. Do not distribute!";
			String cPos = FancyMenu.config.getOrDefault("copyrightposition", "bottom-right");
			int cX = 0;
			int cY = 0;
			int cW = MinecraftClient.getInstance().textRenderer.getWidth(c);
			int cH = 10;
			
			if (cPos.equalsIgnoreCase("top-left")) {
				cX = 2;
				cY = 2;
			} else if (cPos.equalsIgnoreCase("top-centered")) {
				cX = (width / 2) - (cW / 2);
				cY = 2;
			} else if (cPos.equalsIgnoreCase("top-right")) {
				cX = width - cW - 2;
				cY = 2;
			} else if (cPos.equalsIgnoreCase("bottom-left")) {
				cX = 2;
				cY = height - cH - 2;
			} else if (cPos.equalsIgnoreCase("bottom-centered")) {
				cX = (width / 2) - (cW / 2);
				cY = height - cH - 2;
			} else {
				cX = width - cW - 2;
				cY = height - cH - 2;
			}
			
			Color copyrightcolor = RenderUtils.getColorFromHexString(FancyMenu.config.getOrDefault("copyrightcolor", "#ffffff"));
			if (copyrightcolor == null) {
				copyrightcolor = new Color(255, 255, 255);
			}

			drawStringWithShadow(matrix, font, c, cX, cY, copyrightcolor.getRGB() | 255 << 24);
			
			if ((mouseX >= cX) && (mouseX <= cX + cW) && (mouseY >= cY) && (mouseY <= cY + cH)) {
				fill(matrix, cX, cY + cH - 1, cX + cW, cY + cH, -1);
				
				if (MouseInput.isLeftMouseDown()) {
					MinecraftClient.getInstance().openScreen(new CreditsScreen(false, Runnables.doNothing()));
				}
			}

			if (!PopupHandler.isPopupActive()) {
				this.renderButtons(e, mouseX, mouseY);
			}
			
			//Draw notification indicators to the "Realms" button if not disabled in the config
			if (!FancyMenu.config.getOrDefault("hiderealmsnotifications", false)) {
				this.drawRealmsNotification(matrix, e.getGui());
			}
			
			this.renderSplash(matrix, font, e.getGui());

		}
	}
	
	@SubscribeEvent
	@Override
	public void onButtonClickSound(PlayWidgetClickSoundEvent.Pre e) {
		super.onButtonClickSound(e);
	}
	
	@SubscribeEvent
	@Override
	public void onButtonRenderBackground(RenderWidgetBackgroundEvent.Pre e) {
		super.onButtonRenderBackground(e);
	}
	
	@SubscribeEvent
	@Override
	public void onRenderListBackground(RenderGuiListBackgroundEvent.Post e) {
		super.onRenderListBackground(e);
	}

	protected void renderSplash(MatrixStack matrix, TextRenderer font, Screen s) {
		
		if (!FancyMenu.config.getOrDefault("hidesplashtext", true)) {
			
			float finalPosX = (s.width / 2 + 90);
			float finalPosY = 70.0F;

			int rotation = FancyMenu.config.getOrDefault("splashrotation", -20);
			int posX = FancyMenu.config.getOrDefault("splashx", 0);
			int posY = FancyMenu.config.getOrDefault("splashy", 0);
			String orientation = FancyMenu.config.getOrDefault("splashorientation", "original");

			int originX = 0;
			int originY = 0;

			boolean setpos = true;
			
			if (orientation.equalsIgnoreCase("original")) {
				originX = (int) finalPosX;
				originY = (int) finalPosY;
			} else if (orientation.equalsIgnoreCase("top-left")) {
				; //do nuffin
			} else if (orientation.equalsIgnoreCase("mid-left")) {
				originY = s.height / 2;
			} else if (orientation.equalsIgnoreCase("bottom-left")) {
				originY = s.height;
			} else if (orientation.equalsIgnoreCase("top-centered")) {
				originX = s.width / 2;
			} else if (orientation.equalsIgnoreCase("mid-centered")) {
				originX = s.width / 2;
				originY = s.height / 2;
			} else if (orientation.equalsIgnoreCase("bottom-centered")) {
				originX = s.width / 2;
				originY = s.height;
			} else if (orientation.equalsIgnoreCase("top-right")) {
				originX = s.width;
			} else if (orientation.equalsIgnoreCase("mid-right")) {
				originX = s.width;
				originY = s.height / 2;
			} else if (orientation.equalsIgnoreCase("bottom-right")) {
				originX = s.width;
				originY = s.height;
			} else {
				setpos = false;
			}

			//I'm doing this to signalize when an invalid orientation was used
			if (setpos) {
				finalPosX = originX + posX;
				finalPosY = originY + posY;
			}
			
			if (this.splash == null) {
				this.splash = "";
			}

			RenderSystem.pushMatrix();
			RenderSystem.translatef(finalPosX, finalPosY, 0.0F);
			RenderSystem.rotatef((float)rotation, 0.0F, 0.0F, 1.0F);
			float f = 1.8F - MathHelper.abs(MathHelper.sin((float) (System.currentTimeMillis() % 1000L) / 1000.0F * ((float) Math.PI * 2F)) * 0.1F);
			f = f * 100.0F / (float) (font.getWidth(this.splash) + 32);
			RenderSystem.scalef(f, f, f);

			Color c = RenderUtils.getColorFromHexString(FancyMenu.config.getOrDefault("splashcolor", "#ffff00"));
			if (c == null) {
				c = new Color(255, 255, 0);
			}

			drawCenteredText(matrix, font, new LiteralText(this.splash), 0, -8, c.getRGB());

			RenderSystem.popMatrix();
			
		}
		
	}
	
	private void renderButtons(GuiScreenEvent.BackgroundDrawnEvent e, int mouseX, int mouseY) {
		List<AbstractButtonWidget> buttons = this.getButtonList(e.getGui());
		float partial = MinecraftClient.getInstance().getTickDelta();
		
		if (buttons != null) {
			for(int i = 0; i < buttons.size(); ++i) {
				buttons.get(i).render(CurrentScreenHandler.getMatrixStack(), mouseX, mouseY, partial);
			}
		}
	}
	
	private void drawRealmsNotification(MatrixStack matrix, Screen gui) {
		try {
			if (MinecraftClient.getInstance().options.realmsNotifications) {
				Field f = ReflectionHelper.findField(TitleScreen.class, "realmsNotificationGui", "field_2592"); //"realmsNotification" field from GuiMainMenu
				Screen realms = null;
				try {
					realms = (Screen) f.get(gui);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (realms != null) {
					//render
					realms.render(matrix, (int)MinecraftClient.getInstance().mouse.getX(), (int)MinecraftClient.getInstance().mouse.getY(), MinecraftClient.getInstance().getTickDelta());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private List<AbstractButtonWidget> getButtonList(Screen gui) {
		List<AbstractButtonWidget> buttons = new ArrayList<AbstractButtonWidget>();
		try {
			Field f = ReflectionHelper.findField(Screen.class, "buttons", "field_22791");
			try {
				buttons = (List<AbstractButtonWidget>) f.get(gui);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buttons;
	}

	private void setWidthCopyrightRest(int i) {
		try {
			if (MinecraftClient.getInstance().currentScreen instanceof TitleScreen) {
				Field f = ReflectionHelper.findField(TitleScreen.class, "copyrightTextX", "field_2606");
				f.set(MinecraftClient.getInstance().currentScreen, i);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static void setShowFadeInAnimation(boolean showFadeIn, TitleScreen s) {
		try {
			Field f = ReflectionHelper.findField(TitleScreen.class, "doBackgroundFade", "field_18222");
			f.setBoolean(s, showFadeIn);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected static String getRandomSplashText() {
		String customSplashPath = FancyMenu.config.getOrDefault("splashtextfile", "");
		if ((customSplashPath != null) && !customSplashPath.equals("")) {
			File f = new File(customSplashPath);
			if (f.exists() && f.isFile() && f.getPath().toLowerCase().endsWith(".txt")) {
				List<String> l = FileUtils.getFileLines(f);
				if ((l != null) && !l.isEmpty()) {
					int random = MathUtils.getRandomNumberInRange(0, l.size()-1);
					return l.get(random);
				}
			}
		}
		
		return MinecraftClient.getInstance().getSplashTextLoader().get();
	}
}
