package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation FM_LOGO_LOCATION = new ResourceLocation("fancymenu", "textures/fancymenu_logo.png");

    @EventListener(priority = -2000)
    public void onInit(InitOrResizeScreenEvent.Post e) {

//        if (!(e.getScreen() instanceof TitleScreen)) return;
//
//        e.addRenderableWidget(Button.builder(Component.literal("open save screen"), (button) -> {
//            LOGGER.info("################## CLICK");
//            Minecraft.getInstance().setScreen(SaveFileScreen.build(FancyMenu.getGameDirectory(), null, "txt", (call) -> {
//                LOGGER.info("################ CLICK CALLBACK");
//                if (call != null) {
//                    try {
//                        if (call.isFile()) call.delete();
//                        call.createNewFile();
//                        LogManager.getLogger().info("FILE SAVED AS: " + call.getPath());
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
//                }
//                Minecraft.getInstance().setScreen(e.getScreen());
//            }));
//        }).size(100, 20).pos(30, 30).build());

    }

    @EventListener(priority = -2000)
    public void onRenderPost(RenderScreenEvent.Post e) {
//        this.drawLine(e.getPoseStack(), 30, 30, e.getScreen().width - 30, e.getScreen().height - 30);
    }

//    private void drawLine(PoseStack pose, int x1, int y1, int x2, int y2) {
//
//        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
//        RenderSystem.enableBlend();
//        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//
//        RenderType type = RenderType.debugLineStrip(1.0D);
//        Matrix4f matrix4f = pose.last().pose();
//        bufferBuilder.begin(type.mode(), type.format());
//
//        bufferBuilder.vertex(matrix4f, x1, y1, 0).color(1.0F, 0.0F, 0.0F, 1.0F).endVertex();
//        bufferBuilder.vertex(matrix4f, x2, y2, 0).color(1.0F, 0.0F, 0.0F, 1.0F).endVertex();
//        bufferBuilder.vertex(matrix4f, x1+1, y1, 0).color(1.0F, 0.0F, 0.0F, 1.0F).endVertex();
//        bufferBuilder.vertex(matrix4f, x2+1, y2, 0).color(1.0F, 0.0F, 0.0F, 1.0F).endVertex();
//
//        BufferUploader.drawWithShader(bufferBuilder.end());
//        RenderSystem.disableBlend();
//        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//
//    }

}
