package de.keksuccino.fancymenu.customization.customgui;

import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class CustomGuiBaseScreenTest {

    private final List<DrawStep> steps = new ArrayList<>();
    private final BackgroundListener listener = new BackgroundListener(this.steps);
    private MockedStatic<Minecraft> minecraftAccess;
    private MockedStatic<RenderingUtils> renderingAccess;
    private Minecraft minecraft;
    private GuiGraphics graphics;

    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void replaceGraphicsSideEffects() {
        this.minecraft = mock(Minecraft.class);
        this.minecraftAccess = mockStatic(Minecraft.class);
        this.minecraftAccess.when(Minecraft::getInstance).thenReturn(this.minecraft);
        // The 1.19.2 graphics adapter initializes a dummy TitleScreen. Keep that initialization away from client services.
        try (MockedConstruction<TitleScreen> ignored = mockConstruction(TitleScreen.class)) {
            this.graphics = mock(GuiGraphics.class);
        }
        this.renderingAccess = mockStatic(RenderingUtils.class);
        this.renderingAccess.when(() -> RenderingUtils.setOverrideBackgroundBlurRadius(anyInt())).thenCallRealMethod();
        this.renderingAccess.when(RenderingUtils::resetOverrideBackgroundBlurRadius).thenCallRealMethod();
        this.renderingAccess.when(RenderingUtils::shouldOverrideBackgroundBlurRadius).thenCallRealMethod();
        this.renderingAccess.when(RenderingUtils::getOverrideBackgroundBlurRadius).thenCallRealMethod();
        // Only OpenGL setup and the final graphics fill are replaced; the production overlay helper still runs.
        doAnswer(invocation -> {
            assertEquals(0, (int) invocation.getArgument(0));
            assertEquals(0, (int) invocation.getArgument(1));
            assertEquals(320, (int) invocation.getArgument(2));
            assertEquals(180, (int) invocation.getArgument(3));
            int alpha = (int) invocation.getArgument(4) >>> 24;
            assertTrue(alpha > 0 && alpha < 255);
            this.steps.add(DrawStep.TINT);
            return null;
        }).when(this.graphics).fill(anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        EventHandler.INSTANCE.registerListenersOf(this.listener);
    }

    @AfterEach
    void restoreGlobalState() {
        EventHandler.INSTANCE.unregisterListenersOf(this.listener);
        if (this.renderingAccess != null) {
            RenderingUtils.resetOverrideBackgroundBlurRadius();
            this.renderingAccess.close();
        }
        if (this.minecraftAccess != null) this.minecraftAccess.close();
    }

    @ParameterizedTest
    @MethodSource("backgroundCases")
    void rendersOnlyTheRequestedBackgroundLayers(boolean inWorld, boolean worldBackground, boolean worldOverlay, boolean popup, boolean hasParent, boolean popupOverlay, List<DrawStep> expected) {
        CustomGui gui = new CustomGui();
        gui.worldBackground = worldBackground;
        gui.worldBackgroundOverlay = worldOverlay;
        gui.popupMode = popup;
        gui.popupModeBackgroundOverlay = popupOverlay;
        RecordingScreen screen = this.createScreen(gui, inWorld, hasParent);

        screen.renderBackground(this.graphics, 23, 41, 0.5F);

        assertEquals(expected, this.steps);
        this.assertBackgroundEvent(screen);
        assertFalse(RenderingUtils.shouldOverrideBackgroundBlurRadius());
        if (expected.contains(DrawStep.BLUR)) assertEquals(popup && hasParent, screen.blurOverrideActive);
    }

    private static Stream<Arguments> backgroundCases() {
        return Stream.of(
                arguments(true, true, true, false, false, true, List.of(DrawStep.BLUR, DrawStep.TINT, DrawStep.EVENT)),
                arguments(true, true, false, false, false, true, List.of(DrawStep.EVENT)),
                arguments(true, false, true, false, false, true, List.of(DrawStep.DIRT, DrawStep.BLUR, DrawStep.TINT, DrawStep.EVENT)),
                arguments(true, false, false, false, false, true, List.of(DrawStep.DIRT, DrawStep.BLUR, DrawStep.TINT, DrawStep.EVENT)),
                arguments(false, true, true, false, false, true, List.of(DrawStep.DIRT, DrawStep.BLUR, DrawStep.TINT, DrawStep.EVENT)),
                arguments(false, true, false, false, false, true, List.of(DrawStep.DIRT, DrawStep.EVENT)),
                arguments(false, false, true, false, false, true, List.of(DrawStep.DIRT, DrawStep.BLUR, DrawStep.TINT, DrawStep.EVENT)),
                arguments(false, false, false, false, false, true, List.of(DrawStep.DIRT, DrawStep.BLUR, DrawStep.TINT, DrawStep.EVENT)),
                arguments(true, false, true, true, true, true, List.of(DrawStep.PARENT, DrawStep.BLUR, DrawStep.EVENT)),
                arguments(false, true, true, true, true, false, List.of(DrawStep.PARENT, DrawStep.EVENT)),
                arguments(true, true, true, true, false, true, List.of(DrawStep.BLUR, DrawStep.TINT, DrawStep.EVENT)),
                arguments(false, true, true, true, false, false, List.of(DrawStep.DIRT, DrawStep.BLUR, DrawStep.TINT, DrawStep.EVENT))
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void blurFailureStillDrawsTheOverlayAndPublishesTheEvent(boolean popup) {
        CustomGui gui = new CustomGui();
        gui.popupMode = popup;
        RecordingScreen screen = this.createScreen(gui, true, popup);
        screen.failBlur = true;

        screen.renderBackground(this.graphics, 23, 41, 0.5F);

        assertEquals(popup ? List.of(DrawStep.PARENT, DrawStep.BLUR, DrawStep.EVENT) : List.of(DrawStep.BLUR, DrawStep.TINT, DrawStep.EVENT), this.steps);
        this.assertBackgroundEvent(screen);
        assertFalse(RenderingUtils.shouldOverrideBackgroundBlurRadius());
    }

    @Test
    void changingSettingsBetweenFramesDoesNotRetainOpaqueBackgrounds() {
        CustomGui gui = new CustomGui();
        gui.worldBackground = false;
        RecordingScreen screen = this.createScreen(gui, true, false);
        screen.renderBackground(this.graphics, 23, 41, 0.5F);
        assertEquals(List.of(DrawStep.DIRT, DrawStep.BLUR, DrawStep.TINT, DrawStep.EVENT), this.steps);

        this.steps.clear();
        this.listener.events.clear();
        gui.worldBackground = true;
        gui.worldBackgroundOverlay = false;
        screen.renderBackground(this.graphics, 23, 41, 0.5F);

        assertEquals(List.of(DrawStep.EVENT), this.steps);
        this.assertBackgroundEvent(screen);
    }

    private RecordingScreen createScreen(CustomGui gui, boolean inWorld, boolean hasParent) {
        this.minecraft.level = inWorld ? mock(ClientLevel.class) : null;
        RecordingScreen screen = new RecordingScreen(gui, hasParent ? mock(Screen.class) : null, this.steps);
        screen.width = 320;
        screen.height = 180;
        return screen;
    }

    private void assertBackgroundEvent(RecordingScreen screen) {
        assertEquals(1, this.listener.events.size());
        RenderedScreenBackgroundEvent event = this.listener.events.get(0);
        assertSame(screen, event.getScreen());
        assertSame(this.graphics, event.getGraphics());
        assertEquals(23, event.getMouseX());
        assertEquals(41, event.getMouseY());
        assertEquals(0.5F, event.getPartial());
    }

    private enum DrawStep {

        DIRT, PARENT, BLUR, TINT, EVENT

    }

    public static final class BackgroundListener {

        private final List<DrawStep> steps;
        private final List<RenderedScreenBackgroundEvent> events = new ArrayList<>();

        private BackgroundListener(List<DrawStep> steps) {
            this.steps = steps;
        }

        @EventListener
        public void onBackground(RenderedScreenBackgroundEvent event) {
            this.events.add(event);
            this.steps.add(DrawStep.EVENT);
        }

    }

    private static final class RecordingScreen extends CustomGuiBaseScreen {

        private final List<DrawStep> steps;
        private boolean failBlur;
        private boolean blurOverrideActive;

        private RecordingScreen(CustomGui gui, Screen parent, List<DrawStep> steps) {
            super(gui, parent, null);
            this.steps = steps;
        }

        @Override
        public void renderDirtBackground(GuiGraphics graphics) {
            this.steps.add(DrawStep.DIRT);
        }

        @Override
        protected void renderPopupMenuBackgroundScreen(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.steps.add(DrawStep.PARENT);
        }

        @Override
        protected void renderGuiBlurBackground(GuiGraphics graphics, float partial) {
            this.steps.add(DrawStep.BLUR);
            this.blurOverrideActive = RenderingUtils.shouldOverrideBackgroundBlurRadius();
            if (this.blurOverrideActive) assertEquals(7, RenderingUtils.getOverrideBackgroundBlurRadius());
            if (this.failBlur) throw new IllegalStateException();
        }

    }

}
