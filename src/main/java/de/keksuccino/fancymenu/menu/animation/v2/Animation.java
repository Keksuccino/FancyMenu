//package de.keksuccino.fancymenu.menu.animation.v2;
//
//import com.mojang.blaze3d.systems.RenderSystem;
//import com.mojang.blaze3d.vertex.PoseStack;
//import de.keksuccino.auudio.audio.AudioClip;
//import de.keksuccino.fancymenu.menu.animation.v2.exception.AnimationLoadingFailedException;
//import de.keksuccino.fancymenu.menu.animation.v2.resource.packresources.AnimationPackResources;
//import de.keksuccino.konkrete.math.MathUtils;
//import de.keksuccino.konkrete.properties.PropertiesSection;
//import de.keksuccino.konkrete.rendering.RenderUtils;
//import net.minecraft.client.gui.GuiComponent;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.server.packs.PackType;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.function.Predicate;
//
//public class Animation extends GuiComponent {
//
//    private static final Logger LOGGER = LogManager.getLogger();
//
//    public volatile File packFile;
//    public volatile AnimationPackResources packResources;
//    public volatile String name;
//    public volatile boolean loop = false;
//    public volatile int fps = 24;
//    public volatile List<ResourceLocation> mainFrames = new ArrayList<>();
//    public volatile List<ResourceLocation> introFrames = new ArrayList<>();
//    public volatile AudioClip generalAudio;
//    public volatile AudioClip mainAnimationAudio;
//    public volatile AudioClip introAnimationAudio;
//
//    protected volatile float opacity = 1.0F;
//
//    protected volatile boolean running = true;
//    protected volatile boolean playing = false;
//    protected volatile boolean paused = false;
//
//    protected volatile int currentFrameIndex = 1;
//    protected volatile ResourceLocation currentFrame = null;
//
//    protected volatile long lastFrameTime = -1L;
//
//    //TODO extra thread für play
//    //TODO volume handling wie bei video extension
//    // - Button action, um volume up/down für ALLE
//    // - Button action um volume up/down für EINZELNE animation
//
//    public Animation(AnimationPackResources packResources) {
//        this.packResources = packResources;
//        if (this.packResources != null) {
//            this.packFile = this.packResources.animationPackFileOrFolder;
//        }
//        new Thread(this::playLoop).start();
//    }
//
//    public Animation(AnimationPackResources packResources, boolean initializeAnimation) throws AnimationLoadingFailedException {
//        this(packResources);
//        if (initializeAnimation) {
//            this.init();
//        }
//    }
//
//    public void init() throws AnimationLoadingFailedException {
//
//        if (this.packResources != null) {
//            PropertiesSection meta = this.packResources.getAnimationPackMeta();
//            if (meta != null) {
//
//                //Reading meta from animation file/folder
//                this.name = meta.getEntryValue("name");
//                String fpsString = meta.getEntryValue("fps");
//                if ((fpsString != null) && MathUtils.isInteger(fpsString)) {
//                    this.fps = Integer.parseInt(fpsString);
//                }
//                String loopString = meta.getEntryValue("loop");
//                if ((loopString != null) && loopString.equalsIgnoreCase("true")) {
//                    this.loop = true;
//                }
//
//            } else {
//                String s = "null";
//                if (this.packFile != null) {
//                    s = this.packFile.getPath();
//                }
//                throw new AnimationLoadingFailedException("Unable to initialize animation '" + s + "'! Pack meta returned NULL!");
//            }
//        } else {
//            String s = "null";
//            if (this.packFile != null) {
//                s = this.packFile.getPath();
//            }
//            throw new AnimationLoadingFailedException("Unable to initialize animation '" + s + "'! Pack Resources were NULL!");
//        }
//
//    }
//
//    public void prepare() {
//
//        if (this.packResources != null) {
//
//            Predicate<ResourceLocation> isImagePredicate = resLoc -> {
//                String name = resLoc.getPath().toLowerCase();
//                if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")) {
//                    return true;
//                }
//                return false;
//            };
//
//            //Main frames
//            this.mainFrames.clear();
//            for (ResourceLocation r : this.packResources.getResources(PackType.CLIENT_RESOURCES, "fancymenu", "animation/" + this.name + "/main_frames", isImagePredicate)) {
//                this.mainFrames.add(r);
//            }
//
//            //Intro frames
//            this.introFrames.clear();
//            for (ResourceLocation r : this.packResources.getResources(PackType.CLIENT_RESOURCES, "fancymenu", "animation/" + this.name + "/intro_frames", isImagePredicate)) {
//                this.introFrames.add(r);
//            }
//
//            //General Audio
//            if (this.generalAudio != null) {
//                this.generalAudio.stop();
//            }
//            ResourceLocation generalAudioLoc = new ResourceLocation("fancymenu", "animation/" + this.name + "/general_audio.ogg");
//            this.generalAudio = this.prepareAudio(generalAudioLoc);
//
//            //Main Audio
//            if (this.mainAnimationAudio != null) {
//                this.mainAnimationAudio.stop();
//            }
//            ResourceLocation mainAudioLoc = new ResourceLocation("fancymenu", "animation/" + this.name + "/main_audio.ogg");
//            this.mainAnimationAudio = this.prepareAudio(mainAudioLoc);
//
//            //Intro Audio
//            if (this.introAnimationAudio != null) {
//                this.introAnimationAudio.stop();
//            }
//            ResourceLocation introAudioLoc = new ResourceLocation("fancymenu", "animation/" + this.name + "/intro_audio.ogg");
//            this.introAnimationAudio = this.prepareAudio(introAudioLoc);
//
//        }
//
//    }
//
//    protected AudioClip prepareAudio(ResourceLocation audioLoc) {
//        try {
//            if (!this.packResources.hasResource(PackType.CLIENT_RESOURCES, audioLoc)) {
//                return null;
//            }
//            String audioIdentifier = this.name + ":" + audioLoc.getNamespace() + ":" + audioLoc.getPath();
//            if (AnimationAudioRegistry.hasAudio(audioIdentifier)) {
//                return AnimationAudioRegistry.getAudio(audioIdentifier);
//            } else {
//                AudioClip c = AudioClip.buildInternalClip(audioLoc);
//                AnimationAudioRegistry.registerAudio(audioIdentifier, c);
//                return c;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    protected void playLoop() {
//
//        while (this.running) {
//            try {
//                if (this.playing) {
//
//                    long now = System.currentTimeMillis();
//                    long msPerFrame = 1000L / (long)this.fps;
//                    if ((this.lastFrameTime + msPerFrame) <= now) {
//                        this.currentFrameIndex++;
//                        //TODO hier weiter machen
//                    }
//
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            try {
//                Thread.sleep(10);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//    }
//
//    public void render(PoseStack matrix, int x, int y, int width, int height) {
//
//        if (this.currentFrame != null) {
//
//            RenderUtils.bindTexture(this.currentFrame);
//            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.opacity);
//            blit(matrix, x, y, 0.0F, 0.0F, width, height, width, height);
//            RenderSystem.disableBlend();
//
//        }
//
//    }
//
//    public void play() {
//        if (this.isReady()) {
//            this.playing = true;
//            this.paused = false;
//        } else {
//            LOGGER.error("Unable to play unprepared or empty animation: " + this.name);
//        }
//    }
//
//    public void pause() {
//        if (this.playing) {
//            this.playing = false;
//            this.paused = true;
//            if (this.mainAnimationAudio != null) {
//                this.mainAnimationAudio.pause();
//            }
//            if (this.introAnimationAudio != null) {
//                this.introAnimationAudio.pause();
//            }
//            if (this.generalAudio != null) {
//                this.generalAudio.pause();
//            }
//        }
//    }
//
//    public void stop() {
//        this.playing = false;
//        this.paused = false;
//        if (this.mainAnimationAudio != null) {
//            this.mainAnimationAudio.stop();
//        }
//        if (this.introAnimationAudio != null) {
//            this.introAnimationAudio.stop();
//        }
//        if (this.generalAudio != null) {
//            this.generalAudio.stop();
//        }
//        this.lastFrameTime = -1;
//        this.currentFrame = null;
//        this.currentFrameIndex = 1;
//    }
//
//    public boolean isPlaying() {
//        return this.playing && !this.paused;
//    }
//
//    public boolean isPaused() {
//        return this.paused;
//    }
//
//    public boolean isReady() {
//        return !mainFrames.isEmpty();
//    }
//
//    /**
//     * Sets a new {@link AnimationPackResources} instance.<br><br>
//     *
//     * <b>It is needed to call {@link Animation#prepare()} again after doing this!</b>
//     */
//    public void setPackResources(AnimationPackResources packResources) {
//        this.stop();
//        this.mainFrames.clear();
//        this.introFrames.clear();
//        this.mainAnimationAudio = null;
//        this.introAnimationAudio = null;
//        this.generalAudio = null;
//        this.packResources = packResources;
//    }
//
//    public int getCurrentFrameIndex() {
//        return this.currentFrameIndex;
//    }
//
//    public int getTotalFrames() {
//        return this.mainFrames.size() + this.introFrames.size();
//    }
//
//    public ResourceLocation getCurrentFrame() {
//        return this.currentFrame;
//    }
//
//    public float getOpacity() {
//        return this.opacity;
//    }
//
//    public void setOpacity(float opacity) {
//        if (opacity < 0.0F) {
//            opacity = 0.0F;
//        }
//        if (opacity > 1.0F) {
//            opacity = 1.0F;
//        }
//        this.opacity = opacity;
//    }
//
//    public void destroy() {
//        this.stop();
//        this.running = false;
//    }
//
//}
