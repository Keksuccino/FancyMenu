package de.keksuccino.fancymenu.menu.fancy.item;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

public class WebStringCustomizationItem extends CustomizationItemBase {

	public volatile List<String> lines = new ArrayList<String>();
	private volatile boolean updating = false;
	public boolean multiline = false;
	public boolean shadow = false;
	public float scale = 1.0F;
	
	public WebStringCustomizationItem(PropertiesSection item) {
		super(item);
		
		if ((this.action != null) && this.action.equalsIgnoreCase("addwebtext")) {
			this.value = item.getEntryValue("url");
			if (this.value != null) {
				this.value = MenuCustomization.convertString(this.value);
			}
			
			String multi = item.getEntryValue("multiline");
			if ((multi != null) && multi.equalsIgnoreCase("true")) {
				this.multiline = true;
			}
			
			String sh = item.getEntryValue("shadow");
			if ((sh != null)) {
				if (sh.equalsIgnoreCase("true")) {
					this.shadow = true;
				}
			}
			
			String sc = item.getEntryValue("scale");
			if ((sc != null) && MathUtils.isFloat(sc)) {
				this.scale = Float.parseFloat(sc);
			}
			
			this.updateContent(this.value);
			
		}
	}

	@Override
	public void render(GuiScreen menu) throws IOException {
		if (!this.shouldRender()) {
			return;
		}
		
		if (!this.updating) {
			FontRenderer font = Minecraft.getMinecraft().fontRenderer;

			GlStateManager.enableBlend();

			int w = 0;
			int i = 0;
			for (String s : this.lines) {

				float sc = this.scale;
				if (s.startsWith("# ")) {
					s = s.substring(2);
					this.scale += 1.3F;
				}
				if (s.startsWith("## ")) {
					s = s.substring(3);
					this.scale += 1.0F;
				}
				if (s.startsWith("### ")) {
					s = s.substring(4);
					this.scale += 0.5F;
				}
				if (s.startsWith("#### ")) {
					s = s.substring(5);
					this.scale += 0.2F;
				}

				int sw = (int) (font.getStringWidth(s) * this.scale);
				if (w < sw) {
					w = sw;
				}

				int x = this.getPosX(menu);
				int y = this.getPosY(menu);

				GlStateManager.pushMatrix();
				GlStateManager.scale(this.scale, this.scale, this.scale);
				if (this.shadow) {
					font.drawStringWithShadow(s, x, y + (i / this.scale), Color.WHITE.getRGB());
				} else {
					font.drawString(s, x, (int) (y + (i / this.scale)), Color.WHITE.getRGB());
				}
				GlStateManager.popMatrix();

				i += (10*this.scale);

				this.scale = sc;

			}

			this.height = i;
			this.width = w;

			GlStateManager.disableBlend();
		}
	}
	
	@Override
	public int getPosX(GuiScreen menu) {
		int x = super.getPosX(menu);
		x = (int)(x / this.scale);
		return x;
	}

	@Override
	public int getPosY(GuiScreen menu) {
		return (int) (super.getPosY(menu) / this.scale);
	}
	
	@Override
	public boolean shouldRender() {
		if ((this.lines == null) || this.lines.isEmpty()) {
			return false;
		}
		return super.shouldRender();
	}

	public void updateContent(String url) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				updating = true;
				
				String old = value;
				
				value = url;
				if (isValidUrl()) {
					//Get raw github file
					if (value.toLowerCase().contains("/blob/") && (value.toLowerCase().startsWith("http://github.com/")
							|| value.toLowerCase().startsWith("https://github.com/")|| value.toLowerCase().startsWith("http://www.github.com/")
							|| value.toLowerCase().startsWith("https://www.github.com/"))) {
						String path = value.replace("//", "").split("/", 2)[1].replace("/blob/", "/");
						value = "https://raw.githubusercontent.com/" + path;
					}
					
					//Get raw pastebin file
					if (!value.toLowerCase().contains("/raw/") && (value.toLowerCase().startsWith("http://pastebin.com/")
							|| value.toLowerCase().startsWith("https://pastebin.com/")|| value.toLowerCase().startsWith("http://www.pastebin.com/")
							|| value.toLowerCase().startsWith("https://www.pastebin.com/"))) {
						String path = value.replace("//", "").split("/", 2)[1];
						value = "https://pastebin.com/raw/" + path;
					}
					
					try {
						lines.clear();
						
						URL u = new URL(value);
						BufferedReader r = new BufferedReader(new InputStreamReader(u.openStream(), StandardCharsets.UTF_8));
						String s = r.readLine();
						while(s != null) {
							lines.add(StringUtils.convertFormatCodes(s, "&", "§"));
							if (!multiline) {
								break;
							}
							s = r.readLine();
						}
						r.close();
						
					} catch (Exception e) {
						lines.clear();
						lines.add(Locals.localize("customization.items.webstring.unabletoload"));
						e.printStackTrace();
					}

				} else {
					lines.clear();
					lines.add(Locals.localize("customization.items.webstring.unabletoload"));
					
					System.out.println("########################## ERROR ##########################");
					System.out.println("[FM] Cannot load text content from " + value + "! Invalid URL!");
					System.out.println("###########################################################");
					
					value = old;
				}
				
				updating = false;
				
			}
		}).start();
	}
	
	private boolean isValidUrl() {
		if ((this.value == null) || (!this.value.startsWith("http://") && !this.value.startsWith("https://"))) {
			return false;
		}
		
		try {
			URL url = new URL(this.value);
			HttpURLConnection c = (HttpURLConnection) url.openConnection();

			c.setRequestMethod("HEAD");
			int r = c.getResponseCode();
			
			if (r == 200) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Trying alternative method to check for existing url..");
			try {
				URL url = new URL(this.value);
				HttpURLConnection c = (HttpURLConnection) url.openConnection();
				
				int r = c.getResponseCode();
				
				if (r == 200) {
					return true;
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return false;
	}
 
}
