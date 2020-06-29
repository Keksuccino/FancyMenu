//AnimationRenderer
//Copyright (c) Keksuccino

package de.keksuccino.core.rendering.animation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.core.resources.ExternalTextureResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IngameGui;

public class ExternalTextureAnimationRenderer implements IAnimationRenderer {
	
	private String resourceDir;
	private int fps;
	private boolean loop;
	private int width;
	private int height;
	private int x;
	private int y;
	private List<ExternalTextureResourceLocation> resources = new ArrayList<ExternalTextureResourceLocation>();
	private List<String> resourcePaths = new ArrayList<String>();
	private boolean stretch = false;
	private boolean hide = false;
	private volatile boolean done = false;
	
	private volatile boolean ready = false;

	private int frame = 0;
	private long prevTime = 0;

	/**
	 * Renders an animation out of multiple images (frames) stored outside of the mod JAR.<br><br>
	 * 
	 * Just create a new directory and put all animation frames in it.<br>
	 * The frames must be named like: 1.png, 2.png, 3.png, ...
	 * 
	 * @param resourcePath The path pointing to the animation resource directory.
	 * @param fps Frames per second. A value of -1 sets the fps to unlimited.
	 * @param loop If the animation should run in an endless loop or just a single time.
	 */
	public ExternalTextureAnimationRenderer(String resourceDir, int fps, boolean loop, int posX, int posY, int width, int height) {
		this.fps = fps;
		this.loop = loop;
		this.x = posX;
		this.y = posY;
		this.width = width;
		this.height = height;
		this.resourceDir = resourceDir;
	}
	
	public ExternalTextureAnimationRenderer(int fps, boolean loop, int posX, int posY, int width, int height, String... resourcePaths) {
		this.fps = fps;
		this.loop = loop;
		this.x = posX;
		this.y = posY;
		this.width = width;
		this.height = height;
		this.resourcePaths.addAll(Arrays.asList(resourcePaths));
	}
	
	/**
	 * Needs to be called before calling {@code ExternalTextureAnimationManager.render()} and after minecraft's {@link TextureManager} instance was loaded.
	 */
	@Override
	public void prepareAnimation() {
		try {
			//Loading all frames into ResourceLocations so minecraft can render them
			if (this.resourcePaths.isEmpty()) {
				File f = new File(this.resourceDir);
				if (f.isDirectory() && f.exists()) {
					for (File in : f.listFiles()) {
						if (isValidFrame(in)) {
							ExternalTextureResourceLocation er = new ExternalTextureResourceLocation(in.getAbsolutePath());
							er.loadTexture();
							if (er.isReady()) {
								this.resources.add(er);
							}
						}
					}
				}
			} else {
				for (String s : this.resourcePaths) {
					File in = new File(s);
					if (isValidFrame(in)) {
						ExternalTextureResourceLocation er = new ExternalTextureResourceLocation(in.getAbsolutePath());
						er.loadTexture();
						if (er.isReady()) {
							this.resources.add(er);
						}
					}
				}
			}
			
			//Sorting the frames by its name, so they can be played in the correct order
			Collections.sort(this.resources, new Comparator<ExternalTextureResourceLocation>() {
				@Override
				public int compare(ExternalTextureResourceLocation o1, ExternalTextureResourceLocation o2) {
					File f1 = new File(o1.getPath());
					File f2 = new File(o2.getPath());
					int frame1 = Integer.valueOf(new StringBuilder(new StringBuilder(f1.getName()).reverse().toString().split("[.]", 2)[1]).reverse().toString());
					int frame2 = Integer.valueOf(new StringBuilder(new StringBuilder(f2.getName()).reverse().toString().split("[.]", 2)[1]).reverse().toString());
					if (frame1 > frame2) {
						return 1;
					}
					if (frame1 < frame2) {
						return -1;
					}
					return 0;
				}
			});
			this.ready = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void render(MatrixStack matrix) {
		if ((this.resources == null) || (this.resources.isEmpty())) {
			return;
		}

		if (!this.ready) {
			return;
		}
		
		//A value of -1 sets the max fps to unlimited
		if (this.fps < 0) {
			this.fps = -1;
		}

		if (this.frame > this.resources.size()-1) {
			if (this.loop) {
				this.resetAnimation();
			} else {
				this.done = true;
				if (!this.hide) {
					this.frame = this.resources.size()-1;
				} else {
					return;
				}
			}
		}

		//Rendering the current frame
		this.renderFrame(matrix);
		
		//Updating the current frame based on the fps value
		long time = System.currentTimeMillis();
		if (this.fps == -1) {
			this.updateFrame(time);
		} else {
			if ((this.prevTime + (1000 / this.fps)) <= time) {
				this.updateFrame(time);
			}
		}
	}
	
	private void renderFrame(MatrixStack matrix) {
		int h = this.height;
		int w = this.width;
		int x2 = this.x;
		int y2 = this.y;
		
		if (this.stretch) {
			h = Minecraft.getInstance().currentScreen.field_230709_l_;
			w = Minecraft.getInstance().currentScreen.field_230708_k_;
			x2 = 0;
			y2 = 0;
		}
		
		Minecraft.getInstance().getTextureManager().bindTexture(this.resources.get(this.frame).getResourceLocation());
		RenderSystem.enableBlend();
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		IngameGui.func_238463_a_(matrix, x2, y2, 0.0F, 0.0F, w, h, w, h);
		RenderSystem.disableBlend();
	}
	
	private void updateFrame(long time) {
		this.frame++;
		this.prevTime = time;
	}
	
	private static boolean isValidFrame(File f) {
		if (!f.exists() || !f.isFile()) {
			return false;
		}
		String name = f.getName();
		if (!name.endsWith(".jpg") && !name.endsWith(".png")) {
			return false;
		}
		return true;
	}
	
	@Override
	public void resetAnimation() {
		this.frame = 0;
		this.prevTime = 0;
		this.done = false;
	}

	@Override
	public void setStretchImageToScreensize(boolean b) {
		this.stretch = b;
	}

	@Override
	public void setHideAfterLastFrame(boolean b) {
		this.hide = b;
	}

	@Override
	public boolean isFinished() {
		return this.done;
	}
	
	@Override
	public void setWidth(int width) {
		this.width = width;
	}
	
	@Override
	public void setHeight(int height) {
		this.height = height;
	}
	
	@Override
	public int currentFrame() {
		return this.frame;
	}

	@Override
	public boolean isReady() {
		return this.ready;
	}

	@Override
	public void setPosX(int x) {
		this.x = x;
	}

	@Override
	public void setPosY(int y) {
		this.y = y;
	}
	
	@Override
	public int animationFrames() {
		return this.resources.size();
	}

	@Override
	public String getPath() {
		return this.resourceDir;
	}

	@Override
	public void setFPS(int fps) {
		this.fps = fps;
	}

	@Override
	public int getFPS() {
		return this.fps;
	}
	
	@Override
	public void setLooped(boolean b) {
		this.loop = b;
	}

	@Override
	public boolean isGettingLooped() {
		return this.loop;
	}

	@Override
	public boolean isStretchedToStreensize() {
		return this.stretch;
	}
	
	@Override
	public int getWidth() {
		return this.width;
	}

	@Override
	public int getHeight() {
		return this.height;
	}

	@Override
	public int getPosX() {
		return this.x;
	}

	@Override
	public int getPosY() {
		return this.y;
	}

}
