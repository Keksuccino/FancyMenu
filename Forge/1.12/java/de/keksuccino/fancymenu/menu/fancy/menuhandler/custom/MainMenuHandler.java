package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.lwjgl.util.glu.Project;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Runnables;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiWinGame;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.BackgroundDrawnEvent;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class MainMenuHandler extends MenuHandlerBase {

	private static final ResourceLocation FACEBOOK = new ResourceLocation("keksuccino", "socialmedia/fb.png");
	private static final ResourceLocation TWITTER = new ResourceLocation("keksuccino", "socialmedia/twitter.png");
	private static final ResourceLocation INSTAGRAM = new ResourceLocation("keksuccino", "socialmedia/instagram.png");
	private int tickFooter;
	private float fadeFooter;
	
	private static final ResourceLocation[] PANORAMA_RESOURCES = new ResourceLocation[] {new ResourceLocation("textures/gui/title/background/panorama_0.png"), new ResourceLocation("textures/gui/title/background/panorama_1.png"), new ResourceLocation("textures/gui/title/background/panorama_2.png"), new ResourceLocation("textures/gui/title/background/panorama_3.png"), new ResourceLocation("textures/gui/title/background/panorama_4.png"), new ResourceLocation("textures/gui/title/background/panorama_5.png")};
	private DynamicTexture viewport = new DynamicTexture(256, 256);
	private ResourceLocation background;
	private float panoramaTimer;
	
	private static final ResourceLocation MINECRAFT_TITLE_TEXTURES = new ResourceLocation("textures/gui/title/minecraft.png");
	private static final ResourceLocation MINECRAFT_TITLE_EDITION = new ResourceLocation("textures/gui/title/edition.png");
	private static final ResourceLocation SPLASH_TEXTS = new ResourceLocation("texts/splashes.txt");
	private static final Random RANDOM = new Random();
	
	private String splash;
	
	public MainMenuHandler() {
		super(GuiMainMenu.class.getName());
	}
	
	@Override
	public void onButtonsCached(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui())) {
			if (MenuCustomization.isMenuCustomizable(e.getGui())) {
				// Resetting values to defaults
				fadeFooter = 0.1F;
				tickFooter = 0;

				if (this.splash == null) {
					this.splash = "missingno";
					IResource iresource = null;
					try {
						List<String> list = Lists.<String>newArrayList();
						iresource = Minecraft.getMinecraft().getResourceManager().getResource(SPLASH_TEXTS);
						BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(iresource.getInputStream(), StandardCharsets.UTF_8));
						String s;
						while ((s = bufferedreader.readLine()) != null) {
							s = s.trim();

							if (!s.isEmpty()) {
								list.add(s);
							}
						}
						if (!list.isEmpty()) {
							while (true) {
								this.splash = list.get(RANDOM.nextInt(list.size()));

								if (this.splash.hashCode() != 125780783) {
									break;
								}
							}
						}

						Calendar calendar = Calendar.getInstance();
						calendar.setTime(new Date());

						if (calendar.get(2) + 1 == 12 && calendar.get(5) == 24) {
							this.splash = "Merry X-mas!";
						}
						else if (calendar.get(2) + 1 == 1 && calendar.get(5) == 1) {
							this.splash = "Happy new year!";
						}
						else if (calendar.get(2) + 1 == 10 && calendar.get(5) == 31) {
							this.splash = "OOoooOOOoooo! Spooky!";
						}

					} catch (IOException var8) {
					} finally {
						IOUtils.closeQuietly((Closeable)iresource);
					}
				}

				this.background = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("background", this.viewport);
				
				this.setWidthCopyrightRest(Integer.MAX_VALUE);
				
			}
			
		}
		
		super.onButtonsCached(e);
	}
	
	@SubscribeEvent
	public void onRender(GuiScreenEvent.DrawScreenEvent.Pre e) {
		if (this.shouldCustomize(e.getGui())) {
			if (MenuCustomization.isMenuCustomizable(e.getGui())) {
				e.setCanceled(true);
				e.getGui().drawDefaultBackground();

				this.renderFooter(e);
			}
		}
	}
	
	/**
	 * Mimic the original main menu to be able to customize it easier
	 */
	@Override
	public void drawToBackground(BackgroundDrawnEvent e) {
		if (this.shouldCustomize(e.getGui())) {
			FontRenderer font = Minecraft.getMinecraft().fontRenderer;
			int width = e.getGui().width;
			int height = e.getGui().height;
			int j = width / 2 - 137;
			float minecraftLogoSpelling = RANDOM.nextFloat();
			int mouseX = MouseInput.getMouseX();
			int mouseY = MouseInput.getMouseY();
			
			//Draw the panorama skybox and a semi-transparent overlay over it
			if (!this.canRenderBackground()) {
				this.panoramaTimer += Minecraft.getMinecraft().getRenderPartialTicks();
				this.renderSkybox(mouseX, mouseY, Minecraft.getMinecraft().getRenderPartialTicks(), e.getGui());
			}
			
			super.drawToBackground(e);
			
			//Draw minecraft logo and edition textures if not disabled in the config
			if (!FancyMenu.config.getOrDefault("hidelogo", true)) {
				GlStateManager.enableBlend();
				Minecraft.getMinecraft().getTextureManager().bindTexture(MINECRAFT_TITLE_TEXTURES);
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
				if ((double) minecraftLogoSpelling < 1.0E-4D) {
					e.getGui().drawTexturedModalRect(j + 0, 30, 0, 0, 99, 44);
					e.getGui().drawTexturedModalRect(j + 99, 30, 129, 0, 27, 44);
					e.getGui().drawTexturedModalRect(j + 99 + 26, 30, 126, 0, 3, 44);
					e.getGui().drawTexturedModalRect(j + 99 + 26 + 3, 30, 99, 0, 26, 44);
					e.getGui().drawTexturedModalRect(j + 155, 30, 0, 45, 155, 44);
				} else {
					e.getGui().drawTexturedModalRect(j + 0, 30, 0, 0, 155, 44);
					e.getGui().drawTexturedModalRect(j + 155, 30, 0, 45, 155, 44);
				}

				String version = ForgeVersion.mcVersion;
				if (version.equals("1.12.2")) {
					Minecraft.getMinecraft().getTextureManager().bindTexture(MINECRAFT_TITLE_EDITION);
					GuiScreen.drawModalRectWithCustomSizedTexture(j + 88, 67, 0.0F, 0.0F, 98, 14, 128.0F, 16.0F);
				}
				GlStateManager.disableBlend();
			}

			//Draw branding strings to the main menu if not disabled in the config
			if (!FancyMenu.config.getOrDefault("hidebranding", true)) {
				List<String> brandings = Lists.reverse(FMLCommonHandler.instance().getBrandings(true));
		        for (int brdline = 0; brdline < brandings.size(); brdline++) {
		            String brd = brandings.get(brdline);
		            if (!Strings.isNullOrEmpty(brd)) {
		                e.getGui().drawString(Minecraft.getMinecraft().fontRenderer, brd, 2, e.getGui().height - ( 10 + brdline * (Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + 1)), 16777215);
		            }
		        }
			}
			
			if (!FancyMenu.config.getOrDefault("hideforgenotifications", false)) {
				ForgeHooksClient.renderMainMenu((GuiMainMenu) e.getGui(), font, e.getGui().width, e.getGui().height, "");
			}
			
			//Draw and handle copyright
			String c = "Copyright Mojang AB. Do not distribute!";
			String cPos = FancyMenu.config.getOrDefault("copyrightposition", "bottom-right");
			int cX = 0;
			int cY = 0;
			int cW = Minecraft.getMinecraft().fontRenderer.getStringWidth(c);
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
			
			e.getGui().drawString(font, c, cX, cY, -1);
			
			if ((mouseX >= cX) && (mouseX <= cX + cW) && (mouseY >= cY) && (mouseY <= cY + cH)) {
				Gui.drawRect(cX, cY + cH - 1, cX + cW, cY + cH, -1);
				
				if (MouseInput.isLeftMouseDown()) {
					Minecraft.getMinecraft().displayGuiScreen(new GuiWinGame(false, Runnables.doNothing()));
				}
			}

			if (!PopupHandler.isPopupActive()) {
				this.renderButtonsAndLabels(e, mouseX, mouseY);
			}
			
			//Draw notification indicators to the "Realms" button if not disabled in the config
			if (!FancyMenu.config.getOrDefault("hiderealmsnotifications", false)) {
				this.drawRealmsNotification(e.getGui());
			}
			
			//Draw splashtext string to the main menu if not disabled in the config
			if (!FancyMenu.config.getOrDefault("hidesplashtext", true)) {
				int offsetx = FancyMenu.config.getOrDefault("splashoffsetx", 0);
				int offsety = FancyMenu.config.getOrDefault("splashoffsety", 0);
				int rotation = FancyMenu.config.getOrDefault("splashrotation", -20);
				GlStateManager.pushMatrix();
				GlStateManager.translate((float) (width / 2 + 90) + offsetx, 70.0F + offsety, 0.0F);
				GlStateManager.rotate((float)rotation, 0.0F, 0.0F, 1.0F);
				float f = 1.8F - MathHelper.abs(MathHelper.sin((float) (System.currentTimeMillis() % 1000L) / 1000.0F * ((float) Math.PI * 2F)) * 0.1F);
				f = f * 100.0F / (float) (font.getStringWidth(this.splash) + 32);
				GlStateManager.scale(f, f, f);
				e.getGui().drawCenteredString(font, this.splash, 0, -8, -256);
				GlStateManager.popMatrix();
			}
		}
	}
	
	private void renderButtonsAndLabels(GuiScreenEvent.BackgroundDrawnEvent e, int mouseX, int mouseY) {
		List<GuiButton> buttons = this.getButtonList(e.getGui());
		List<GuiLabel> labels = this.getLabelList(e.getGui());
		float partial = Minecraft.getMinecraft().getRenderPartialTicks();
		
		if (buttons != null) {
			for(int i = 0; i < buttons.size(); ++i) {
				buttons.get(i).drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partial);
			}
		}
		
		if (labels != null) {
			for(int j = 0; j < labels.size(); ++j) {
				System.out.println(labels.get(j).toString());
				labels.get(j).drawLabel(Minecraft.getMinecraft(), mouseX, mouseY);
			}
		}
	}
	
	private void drawRealmsNotification(GuiScreen gui) {
		if (Minecraft.getMinecraft().gameSettings.getOptionOrdinalValue(GameSettings.Options.REALMS_NOTIFICATIONS)) {
			Field f = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(GuiMainMenu.class, "field_183503_M", "realmsNotification");
			GuiScreen realms = null;
			try {
				realms = (GuiScreen) f.get(gui);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (realms != null) {
				realms.drawScreen((int)MouseInput.getMouseX(), (int)MouseInput.getMouseY(), Minecraft.getMinecraft().getRenderPartialTicks());
			}
		}
	}
	
	private List<GuiButton> getButtonList(GuiScreen gui) {
		Field f = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(GuiScreen.class, "field_146292_n", "buttonList");
		List<GuiButton> buttons = new ArrayList<GuiButton>();
		try {
			buttons = (List<GuiButton>) f.get(gui);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buttons;
	}
	
	private List<GuiLabel> getLabelList(GuiScreen gui) {
		Field f = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(GuiScreen.class, "field_146293_o", "labelList");
		List<GuiLabel> labels = new ArrayList<GuiLabel>();
		try {
			labels = (List<GuiLabel>) f.get(gui);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return labels;
	}
	
	private void setWidthCopyrightRest(int i) {
		try {
			if (Minecraft.getMinecraft().currentScreen instanceof GuiMainMenu) {
				Field f = ReflectionHelper.findField(GuiMainMenu.class, "field_193979_N", "widthCopyrightRest");
				f.set(Minecraft.getMinecraft().currentScreen, i);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void renderFooter(GuiScreenEvent.DrawScreenEvent e) {
		if (!FancyMenu.config.getOrDefault("showmainmenufooter", true)) {
			return;
		}
		
		if (tickFooter < 30) {
			tickFooter += 1;
		} else if (e.getGui().height >= 280) {
			int i = MathHelper.ceil(fadeFooter * 255.0F) << 24;
			
			RenderUtils.setScale(1.1F);
			e.getGui().drawCenteredString(Minecraft.getMinecraft().fontRenderer, "§fDISCOVER MORE AT MINECRAFT.NET", (int) (e.getGui().width / 2 / 1.1D), (int) ((e.getGui().height - 50) / 1.1D), i);
			RenderUtils.postScale();
			
			RenderUtils.setScale(0.7F);
			e.getGui().drawString(Minecraft.getMinecraft().fontRenderer, "§f@MINECRAFT", (int) ((e.getGui().width / 2 - 10) / 0.7D), (int) ((e.getGui().height - 30) / 0.7D), i);
			
			e.getGui().drawString(Minecraft.getMinecraft().fontRenderer, "§fMINECRAFT", (int) ((e.getGui().width / 2 + 60) / 0.7D), (int) ((e.getGui().height - 30) / 0.7D), i);

			e.getGui().drawString(Minecraft.getMinecraft().fontRenderer, "§f/MINECRAFT", (int) ((e.getGui().width / 2 - 80) / 0.7D), (int) ((e.getGui().height - 30) / 0.7D), i);
			RenderUtils.postScale();
			
			GlStateManager.enableBlend();
			
			Minecraft.getMinecraft().getTextureManager().bindTexture(FACEBOOK);
			GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			GlStateManager.color(1.0F, 1.0F, 1.0F, MathHelper.clamp(fadeFooter, 0.1F, 1.0F));
			GuiIngame.drawModalRectWithCustomSizedTexture(e.getGui().width / 2 - 100, e.getGui().height - 35, 0.0F, 0.0F, 15, 15, 15, 15);

			Minecraft.getMinecraft().getTextureManager().bindTexture(TWITTER);
			GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			GlStateManager.color(1.0F, 1.0F, 1.0F, MathHelper.clamp(fadeFooter, 0.1F, 1.0F));
			GuiIngame.drawModalRectWithCustomSizedTexture(e.getGui().width / 2 - 30, e.getGui().height - 35, 0.0F, 0.0F, 15, 15, 15, 15);

			Minecraft.getMinecraft().getTextureManager().bindTexture(INSTAGRAM);
			GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			GlStateManager.color(1.0F, 1.0F, 1.0F, MathHelper.clamp(fadeFooter, 0.1F, 1.0F));
			GuiIngame.drawModalRectWithCustomSizedTexture(e.getGui().width / 2 + 40, e.getGui().height - 35, 0.0F, 0.0F, 15, 15, 15, 15);
			
			GlStateManager.disableBlend();

			if (fadeFooter < 1.0F) {
				fadeFooter += 0.005F;
			}
		}
	}
	
	private void drawPanorama(int mouseX, int mouseY, float partialTicks) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();
		GlStateManager.matrixMode(5889);
		GlStateManager.pushMatrix();
		GlStateManager.loadIdentity();
		Project.gluPerspective(120.0F, 1.0F, 0.05F, 10.0F);
		GlStateManager.matrixMode(5888);
		GlStateManager.pushMatrix();
		GlStateManager.loadIdentity();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
		GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
		GlStateManager.enableBlend();
		GlStateManager.disableAlpha();
		GlStateManager.disableCull();
		GlStateManager.depthMask(false);
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);

		for (int j = 0; j < 64; ++j) {
			GlStateManager.pushMatrix();
			float f = ((float) (j % 8) / 8.0F - 0.5F) / 64.0F;
			float f1 = ((float) (j / 8) / 8.0F - 0.5F) / 64.0F;
			GlStateManager.translate(f, f1, 0.0F);
			GlStateManager.rotate(MathHelper.sin(this.panoramaTimer / 400.0F) * 25.0F + 20.0F, 1.0F, 0.0F, 0.0F);
			GlStateManager.rotate(-this.panoramaTimer * 0.1F, 0.0F, 1.0F, 0.0F);

			for (int k = 0; k < 6; ++k) {
				GlStateManager.pushMatrix();

				if (k == 1) {
					GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
				}
				if (k == 2) {
					GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
				}
				if (k == 3) {
					GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
				}
				if (k == 4) {
					GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
				}
				if (k == 5) {
					GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
				}

				Minecraft.getMinecraft().getTextureManager().bindTexture(PANORAMA_RESOURCES[k]);
				bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
				int l = 255 / (j + 1);
				bufferbuilder.pos(-1.0D, -1.0D, 1.0D).tex(0.0D, 0.0D).color(255, 255, 255, l).endVertex();
				bufferbuilder.pos(1.0D, -1.0D, 1.0D).tex(1.0D, 0.0D).color(255, 255, 255, l).endVertex();
				bufferbuilder.pos(1.0D, 1.0D, 1.0D).tex(1.0D, 1.0D).color(255, 255, 255, l).endVertex();
				bufferbuilder.pos(-1.0D, 1.0D, 1.0D).tex(0.0D, 1.0D).color(255, 255, 255, l).endVertex();
				tessellator.draw();
				GlStateManager.popMatrix();
			}

			GlStateManager.popMatrix();
			GlStateManager.colorMask(true, true, true, false);
		}

		bufferbuilder.setTranslation(0.0D, 0.0D, 0.0D);
		GlStateManager.colorMask(true, true, true, true);
		GlStateManager.matrixMode(5889);
		GlStateManager.popMatrix();
		GlStateManager.matrixMode(5888);
		GlStateManager.popMatrix();
		GlStateManager.depthMask(true);
		GlStateManager.enableCull();
		GlStateManager.enableDepth();
	}

	private void rotateAndBlurSkybox(GuiScreen gui) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(this.background);
		GlStateManager.glTexParameteri(3553, 10241, 9729);
		GlStateManager.glTexParameteri(3553, 10240, 9729);
		GlStateManager.glCopyTexSubImage2D(3553, 0, 0, 0, 0, 0, 256, 256);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		GlStateManager.colorMask(true, true, true, false);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		GlStateManager.disableAlpha();
		float zLevel = getZlevel(gui);

		for (int j = 0; j < 3; ++j) {
			float f = 1.0F / (float) (j + 1);
			int k = gui.width;
			int l = gui.height;
			float f1 = (float) (j - 1) / 256.0F;
			bufferbuilder.pos((double) k, (double) l, (double)zLevel).tex((double) (0.0F + f1), 1.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
			bufferbuilder.pos((double) k, 0.0D, (double) zLevel).tex((double) (1.0F + f1), 1.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
			bufferbuilder.pos(0.0D, 0.0D, (double) zLevel).tex((double) (1.0F + f1), 0.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
			bufferbuilder.pos(0.0D, (double) l, (double) zLevel).tex((double) (0.0F + f1), 0.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
		}

		tessellator.draw();
		GlStateManager.enableAlpha();
		GlStateManager.colorMask(true, true, true, true);
	}

    private void renderSkybox(int mouseX, int mouseY, float partialTicks, GuiScreen gui) {
    	Minecraft.getMinecraft().getFramebuffer().unbindFramebuffer();
        GlStateManager.viewport(0, 0, 256, 256);
        this.drawPanorama(mouseX, mouseY, partialTicks);
        this.rotateAndBlurSkybox(gui);
        this.rotateAndBlurSkybox(gui);
        this.rotateAndBlurSkybox(gui);
        this.rotateAndBlurSkybox(gui);
        this.rotateAndBlurSkybox(gui);
        this.rotateAndBlurSkybox(gui);
        this.rotateAndBlurSkybox(gui);
        Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
        GlStateManager.viewport(0, 0, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
        float f = 120.0F / (float)(gui.width > gui.height ? gui.width : gui.height);
        float f1 = (float)gui.height * f / 256.0F;
        float f2 = (float)gui.width * f / 256.0F;
        int i = gui.width;
        int j = gui.height;
        float zLevel = getZlevel(gui);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        bufferbuilder.pos(0.0D, (double)j, (double)zLevel).tex((double)(0.5F - f1), (double)(0.5F + f2)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        bufferbuilder.pos((double)i, (double)j, (double)zLevel).tex((double)(0.5F - f1), (double)(0.5F - f2)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        bufferbuilder.pos((double)i, 0.0D, (double)zLevel).tex((double)(0.5F + f1), (double)(0.5F - f2)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        bufferbuilder.pos(0.0D, 0.0D, (double)zLevel).tex((double)(0.5F + f1), (double)(0.5F + f2)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        tessellator.draw();
    }
    
    private static float getZlevel(Gui gui) {
    	Field f = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(Gui.class, "field_73735_i", "zLevel");
    	try {
			return f.getFloat(gui);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return -1;
    }
}
