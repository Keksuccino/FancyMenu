package de.keksuccino.fancymenu.menu.fancy.item;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.math.Axis;
import de.keksuccino.fancymenu.events.InitOrResizeScreenEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;

public class SplashTextCustomizationItem extends CustomizationItemBase {

	protected static Map<String, String> splashCache = new HashMap<String, String>();
	protected static boolean init = false;
	
	public float scale = 1.0F;
	public boolean shadow = true;
	public boolean bounce = true;
	public float rotation = 20.0F;
	public Color basecolor = new Color(255, 255, 0);
	public String basecolorString = "#ffff00";
	public boolean refreshOnMenuReload = false;
	public File splashfile;
	public String text = null;
	public boolean vanillaLike = false;
	
	protected float basescale = 1.8F;
	
	protected static boolean isNewMenu = false;
	protected boolean isNewMenuThis = false;
	protected static Screen lastScreen = null;

	protected static Map<String, String> vanillaLikeCache = new HashMap<>();
	
	public SplashTextCustomizationItem(PropertiesSection item) {
		super(item);
		
		if (!init) {
			Konkrete.getEventHandler().registerEventsFrom(SplashTextCustomizationItem.class);
			init = true;
		}
		
		if ((this.action != null) && this.action.equalsIgnoreCase("addsplash")) {

			String vanillaLikeString = item.getEntryValue("vanilla-like");
			if ((vanillaLikeString != null) && vanillaLikeString.equals("true")) {
				this.vanillaLike = true;
			}

			String filepath = fixBackslashPath(item.getEntryValue("splashfilepath"));
			if (filepath != null) {
				this.splashfile = new File(filepath);
				if (!this.splashfile.exists() || !this.splashfile.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
					filepath = Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + filepath;
					this.splashfile = new File(filepath);
				}
				if (!this.splashfile.exists() || !this.splashfile.getPath().toLowerCase().endsWith(".txt")) {
					this.splashfile = null;
				}
			}

			if (!this.vanillaLike) {
				this.text = item.getEntryValue("text");
			} else if (vanillaLikeCache.containsKey(this.actionId)) {
				this.text = vanillaLikeCache.get(this.actionId);
			}
			
			String ro = item.getEntryValue("rotation");
			if ((ro != null) && MathUtils.isFloat(ro)) {
				this.rotation = Float.parseFloat(ro);
			}
			
			String re = item.getEntryValue("refresh");
			if ((re != null) && re.equalsIgnoreCase("true")) {
				this.refreshOnMenuReload = true;
			}
			
			String co = item.getEntryValue("basecolor");
			if (co != null) {
				Color c = RenderUtils.getColorFromHexString(co);
				if (c != null) {
					this.basecolor = c;
					this.basecolorString = co;
				}
			}
			
			String sh = item.getEntryValue("shadow");
			if ((sh != null)) {
				if (sh.equalsIgnoreCase("false")) {
					this.shadow = false;
				}
			}
			
			String sc = item.getEntryValue("scale");
			if ((sc != null) && MathUtils.isFloat(sc)) {
				this.scale = Float.parseFloat(sc);
			}
			
			String b = item.getEntryValue("bouncing");
			if ((b != null) && b.equalsIgnoreCase("false")) {
				this.bounce = false;
			}
			
			this.value = "splash text";
			
			this.setWidth((int) (30 * basescale * this.scale));
			this.setHeight((int) (10 * basescale * this.scale));
			
		}
	}

	public void render(PoseStack matrix, Screen menu) throws IOException {
		
		if (this.isNewMenuThis) {
			isNewMenu = false;
		}
		this.isNewMenuThis = isNewMenu;
		
		this.setWidth((int) (30 * basescale * this.scale));
		this.setHeight((int) (10 * basescale * this.scale));
		
		if (this.shouldRender()) {
			
			this.renderSplash(matrix, Minecraft.getInstance().font, menu);
			
		}
		
	}
	
	protected void renderSplash(PoseStack matrix, Font font, Screen s) {

		String splash = null;

		if (this.vanillaLike && (this.text == null)) {
			this.text = Minecraft.getInstance().getSplashManager().getSplash();
			vanillaLikeCache.put(this.actionId, this.text);
		}
		
		if ((this.splashfile != null) && (this.text == null)) {
			
			if (isNewMenu && this.refreshOnMenuReload) {
				splashCache.remove(this.getActionId());
			}
			
			if (!splashCache.containsKey(this.getActionId())) {
				List<String> l = FileUtils.getFileLines(this.splashfile);
				if (!l.isEmpty()) {
					int i = MathUtils.getRandomNumberInRange(0, l.size()-1);
					splashCache.put(this.getActionId(), l.get(i));
				}
			}
			
			if (splashCache.containsKey(this.getActionId())) {
				splash = splashCache.get(this.getActionId());
			}
			
		}
		
		if (this.text != null) {
			splash = this.text;
		}
		
		if (splash != null) {
			
			if (this.value != null) {
				if (!isEditorActive()) {
					splash = de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser.replacePlaceholders(splash);
				} else {
					splash = StringUtils.convertFormatCodes(splash, "&", "§");
				}
			}
			
			this.value = splash;
			
			float f = basescale;
			if (this.bounce) {
				f = f - Mth.abs(Mth.sin((float) (System.currentTimeMillis() % 1000L) / 1000.0F * ((float) Math.PI * 2F)) * 0.1F);
			}
			f = f * 100.0F / (float) (font.width(splash) + 32);
			
			RenderSystem.enableBlend();
			
			matrix.pushPose();
			matrix.scale(this.scale, this.scale, this.scale);
			
			matrix.pushPose();
			matrix.translate(((this.getPosX(s) + (this.getWidth() / 2)) / this.scale), this.getPosY(s) / this.scale, 0.0F);
			matrix.mulPose(Axis.ZP.rotationDegrees(this.rotation));
			matrix.scale(f, f, f);

			int alpha = this.basecolor.getAlpha();
			int i = Mth.ceil(this.opacity * 255.0F);
			if (i < alpha) {
				alpha = i;
			}
			Color c = new Color(this.basecolor.getRed(), this.basecolor.getGreen(), this.basecolor.getBlue(), alpha);
			
			if (this.shadow) {
				font.drawShadow(matrix, splash, -(font.width(splash) / 2), 0, c.getRGB());
			} else {
				font.draw(matrix, splash, -(font.width(splash) / 2), 0, c.getRGB());
			}

			matrix.popPose();
			matrix.popPose();
			
		}

	}
	
	@SubscribeEvent
	public static void onInitScreenPre(InitOrResizeScreenEvent.Pre e) {
		Screen s = Minecraft.getInstance().screen;
		if (s != null) {
			if ((lastScreen == null) || !lastScreen.getClass().getName().equals(s.getClass().getName())) {
				isNewMenu = true;
			}
		}
		lastScreen = s;
	}
	
	@SubscribeEvent
	public static void onMenuReloaded(MenuReloadedEvent e) {
		splashCache.clear();
	}

}
