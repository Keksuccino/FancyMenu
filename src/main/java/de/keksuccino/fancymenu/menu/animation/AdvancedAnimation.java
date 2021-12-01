package de.keksuccino.fancymenu.menu.animation;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import de.keksuccino.konkrete.rendering.animation.ExternalTextureAnimationRenderer;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import de.keksuccino.fancymenu.menu.animation.exceptions.AnimationNotFoundException;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.util.math.MatrixStack;

public class AdvancedAnimation implements IAnimationRenderer {
	
	private IAnimationRenderer introRenderer;
	private IAnimationRenderer animationRenderer;
	private boolean started = false;
	private String mainAudioPath;
	private String introAudioPath;
	private boolean muted = false;
	private boolean replayIntro = false;

	public String propertiesPath = null;

	protected boolean prepared = false;
	
	/**
	 * Container to hold a {@link IAnimationRenderer} instance with an optional intro which plays before the main animation starts.
	 * 
	 * @param introAnimation The intro animation. Can be null.
	 * @param mainAnimation The main animation.
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
	public void prepareAnimation() {
		if (!this.prepared) {
			if (this.mainAudioPath != null) {
				SoundHandler.registerSound(mainAudioPath, mainAudioPath);
			}
			if ((this.introAudioPath != null) && this.hasIntro()) {
				SoundHandler.registerSound(introAudioPath, introAudioPath);
			}
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
	
	@Override
	public void render(MatrixStack matrix) {
		if (this.isReady()) {
			this.started = true;

			if (!this.muted) {
				if (this.hasIntroAudio() && !this.introRenderer.isFinished() && ((this.introRenderer.currentFrame() == 1) || (this.introRenderer.currentFrame() > 1) && !SoundHandler.isPlaying(introAudioPath))) {
					SoundHandler.stopSound(mainAudioPath);
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
						this.introRenderer.render(matrix);
					}
				} else {
					if (canRenderFrameOf(this.animationRenderer, this.animationRenderer.currentFrame())) {
						this.animationRenderer.render(matrix);
					}
				}
			} else {
				if (canRenderFrameOf(this.animationRenderer, this.animationRenderer.currentFrame())) {
					this.animationRenderer.render(matrix);
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
		return ((this.mainAudioPath != null) && SoundHandler.soundExists(mainAudioPath));
	}

	public boolean hasIntroAudio() {
		return (this.hasIntro() && (this.introAudioPath != null) && SoundHandler.soundExists(introAudioPath));
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

	public static boolean canRenderFrameOf(IAnimationRenderer renderer, int frame) {
		try {
			if (renderer.isReady()) {
				if (renderer instanceof ResourcePackAnimationRenderer) {
					List<Identifier> l = ((ResourcePackAnimationRenderer) renderer).resources;
					if (!l.isEmpty()) {
						if (l.size() > frame) {
							Identifier r = l.get(frame);
							Resource res = MinecraftClient.getInstance().getResourceManager().getResource(r);
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
						Identifier r = l.get(frame).getResourceLocation();
						if (r != null) {
							Resource res = MinecraftClient.getInstance().getResourceManager().getResource(r);
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
	
}
