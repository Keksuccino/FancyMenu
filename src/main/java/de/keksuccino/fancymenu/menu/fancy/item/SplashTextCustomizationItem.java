package de.keksuccino.fancymenu.menu.fancy.item;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.google.common.collect.Lists;
import de.keksuccino.fancymenu.menu.fancy.DynamicValueHelper;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.io.IOUtils;

public class SplashTextCustomizationItem extends CustomizationItemBase {

	private static final Random RANDOM = new Random();
	private static final ResourceLocation SPLASH_TEXTS = new ResourceLocation("texts/splashes.txt");

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
	protected static GuiScreen lastScreen = null;

	protected static Map<String, String> vanillaLikeCache = new HashMap<>();
	
	public SplashTextCustomizationItem(PropertiesSection item) {
		super(item);
		
		if (!init) {
			MinecraftForge.EVENT_BUS.register(SplashTextCustomizationItem.class);
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
			
			this.width = (int) (30 * basescale * this.scale);
			this.height = (int) (10 * basescale * this.scale);
			
		}
	}

	public void render(GuiScreen menu) throws IOException {
		
		if (this.isNewMenuThis) {
			isNewMenu = false;
		}
		this.isNewMenuThis = isNewMenu;
		
		this.width = (int) (30 * basescale * this.scale);
		this.height = (int) (10 * basescale * this.scale);
		
		if (this.shouldRender()) {
			
			this.renderSplash(Minecraft.getMinecraft().fontRenderer, menu);
			
		}
		
	}
	
	protected void renderSplash(FontRenderer font, GuiScreen s) {

		String splash = null;

		if (this.vanillaLike && (this.text == null)) {
			this.text = getVanillaSplash();
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
					splash = DynamicValueHelper.convertFromRaw(splash);
				} else {
					splash = StringUtils.convertFormatCodes(splash, "&", "§");
				}
			}
			
			this.value = splash;
			
			float f = basescale;
			if (this.bounce) {
				f = f - MathHelper.abs(MathHelper.sin((float) (System.currentTimeMillis() % 1000L) / 1000.0F * ((float) Math.PI * 2F)) * 0.1F);
			}
			f = f * 100.0F / (float) (font.getStringWidth(splash) + 32);
			
			GlStateManager.enableBlend();
			
			GlStateManager.pushMatrix();
			GlStateManager.scale(this.scale, this.scale, this.scale);
			
			GlStateManager.pushMatrix();
			GlStateManager.translate(((this.getPosX(s) + (this.width / 2)) / this.scale), this.getPosY(s) / this.scale, 0.0F);
			GlStateManager.rotate(this.rotation, 0.0F, 0.0F, 1.0F);
			GlStateManager.scale(f, f, f);

			int alpha = this.basecolor.getAlpha();
			int i = MathHelper.ceil(this.opacity * 255.0F);
			if (i < alpha) {
				alpha = i;
			}
			Color c = new Color(this.basecolor.getRed(), this.basecolor.getGreen(), this.basecolor.getBlue(), alpha);
			
			if (this.shadow) {
				font.drawStringWithShadow(splash, -(font.getStringWidth(splash) / 2), 0, c.getRGB());
			} else {
				font.drawString(splash, -(font.getStringWidth(splash) / 2), 0, c.getRGB());
			}

			GlStateManager.popMatrix();
			GlStateManager.popMatrix();
			
		}

	}
	
	@SubscribeEvent
	public static void onInitScreenPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		GuiScreen s = Minecraft.getMinecraft().currentScreen;
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

	public static String getVanillaSplash() {
		String sp = "missingno";
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
					sp = list.get(RANDOM.nextInt(list.size()));

					if (sp.hashCode() != 125780783) {
						break;
					}
				}
			}

			Calendar calendar = Calendar.getInstance();
			calendar.setTime(new Date());

			if (calendar.get(2) + 1 == 12 && calendar.get(5) == 24) {
				sp = "Merry X-mas!";
			}
			else if (calendar.get(2) + 1 == 1 && calendar.get(5) == 1) {
				sp = "Happy new year!";
			}
			else if (calendar.get(2) + 1 == 10 && calendar.get(5) == 31) {
				sp = "OOoooOOOoooo! Spooky!";
			}

		} catch (IOException var8) {
		} finally {
			IOUtils.closeQuietly(iresource);
		}

		return sp;
	}

}
