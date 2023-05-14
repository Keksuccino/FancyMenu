package de.keksuccino.fancymenu.customization.slideshow;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.rendering.texture.ExternalTextureHandler;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.fancymenu.properties.PropertyContainer;
import de.keksuccino.fancymenu.properties.PropertiesSerializer;
import de.keksuccino.fancymenu.properties.PropertyContainerSet;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;

public class ExternalTextureSlideshowRenderer extends GuiComponent {
	
	protected List<ExternalTextureResourceLocation> images = new ArrayList<>();
	protected ExternalTextureResourceLocation overlay_texture;
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
	
	protected ExternalTextureResourceLocation previous;
	protected ExternalTextureResourceLocation current;

	public ExternalTextureSlideshowRenderer(String slideshowDir) {
		this.dir = slideshowDir;
		File props = new File(this.dir + "/properties.txt");
		
		if (props.exists()) {
			
			PropertyContainerSet s = PropertiesSerializer.deserializePropertyContainerSet(props.getPath());
			
			if (s != null) {
				List<PropertyContainer> l = s.getSectionsOfType("slideshow-meta");
				if ((l != null) && !l.isEmpty()) {
					this.name = l.get(0).getValue("name");
					if (this.name == null) {
						System.out.println("############## ERROR [FANCYMENU] ##############");
						System.out.println("Missing 'name' value in properties file for slideshow: " + this.dir);
						System.out.println("###############################################");
					}
					
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
					
				} else {
					System.out.println("############## ERROR [FANCYMENU] ##############");
					System.out.println("Missing 'slideshow-meta' section in properties file for slideshow: " + this.dir);
					System.out.println("###############################################");
				}
			} else {
				System.out.println("############## ERROR [FANCYMENU] ##############");
				System.out.println("An error happened while trying to get properties for slideshow: " + this.dir);
				System.out.println("###############################################");
			}
			
		} else {
			System.out.println("############## ERROR [FANCYMENU] ##############");
			System.out.println("Properties file not found for slideshow: " + this.dir);
			System.out.println("###############################################");
		}
	}
	
	public void prepareSlideshow() {
		if (!this.prepared && (this.name != null)) {
			
			File imagesDir = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(this.dir + "/images"));
			
			if (imagesDir.exists() && imagesDir.isDirectory()) {
				
				String[] list = imagesDir.list();
				List<String> images = Arrays.asList(list);
				
				if (!images.isEmpty()) {
					
					Collections.sort(images, String.CASE_INSENSITIVE_ORDER);

					for (String s : images) {
						File f = new File(imagesDir.getPath() + "/" + s);
						if (f.exists() && f.isFile() && (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".png"))) {
							ExternalTextureResourceLocation r = ExternalTextureHandler.INSTANCE.getTexture(f.getPath());
							this.images.add(r);
						}
					}
					if (!this.images.isEmpty()) {
						this.originalWidth = this.images.get(0).getWidth();
						this.originalHeight = this.images.get(0).getHeight();
					}
					
					File overlay = new File(this.dir + "/overlay.png");
					if (overlay.exists()) {
						this.overlay_texture = new ExternalTextureResourceLocation(overlay.getPath());
					}
					
				}
				
				this.prepared = true;
				
			}
			
		}
	}

	public void render(PoseStack matrix) {

		try {
			
			if (!this.images.isEmpty()) {
				
				this.tick();
				
				this.renderCurrent(matrix);
				
				this.renderPrevious(matrix);
				
				this.renderOverlay(matrix);
				
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

	protected void renderPrevious(PoseStack matrix) {
		if ((this.previous != null) && (this.current != this.previous)) {
			if (!this.previous.isReady()) {
				this.previous.loadTexture();
			}
			matrix.pushPose();
			RenderSystem.enableBlend();
			float o = this.opacity;
			if (o > this.slideshowOpacity) {
				o = this.slideshowOpacity;
			}
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, o);
			ResourceLocation r = this.previous.getResourceLocation();
			if (r != null) {
				RenderUtils.bindTexture(r);
				blit(matrix, this.x, this.y, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
			}
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			matrix.popPose();
		}
	}

	protected void renderCurrent(PoseStack matrix) {
		if (this.current != null) {
			if (!this.current.isReady()) {
				this.current.loadTexture();
			}
			RenderSystem.enableBlend();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.slideshowOpacity);
			ResourceLocation r = this.current.getResourceLocation();
			if (r != null) {
				RenderUtils.bindTexture(r);
				blit(matrix, this.x, this.y, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
			}
		}
	}

	protected void renderOverlay(PoseStack matrix) {
		if (this.overlay_texture != null) {
			if (!this.overlay_texture.isReady()) {
				this.overlay_texture.loadTexture();
			}
			RenderSystem.enableBlend();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			ResourceLocation r = this.overlay_texture.getResourceLocation();
			if (r != null) {
				RenderUtils.bindTexture(r);
				blit(matrix, this.x, this.y, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
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

}
