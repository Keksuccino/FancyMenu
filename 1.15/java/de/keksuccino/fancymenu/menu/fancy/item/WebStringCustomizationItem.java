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

import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.core.input.StringUtils;
import de.keksuccino.core.math.MathUtils;
import de.keksuccino.core.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;

public class WebStringCustomizationItem extends CustomizationItemBase {

	private List<String> lines = new ArrayList<String>();
	public boolean multiline = false;
	public boolean shadow = false;
	public float scale = 1.0F;
	
	public WebStringCustomizationItem(PropertiesSection item) {
		super(item);
		
		if ((this.action != null) && this.action.equalsIgnoreCase("addwebtext")) {
			this.value = item.getEntryValue("url");
			
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
	public void render(Screen menu) throws IOException {
		if (!this.shouldRender()) {
			return;
		}
		
		FontRenderer font = Minecraft.getInstance().fontRenderer;

		RenderSystem.enableBlend();
		
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
			
			RenderSystem.pushMatrix();
			RenderSystem.scalef(this.scale, this.scale, this.scale);
			if (this.shadow) {
				font.drawStringWithShadow(s, x, y + (i / this.scale), Color.WHITE.getRGB());
			} else {
				font.drawString(s, x, y + (i / this.scale), Color.WHITE.getRGB());
			}
			RenderSystem.popMatrix();
			
			i += (10*this.scale);
			
			this.scale = sc;
			
		}
		
		this.height = i;
		this.width = w;
		
		RenderSystem.disableBlend();
	}
	
	@Override
	public int getPosX(Screen menu) {
		int x = super.getPosX(menu);
		x = (int)(x / this.scale);
		return x;
	}

	@Override
	public int getPosY(Screen menu) {
		return (int) (super.getPosY(menu) / this.scale);
	}
	
	@Override
	public boolean shouldRender() {
		if ((this.lines == null) || this.lines.isEmpty()) {
			return false;
		}
		return super.shouldRender();
	}

	public boolean updateContent(String url) {
		String old = this.value;
		
		this.value = url;
		if (this.isValidUrl()) {
			//Get raw github file
			if (this.value.toLowerCase().contains("/blob/") && (this.value.toLowerCase().startsWith("http://github.com/")
					|| this.value.toLowerCase().startsWith("https://github.com/")|| this.value.toLowerCase().startsWith("http://www.github.com/")
					|| this.value.toLowerCase().startsWith("https://www.github.com/"))) {
				String path = this.value.replace("//", "").split("/", 2)[1].replace("/blob/", "/");
				this.value = "https://raw.githubusercontent.com/" + path;
			}
			
			//Get raw pastebin file
			if (!this.value.toLowerCase().contains("/raw/") && (this.value.toLowerCase().startsWith("http://pastebin.com/")
					|| this.value.toLowerCase().startsWith("https://pastebin.com/")|| this.value.toLowerCase().startsWith("http://www.pastebin.com/")
					|| this.value.toLowerCase().startsWith("https://www.pastebin.com/"))) {
				String path = this.value.replace("//", "").split("/", 2)[1];
				this.value = "https://pastebin.com/raw/" + path;
			}
			
			try {
				this.lines.clear();
				
				URL u = new URL(this.value);
				BufferedReader r = new BufferedReader(new InputStreamReader(u.openStream(), StandardCharsets.UTF_8));
				String s = r.readLine();
				while(s != null) {
					this.lines.add(StringUtils.convertFormatCodes(s, "&", "§"));
					if (!multiline) {
						break;
					}
					s = r.readLine();
				}
				r.close();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return true;
		} else {
			System.out.println("########################## ERROR ##########################");
			System.out.println("[FM] Cannot load text content from " + this.value + "! Invalid URL!");
			System.out.println("###########################################################");
			
			this.value = old;
		}
		return false;
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
