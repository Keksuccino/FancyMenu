package de.keksuccino.fancymenu.customization.slideshow;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExternalTextureSlideshowRenderer {

	public List<ResourceSupplier<ITexture>> images = new ArrayList<>();
	@Nullable
	public ResourceSupplier<ITexture> overlayTexture;
	protected String name = null;
	public String dir;
	protected boolean prepared = false;
	protected double imageDuration = 10.0D;
	protected float fadeSpeed = 1.0F;
	protected int originalWidth = 10;
	protected int originalHeight = 10;
	public int width = 50;
	public int height = 50;
	public int x = 0;
	public int y = 0;
	protected float opacity = 1.0F;
	protected int imageTick = -1;
	protected long opacityTick = -1;
	protected long lastChange = -1;
	protected boolean firstLoop = true;
	public float slideshowOpacity = 1.0F;
	protected ResourceSupplier<ITexture> previous;
	protected ResourceSupplier<ITexture> current;

	public ExternalTextureSlideshowRenderer(String slideshowDir) {
		this.dir = slideshowDir;
		File props = new File(this.dir + "/properties.txt");
		if (!props.isFile()) props = new File(this.dir + "/properties.txt.txt");
		
		if (props.exists()) {
			
			PropertyContainerSet s = PropertiesParser.deserializeSetFromFile(props.getPath());
			
			if (s != null) {
				List<PropertyContainer> l = s.getContainersOfType("slideshow-meta");
				if (!l.isEmpty()) {
					this.name = l.get(0).getValue("name");
					
					String dur = l.get(0).getValue("duration");
					if ((dur != null) && MathUtils.isDouble(dur)) {
						this.imageDuration = Double.parseDouble(dur);
					}
					
					String fs = l.get(0).getValue("fadespeed");
					if ((fs != null) && MathUtils.isFloat(fs)) {
						float f = Float.parseFloat(fs);
						if (f < 0.0F) {
							f = 0.0F;
						}
						this.fadeSpeed = f;
					}
					
					String sx = l.get(0).getValue("x");
					if ((sx != null) && MathUtils.isInteger(sx)) {
						this.x = Integer.parseInt(sx);
					}
					
					String sy = l.get(0).getValue("y");
					if ((sy != null) && MathUtils.isInteger(sy)) {
						this.y = Integer.parseInt(sy);
					}
					
					String sw = l.get(0).getValue("width");
					if ((sw != null) && MathUtils.isInteger(sw)) {
						this.width = Integer.parseInt(sw);
					}
					
					String sh = l.get(0).getValue("height");
					if ((sh != null) && MathUtils.isInteger(sh)) {
						this.height = Integer.parseInt(sh);
					}
					
				}
			}
			
		}
	}
	
	public void prepareSlideshow() {
		if (!this.prepared && (this.name != null)) {
			
			File imagesDir = new File(GameDirectoryUtils.getAbsoluteGameDirectoryPath(this.dir + "/images"));
			
			if (imagesDir.exists() && imagesDir.isDirectory()) {
				
				String[] list = imagesDir.list();
				List<String> images = (list != null) ? Arrays.asList(list) : new ArrayList<>();
				
				if (!images.isEmpty()) {
					
					images.sort(String.CASE_INSENSITIVE_ORDER);

					for (String s : images) {
						File f = new File(imagesDir.getPath() + "/" + s);
						if (f.exists() && f.isFile() && (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".png"))) {
							this.images.add(ResourceSupplier.image(f.getPath()));
						}
					}
					if (!this.images.isEmpty()) {
						ITexture t = this.images.get(0).get();
						if (t != null) {
							t.waitForReady(5000);
							this.originalWidth = t.getWidth();
							this.originalHeight = t.getHeight();
						}
					}
					
					File overlay = new File(this.dir + "/overlay.png");
					if (overlay.exists()) {
						this.overlayTexture = ResourceSupplier.image(overlay.getPath());
					}
					
				}
				
				this.prepared = true;
				
			}
			
		}
	}

	public void render(GuiGraphics graphics) {
		try {
			if (!this.images.isEmpty()) {
				this.tick();
				this.renderCurrent(graphics);
				this.renderPrevious(graphics);
				this.renderOverlay(graphics);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void tick() {
		
		if (!this.images.isEmpty()) {
			
			long time = System.currentTimeMillis();
			long duration = (long) (1000 * this.imageDuration);
			long opacityTickSpeed = 25;
			
			if (firstLoop) {
				duration = duration / 2;
			}
			
			//switch to next image
			if ((this.previous == null) && ((this.lastChange + duration) < time)) {
				this.imageTick++;
				if (this.imageTick > this.images.size()-1) {
					this.imageTick = 0;
				}
				this.lastChange = time;
				this.opacity = 1.0F;
				this.previous = this.current;
				this.current = this.images.get(this.imageTick);
			}
			
			//lower opacity when prev image is set to fade it out
			if ((this.previous != null) && (this.opacity > 0.0F)) {
				this.firstLoop = false;
				if ((this.opacityTick + opacityTickSpeed) < time) {
					this.opacityTick = time;
					this.opacity = this.opacity - (0.005F * this.fadeSpeed);
					if (this.opacity < 0.0F) {
						this.opacity = 0.0F;
					}
				}
			} else {
				this.previous = null;
			}
			
		}
		
	}

	protected void renderPrevious(GuiGraphics graphics) {
		if ((this.previous != null) && (this.current != this.previous)) {
			graphics.pose().pushPose();
			RenderSystem.enableBlend();
			float o = this.opacity;
			if (o > this.slideshowOpacity) {
				o = this.slideshowOpacity;
			}
			graphics.setColor(1.0F, 1.0F, 1.0F, o);
			ITexture t = this.previous.get();
			ResourceLocation loc = (t != null) ? t.getResourceLocation() : null;
			if (loc != null) {
				graphics.blit(loc, this.x, this.y, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
			}
			graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
			graphics.pose().popPose();
		}
	}

	protected void renderCurrent(GuiGraphics graphics) {
		if (this.current != null) {
			RenderSystem.enableBlend();
			graphics.setColor(1.0F, 1.0F, 1.0F, this.slideshowOpacity);
			ITexture t = this.current.get();
			ResourceLocation loc = (t != null) ? t.getResourceLocation() : null;
			if (loc != null) {
				graphics.blit(loc, this.x, this.y, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
			}
		}
	}

	protected void renderOverlay(GuiGraphics graphics) {
		if (this.overlayTexture != null) {
			RenderSystem.enableBlend();
			graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
			ITexture t = this.overlayTexture.get();
			ResourceLocation loc = (t != null) ? t.getResourceLocation() : null;
			if (loc != null) {
				graphics.blit(loc, this.x, this.y, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
			}
		}
	}
	
	public String getName() {
		return this.name;
	}

	public void setDuration(double duration) {
		this.imageDuration = duration;
	}
	
	public void setFadeSpeed(float speed) {
		if (speed < 0.0F) {
			speed = 0.0F;
		}
		this.fadeSpeed = speed;
	}
	
	public boolean isReady() {
		return this.prepared;
	}

	public int getImageWidth() {
		return this.originalWidth;
	}

	public int getImageHeight() {
		return this.originalHeight;
	}

	public int getImageCount() {
		return this.images.size();
	}

}
