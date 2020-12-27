//AnimationRenderer
//Copyright (c) Keksuccino

package de.keksuccino.core.rendering.animation;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.http.client.utils.URIBuilder;

import de.keksuccino.core.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.loading.LoadingModList;

public class AnimationRenderer implements IAnimationRenderer {
	
	private String resourceDir;
	private int fps;
	private boolean loop;
	private int width;
	private int height;
	private int x;
	private int y;
	private List<ResourceLocation> resources = new ArrayList<ResourceLocation>();
	private boolean stretch = false;
	private boolean hide = false;
	private volatile boolean done = false;
	private String modid;
	
	private static FileSystem jarFileSystem = null;

	private int frame = 0;
	private long prevTime = 0;

	/**
	 * Renders an animation out of multiple images (frames).<br><br>
	 * 
	 * Just create a resource directory inside /resources/assets/ and put all animation frames in it.<br>
	 * The frames must be named like: 1.png, 2.png, 3.png, ...
	 * 
	 * @param resourcePath The path pointing to the resource directory <b>inside</b> /resources/assets/.
	 * @param fps Frames per second. A value of -1 sets the fps to unlimited.
	 * @param loop If the animation should run in an endless loop or just a single time.
	 * @param modid The Mod ID of your mod.
	 */
	public AnimationRenderer(String resourceDir, int fps, boolean loop, int posX, int posY, int width, int height, String modid) {
		this.fps = fps;
		this.loop = loop;
		this.x = posX;
		this.y = posY;
		this.width = width;
		this.height = height;
		this.resourceDir = resourceDir;
		this.modid = modid;
		this.loadAnimationFrames();
	}
	
	private void loadAnimationFrames() {
		try {
			//Loading all frames into ResourceLocations so minecraft can render them
			List<String> resourcePaths = this.getAnimationResource(resourceDir, Minecraft.class);
			for (String s : resourcePaths) {
				resources.add(ResourceLocation.of(s, "/".charAt(0)));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void render() {
		if ((this.resources == null) || (this.resources.isEmpty())) {
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
		this.renderFrame();
		
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
	
	private <T> List<String> getAnimationResource(String path, Class<T> c) throws URISyntaxException, IllegalArgumentException {
		List<String> ltemp = new ArrayList<String>();
		List<String> l = new ArrayList<String>();
		List<String> paths = new ArrayList<String>();
		
		try {
			//I tested multiple ways of getting the direct path to the jar and nothing worked (I think because of forge), so I took this way..
			Field f = ModLoader.class.getDeclaredField("loadingModList");
			f.setAccessible(true);
			LoadingModList lml = (LoadingModList) f.get(ModLoader.get());
			File f1 = lml.getModFileById(this.modid).getFile().getFilePath().toFile();

			//Probably bad and/or lazy way to check, but it works :) 
			boolean isIDE = true;
			if (f1.getPath().endsWith(".jar")) {
				isIDE = false;
			}

	        if (!isIDE) {
	        	//Creating the modjar filesystem only if it wasn't created before
	            if (jarFileSystem == null) {
	            	URI uri = new URIBuilder(f1.toURI()).setScheme("jar:file").build();
	            	jarFileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
	            }
	            
	            Path path2 = jarFileSystem.getPath("/assets/" + path + "/");
	            
	            //Adding all frame filenames to the path list
	            Stream<Path> pathStream = Files.walk(path2, 1);
	            pathStream.forEach(p -> paths.add(p.getFileName().toString()));
		        pathStream.close();
	        } else {
	        	URL url = c.getResource("/assets/" + path + "/");
	    		if (url == null) {
	    			throw new IllegalArgumentException("Resource URL cannot be null!");
	    		}
	    		File f2 = new File(url.toURI());
	    		if ((f2 == null) || (!f2.exists())) {
	    			throw new IllegalArgumentException("Resource path don't exists!");
	    		}
	    		paths.addAll(Arrays.asList(f2.list()));
	        }
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		//Sort out all non-frame-filenames
		for (String s : paths) {
			if (!s.contains(".")) {
				continue;
			}
			String name = new StringBuilder(new StringBuilder(s).reverse().toString().split("[.]", 2)[1]).reverse().toString();
			if (MathUtils.isInteger(name)) {
				ltemp.add(s);
			}
		}
		//Sorting the frames by its name, so they can be played correctly
		Collections.sort(ltemp, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				int frame1 = Integer.valueOf(new StringBuilder(new StringBuilder(o1).reverse().toString().split("[.]", 2)[1]).reverse().toString());
				int frame2 = Integer.valueOf(new StringBuilder(new StringBuilder(o2).reverse().toString().split("[.]", 2)[1]).reverse().toString());
				if (frame1 > frame2) {
					return 1;
				}
				if (frame1 < frame2) {
					return -1;
				}
				return 0;
			}
		});
		for (String s : ltemp) {
			l.add(path + "/" + s);
		}
		return l;
	}
	
	private void renderFrame() {
		int h = this.height;
		int w = this.width;
		int x2 = this.x;
		int y2 = this.y;
		
		if (this.stretch) {
			h = Minecraft.getInstance().currentScreen.height;
			w = Minecraft.getInstance().currentScreen.width;
			x2 = 0;
			y2 = 0;
		}
		
		Minecraft.getInstance().getTextureManager().bindTexture(this.resources.get(this.frame));
		GlStateManager.enableBlend();
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		GuiIngame.drawModalRectWithCustomSizedTexture(x2, y2, 0.0F, 0.0F, w, h, w, h);
		GlStateManager.disableBlend();
	}
	
	private void updateFrame(long time) {
		this.frame++;
		this.prevTime = time;
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
		return true;
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
	public void prepareAnimation() {
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
