package de.keksuccino.fancymenu.menu.fancy.item;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import de.keksuccino.fancymenu.menu.fancy.DynamicValueHelper;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.resources.SelfcleaningDynamicTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;

public class WebTextureCustomizationItem extends CustomizationItemBase {

	public static Map<String, WebTexture> cachedWebImages = new HashMap<>();

	public volatile ResourceLocation texture;
	public volatile WebTexture webTexture;
	public String rawURL = "";
	public volatile boolean ready = false;

	public static volatile Map<String, WebTexture> textureCache = new HashMap<>();

	public WebTextureCustomizationItem(PropertiesSection item) {
		super(item);

		if ((this.action != null) && this.action.equalsIgnoreCase("addwebtexture")) {
			this.value = item.getEntryValue("url");
			if (this.value != null) {
				this.rawURL = this.value;
				this.value = DynamicValueHelper.convertFromRaw(this.value);

				if ((this.width <= 0) && (this.height <= 0)) {
					this.setWidth(100);
				}

				if (cachedWebImages.containsKey(this.actionId)) {
					this.webTexture = cachedWebImages.get(this.actionId);
					if ((this.webTexture.getLocation() != null) && this.webTexture.textureURL.equals(this.value)) {
						this.calculateAspectRatio();
						this.ready = true;
					} else {
						this.webTexture = null;
						this.texture = null;
					}
				}

				if (this.webTexture == null) {
					new Thread(() -> {
						try {

							if (isValidUrl(this.value)) {
								this.webTexture = getWebTexture(this.value);
								cachedWebImages.put(this.actionId, this.webTexture);
								this.calculateAspectRatio();
							}

							this.ready = true;

						} catch (Exception e) {
							e.printStackTrace();
						}
					}).start();
				}

			}
		}

	}

	protected void calculateAspectRatio() {
		if (this.webTexture == null) {
			if (this.width <= 0) {
				this.setWidth(100);
			}
			if (this.height <= 0) {
				this.setHeight(100);
			}
			this.ready = true;
			return;
		}
		int w = this.webTexture.width;
		int h = this.webTexture.height;
		double ratio = (double) w / (double) h;
		//Calculate missing width
		if ((this.getWidth() < 0) && (this.getHeight() >= 0)) {
			this.setWidth((int)(this.getHeight() * ratio));
		}
		//Calculate missing height
		if ((this.getHeight() < 0) && (this.getWidth() >= 0)) {
			this.setHeight((int)(this.getWidth() / ratio));
		}
	}

	@Override
	public void render(GuiScreen menu) throws IOException {

		if (this.shouldRender() || isEditorActive()) {

			int x = this.getPosX(menu);
			int y = this.getPosY(menu);

			if (this.webTexture != null) {
				this.texture = this.webTexture.getLocation();
			}

			if (this.isTextureReady()) {
				RenderUtils.bindTexture(this.texture);
				GlStateManager.enableBlend();
				GlStateManager.color(1.0F, 1.0F, 1.0F, this.opacity);
				drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
				GlStateManager.disableBlend();
			} else if (isEditorActive()) {
				drawRect(this.getPosX(menu), this.getPosY(menu), this.getPosX(menu) + this.getWidth(), this.getPosY(menu) + this.getHeight(), Color.MAGENTA.getRGB());
				if (this.ready) {
					drawCenteredString(Minecraft.getMinecraft().fontRenderer, "§lMISSING", this.getPosX(menu) + (this.width / 2), this.getPosY(menu) + (this.height / 2) - (Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT / 2), -1);
				}
			}

			if (!this.ready && isEditorActive()) {
				drawCenteredString(Minecraft.getMinecraft().fontRenderer, "§lLOADING TEXTURE..", this.getPosX(menu) + (this.width / 2), this.getPosY(menu) + (this.height / 2) - (Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT / 2), -1);
			}

		}
	}

	public boolean isTextureReady() {
		return ((this.texture != null) && this.ready);
	}

	@Override
	public boolean shouldRender() {
		if ((this.getWidth() < 0) || (this.getHeight() < 0)) {
			return false;
		}
		return super.shouldRender();
	}

	public static boolean isValidUrl(String url) {
		if ((url != null) && (url.startsWith("http://") || url.startsWith("https://"))) {
			try {
				URL u = new URL(url);
				HttpURLConnection c = (HttpURLConnection)u.openConnection();
				c.addRequestProperty("User-Agent", "Mozilla/4.0");
				c.setRequestMethod("HEAD");
				int r = c.getResponseCode();
				if (r == 200) {
					return true;
				}
			} catch (Exception e1) {
				try {
					URL u = new URL(url);
					HttpURLConnection c = (HttpURLConnection)u.openConnection();
					c.addRequestProperty("User-Agent", "Mozilla/4.0");
					int r = c.getResponseCode();
					if (r == 200) {
						return true;
					}
				} catch (Exception e2) {}
			}
			return false;
		}
		return false;
	}

	public static WebTexture getWebTexture(String url) {
		WebTexture wt = null;
		try {
			if (!textureCache.containsKey(url)) {
				URL u = new URL(url);
				HttpURLConnection httpcon = (HttpURLConnection)u.openConnection();
				httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");
				InputStream s = httpcon.getInputStream();
				if (s != null) {
					BufferedImage i = ImageIO.read(s);
					wt = new WebTexture(i, url);
					textureCache.put(url, wt);
					s.close();
				}
			} else {
				wt = textureCache.get(url);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return wt;
	}

	public static class WebTexture {

		public int width;
		public int height;
		public ResourceLocation location = null;
		public BufferedImage image;
		public String textureURL;

		public WebTexture(BufferedImage image, String url) {
			this.image = image;
			if (image != null) {
				this.width = image.getWidth();
				this.height = image.getHeight();
			}
			this.textureURL = url;
		}

		public ResourceLocation getLocation() {
			try {
				if (this.location == null) {
					if (this.image != null) {
						this.location = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("webtexture", new SelfcleaningDynamicTexture(this.image));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			this.image = null;
			return this.location;
		}

	}

}
