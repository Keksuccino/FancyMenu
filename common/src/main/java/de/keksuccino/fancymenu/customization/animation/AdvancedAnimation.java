package de.keksuccino.fancymenu.customization.animation;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.animation.exceptions.AnimationNotFoundException;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resources.PlayableResource;
import de.keksuccino.fancymenu.util.resources.RenderableResource;
import de.keksuccino.konkrete.rendering.animation.ExternalTextureAnimationRenderer;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//TODO Rewrite animations

@SuppressWarnings("all")
public class AdvancedAnimation implements IAnimationRenderer, RenderableResource, PlayableResource {
	
	private final IAnimationRenderer introRenderer;
	private final IAnimationRenderer animationRenderer;
	private boolean started = false;
	private final String mainAudioPath;
	private final String introAudioPath;
	private boolean muted = false;
	private final boolean replayIntro;
	public String propertiesPath = null;
	protected boolean prepared = false;
	//--------------------
	protected boolean playing = false;
	
	/**
	 * Container to hold a {@link IAnimationRenderer} instance with an optional intro which plays before the main animation starts.
	 *
	 * @throws AnimationNotFoundException If the main animation is null.
	 */
	public AdvancedAnimation(@Nullable IAnimationRenderer introAnimation, IAnimationRenderer mainAnimation, @Nullable String introAudioPath, @Nullable String mainAudioPath, boolean replayIntro) throws AnimationNotFoundException {
		if (mainAnimation != null) {
			this.animationRenderer = mainAnimation;
		} else {
			throw new AnimationNotFoundException("Animation cannot be null!");
		}
		this.introRenderer = introAnimation;
		this.mainAudioPath = mainAudioPath;
		this.introAudioPath = introAudioPath;
		this.replayIntro = replayIntro;
	}
	
	public boolean hasIntro() {
		return (this.introRenderer != null);
	}
	
	@Override
	public boolean isReady() {
		if ((this.animationRenderer != null) && this.hasIntro()) {
			if (this.animationRenderer.isReady() && this.introRenderer.isReady()) {
				return true;
			}
		} else if (this.animationRenderer != null) {
			return this.animationRenderer.isReady();
		}
		
		return false;
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public void prepareAnimation() {
		if (!this.prepared) {
			if (this.animationRenderer != null) {
				this.animationRenderer.prepareAnimation();
			}
			if (this.hasIntro()) {
				this.introRenderer.prepareAnimation();
			}
			this.prepared = true;
		}
	}
	
	/**
	 * Resets the animation to the first frame and replays the intro.
	 */
	@Override
	public void resetAnimation() {
		this.stopAudio();
		this.resetAudio();
		if (this.animationRenderer != null) {
			this.animationRenderer.resetAnimation();
		}
		if (this.hasIntro()) {
			this.introRenderer.resetAnimation();
		}
		this.started = false;
	}
	
	public boolean hasStarted() {
		return this.started;
	}

	@Deprecated
	@Override
	public void render(PoseStack pose) {
		if (this.isReady()) {

			//This is to force-start playing the animation if the (deprecated) render method is used
			this.playing = true;

			this.started = true;
			
			if (!this.muted) {
				if (this.hasIntroAudio() && !this.introRenderer.isFinished() && ((this.introRenderer.currentFrame() == 1) || (this.introRenderer.currentFrame() > 1) && !SoundHandler.isPlaying(introAudioPath))) {
					SoundHandler.stopSound(mainAudioPath);
					SoundHandler.registerSound(introAudioPath, introAudioPath);
					SoundHandler.resetSound(introAudioPath);
					SoundHandler.playSound(introAudioPath);
				}
				if (this.hasIntroAudio() && this.introRenderer.isFinished()) {
					SoundHandler.stopSound(introAudioPath);
				}
				if (this.hasMainAudio() && !this.animationRenderer.isFinished() && ((this.animationRenderer.currentFrame() == 1) || (this.animationRenderer.currentFrame() > 1) && !SoundHandler.isPlaying(mainAudioPath))) {
					if (this.hasIntroAudio()) {
						SoundHandler.stopSound(introAudioPath);
					}
					SoundHandler.registerSound(mainAudioPath, mainAudioPath);
					SoundHandler.resetSound(mainAudioPath);
					SoundHandler.playSound(mainAudioPath);
					SoundHandler.setLooped(mainAudioPath, true);
				}
			}
			
			if (this.hasIntro()) {
				this.introRenderer.setFPS(this.animationRenderer.getFPS());
				this.introRenderer.setWidth(this.animationRenderer.getWidth());
				this.introRenderer.setHeight(this.animationRenderer.getHeight());
				this.introRenderer.setPosX(this.animationRenderer.getPosX());
				this.introRenderer.setPosY(this.animationRenderer.getPosY());
				this.introRenderer.setLooped(false);
				if (!this.introRenderer.isFinished()) {
					if (canRenderFrameOf(this.introRenderer, this.introRenderer.currentFrame())) {
						this.introRenderer.render(pose);
					}
				} else {
					if (canRenderFrameOf(this.animationRenderer, this.animationRenderer.currentFrame())) {
						this.animationRenderer.render(pose);
					}
				}
			} else {
				if (canRenderFrameOf(this.animationRenderer, this.animationRenderer.currentFrame())) {
					this.animationRenderer.render(pose);
				}
			}

		}
		
		if (this.isFinished() || this.muted) {
			this.stopAudio();
		}

	}

	@Override
	public void setStretchImageToScreensize(boolean b) {
		if (this.hasIntro()) {
			this.introRenderer.setStretchImageToScreensize(b);
		}
		if (this.animationRenderer != null) {
			this.animationRenderer.setStretchImageToScreensize(b);
		}
	}

	@Override
	public void setHideAfterLastFrame(boolean b) {
		if (this.hasIntro()) {
			this.introRenderer.setHideAfterLastFrame(b);
		}
		if (this.animationRenderer != null) {
			this.animationRenderer.setHideAfterLastFrame(b);
		}
	}

	@Override
	public boolean isFinished() {
		if (this.hasIntro() && (this.animationRenderer != null)) {
			if (this.introRenderer.isFinished() && this.animationRenderer.isFinished()) {
				return true;
			}
		} else if (this.animationRenderer != null) {
			return this.animationRenderer.isFinished();
		}
		return false;
	}

	@Override
	public void setWidth(int width) {
		if (this.hasIntro()) {
			this.introRenderer.setWidth(width);
		}
		if (this.animationRenderer != null) {
			this.animationRenderer.setWidth(width);
		}
	}

	@Override
	public void setHeight(int height) {
		if (this.hasIntro()) {
			this.introRenderer.setHeight(height);
		}
		if (this.animationRenderer != null) {
			this.animationRenderer.setHeight(height);
		}
	}

	@Override
	public void setPosX(int x) {
		if (this.hasIntro()) {
			this.introRenderer.setPosX(x);
		}
		if (this.animationRenderer != null) {
			this.animationRenderer.setPosX(x);
		}
	}

	@Override
	public void setPosY(int y) {
		if (this.hasIntro()) {
			this.introRenderer.setPosY(y);
		}
		if (this.animationRenderer != null) {
			this.animationRenderer.setPosY(y);
		}
	}

	@Override
	public int currentFrame() {
		int i = 0;
		if (this.hasIntro()) {
			i = this.introRenderer.currentFrame();
		}
		if (this.animationRenderer != null) {
			i += this.animationRenderer.currentFrame();
		}
		return i;
	}

	@Override
	public int animationFrames() {
		int i = 0;
		if (this.hasIntro()) {
			i = this.introRenderer.animationFrames();
		}
		if (this.animationRenderer != null) {
			i += this.animationRenderer.animationFrames();
		}
		return i;
	}

	@Override
	public String getPath() {
		if (this.animationRenderer != null) {
			return new File(this.animationRenderer.getPath()).toPath().getParent().toString();
		}
		return null;
	}

	@Override
	public void setFPS(int fps) {
		if (this.hasIntro()) {
			this.introRenderer.setFPS(fps);
		}
		if (this.animationRenderer != null) {
			this.animationRenderer.setFPS(fps);
		}
	}

	@Override
	public void setLooped(boolean b) {
		if (this.animationRenderer != null) {
			this.animationRenderer.setLooped(b);
		}
	}

	@Override
	public int getFPS() {
		if (this.animationRenderer != null) {
			return this.animationRenderer.getFPS();
		}
		return 0;
	}

	@Override
	public boolean isGettingLooped() {
		return this.animationRenderer.isGettingLooped();
	}

	@Override
	public boolean isStretchedToStreensize() {
		if (this.animationRenderer != null) {
			return this.animationRenderer.isStretchedToStreensize();
		}
		return false;
	}

	@Override
	public int getWidth() {
		return this.animationRenderer.getWidth();
	}

	@Override
	public int getHeight() {
		return this.animationRenderer.getHeight();
	}

	@Override
	public @NotNull AspectRatio getAspectRatio() {
		return null;
	}

	@Override
	public int getPosX() {
		return this.animationRenderer.getPosX();
	}

	@Override
	public int getPosY() {
		return this.animationRenderer.getPosY();
	}
	
	public void setMuteAudio(boolean b) {
		this.muted = b;
	}

	public boolean hasMainAudio() {
		return (this.mainAudioPath != null);
	}

	public boolean hasIntroAudio() {
		return (this.hasIntro() && (this.introAudioPath != null));
	}

	public void stopAudio() {
		SoundHandler.stopSound(mainAudioPath);
		if (this.hasIntro()) {
			SoundHandler.stopSound(introAudioPath);
		}
	}

	public void resetAudio() {
		SoundHandler.resetSound(mainAudioPath);
		if (this.hasIntro()) {
			SoundHandler.resetSound(introAudioPath);
		}
	}

	public IAnimationRenderer getMainAnimationRenderer() {
		return this.animationRenderer;
	}

	public IAnimationRenderer getIntroAnimationRenderer() {
		return this.introRenderer;
	}
	
	public boolean replayIntro() {
		return this.replayIntro;
	}

	@Override
	public void setOpacity(float opacity) {
		if (this.animationRenderer != null) {
			this.animationRenderer.setOpacity(opacity);
		}
		if (this.introRenderer != null) {
			this.introRenderer.setOpacity(opacity);
		}
	}

	protected static void tickRenderer(@NotNull IAnimationRenderer renderer) {

		List<ResourceLocation> frames = getFramesOf(renderer);
		int fps = renderer.getFPS();

		if (frames.isEmpty()) {
			return;
		}

		//A value of -1 sets the max fps to unlimited
		if (fps <= 0) {
			fps = -1;
		}

		//Reset animation if last frame reached
		if ((renderer.currentFrame() >= frames.size()) && renderer.isGettingLooped()) {
			renderer.resetAnimation();
		}

		Consumer<Long> updateFrameInvoker = time -> {
			if (renderer.currentFrame() >= frames.size()) return;
			try {
				if (renderer instanceof ResourcePackAnimationRenderer r) {
					r.updateFrame(time);
				} else if (renderer instanceof ExternalTextureAnimationRenderer e) {
					Method m = ExternalTextureAnimationRenderer.class.getDeclaredMethod("updateFrame", long.class);
					m.setAccessible(true);
					m.invoke(e, time);
				}
			} catch (Exception ignore) {}
		};

		Supplier<Long> prevTimeSupplier = () -> {
			try {
				if (renderer instanceof ResourcePackAnimationRenderer r) {
					return r.prevTime;
				} else if (renderer instanceof ExternalTextureAnimationRenderer e) {
					Field f = ExternalTextureAnimationRenderer.class.getDeclaredField("prevTime");
					f.setAccessible(true);
					return (Long) f.get(e);
				}
			} catch (Exception ignore) {}
			return 0L;
		};

		//Updating the current frame based on the fps value
		long time = System.currentTimeMillis();
		if (fps == -1) {
			updateFrameInvoker.accept(time);
		} else {
			if ((prevTimeSupplier.get() + (1000 / fps)) <= time) {
				updateFrameInvoker.accept(time);
			}
		}

	}

	@NotNull
	protected static List<ResourceLocation> getFramesOf(@NotNull IAnimationRenderer renderer) {
		try {
			if (renderer.isReady()) {
				if (renderer instanceof ResourcePackAnimationRenderer r) {
					return r.resources;
				} else if (renderer instanceof ExternalTextureAnimationRenderer e) {
					Field f = ExternalTextureAnimationRenderer.class.getDeclaredField("resources");
					f.setAccessible(true);
					List<ExternalTextureResourceLocation> resources = (List<ExternalTextureResourceLocation>) f.get(renderer);
					List<ResourceLocation> locations = new ArrayList<>();
					for (ExternalTextureResourceLocation external : resources) {
						ResourceLocation loc = external.getResourceLocation();
						if (loc == null) loc = FULLY_TRANSPARENT_TEXTURE;
						locations.add(loc);
					}
					return locations;
				}
			}
		} catch (Exception ignore) {}
		return new ArrayList<>();
	}

	@Nullable
	protected static ResourceLocation getCurrentFrameOf(@NotNull IAnimationRenderer renderer) {
		List<ResourceLocation> locations = getFramesOf(renderer);
		if (!locations.isEmpty()) {
			if (renderer.currentFrame() < locations.size()) {
				return locations.get(renderer.currentFrame());
			}
			return locations.get(locations.size()-1);
		}
		return null;
	}

	protected static boolean canRenderFrameOf(@NotNull IAnimationRenderer renderer, int frame) {
		try {
			if (renderer.isReady()) {
				if (renderer instanceof ResourcePackAnimationRenderer) {
					List<ResourceLocation> l = ((ResourcePackAnimationRenderer) renderer).resources;
					if (!l.isEmpty()) {
						if (l.size() > frame) {
							ResourceLocation r = l.get(frame);
							Resource res = Minecraft.getInstance().getResourceManager().getResource(r).get();
							return (res != null);
						} else {
							return true;
						}
					}
				} else if (renderer instanceof ExternalTextureAnimationRenderer) {
					Field f = ExternalTextureAnimationRenderer.class.getDeclaredField("resources");
					f.setAccessible(true);
					List<ExternalTextureResourceLocation> l = (List<ExternalTextureResourceLocation>) f.get(renderer);
					if ((l != null) && (l.size() > frame)) {
						ResourceLocation r = l.get(frame).getResourceLocation();
						if (r != null) {
							Resource res = Minecraft.getInstance().getResourceManager().getResource(r).get();
							return (res != null);
						}
					} else {
						return true;
					}
				} else {
					return true;
				}
			}
		} catch (Exception e) {}
		return false;
	}

	//------------------- RenderableResource + PlayableResource stuff

	@Override
	public void play() {
		this.playing = true;
		this.started = true;
	}

	@Override
	public void pause() {
		this.playing = false;
		this.stopAudio();
	}

	@Override
	public void stop() {
		this.playing = false;
		this.started = false;
		this.resetAnimation();
		this.stopAudio();
	}

	@Override
	public boolean isPlaying() {
		return this.playing;
	}

	@Override
	public @Nullable ResourceLocation getResourceLocation() {

		if (!this.playing) return null;

		//Audio handling
		if (!this.muted) {
			if (this.hasIntroAudio() && !this.introRenderer.isFinished() && ((this.introRenderer.currentFrame() == 1) || (this.introRenderer.currentFrame() > 1) && !SoundHandler.isPlaying(introAudioPath))) {
				SoundHandler.stopSound(mainAudioPath);
				SoundHandler.registerSound(introAudioPath, introAudioPath);
				SoundHandler.resetSound(introAudioPath);
				SoundHandler.playSound(introAudioPath);
			}
			if (this.hasIntroAudio() && this.introRenderer.isFinished()) {
				SoundHandler.stopSound(introAudioPath);
			}
			if (this.hasMainAudio() && !this.animationRenderer.isFinished() && ((this.animationRenderer.currentFrame() == 1) || (this.animationRenderer.currentFrame() > 1) && !SoundHandler.isPlaying(mainAudioPath))) {
				if (this.hasIntroAudio()) {
					SoundHandler.stopSound(introAudioPath);
				}
				SoundHandler.registerSound(mainAudioPath, mainAudioPath);
				SoundHandler.resetSound(mainAudioPath);
				SoundHandler.playSound(mainAudioPath);
				SoundHandler.setLooped(mainAudioPath, true);
			}
		}

		//Get and return current frame + tick renderers
		if (this.hasIntro()) {
			this.introRenderer.setFPS(this.animationRenderer.getFPS());
			this.introRenderer.setLooped(false);
			if (!this.introRenderer.isFinished()) {
				ResourceLocation current = getCurrentFrameOf(this.introRenderer);
				tickRenderer(this.introRenderer);
				return current;
			}
		}
		ResourceLocation current = getCurrentFrameOf(this.animationRenderer);
		tickRenderer(this.animationRenderer);
		return current;

	}

	@Override
	public void reset() {
		this.playing = false;
		this.resetAnimation();
	}

	@Override
	public void close() throws IOException {
	}

}
