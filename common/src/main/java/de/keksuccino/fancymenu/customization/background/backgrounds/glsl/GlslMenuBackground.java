package de.keksuccino.fancymenu.customization.background.backgrounds.glsl;

import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.glsl.GlslShaderRuntime;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class GlslMenuBackground extends MenuBackground<GlslMenuBackground> {

    private static final FileFilter SHADER_FILE_FILTER = file -> {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".txt");
    };

    private static final List<GlslShaderRuntime.CompileMode> COMPILE_MODES = List.of(
            GlslShaderRuntime.CompileMode.AUTO,
            GlslShaderRuntime.CompileMode.DIRECT,
            GlslShaderRuntime.CompileMode.SHADERTOY
    );

    public final Property<ResourceSupplier<IText>> shaderSource = putProperty(Property.resourceSupplierProperty(IText.class, "shader_source", null, "fancymenu.backgrounds.glsl.shader_source", true, true, true, SHADER_FILE_FILTER));
    public final Property.StringProperty inlineShaderSource = putProperty(Property.stringProperty("inline_shader_source", "", true, false, "fancymenu.backgrounds.glsl.inline_shader_source"));
    public final Property.BooleanProperty preferInlineShaderSource = putProperty(Property.booleanProperty("prefer_inline_shader_source", false, "fancymenu.backgrounds.glsl.prefer_inline_shader_source"));
    public final Property.StringProperty compileMode = putProperty(Property.stringProperty("compile_mode", "auto", false, false, "fancymenu.backgrounds.glsl.compile_mode"));
    public final Property.BooleanProperty forceShadertoyCompatibility = putProperty(Property.booleanProperty("force_shadertoy_compatibility", true, "fancymenu.backgrounds.glsl.force_shadertoy_compatibility"));
    public final Property.BooleanProperty freezeTime = putProperty(Property.booleanProperty("freeze_time", false, "fancymenu.backgrounds.glsl.freeze_time"));
    public final Property.FloatProperty timeScale = putProperty(Property.floatProperty("time_scale", 1.0F, "fancymenu.backgrounds.glsl.time_scale"));
    public final Property.BooleanProperty enableBlending = putProperty(Property.booleanProperty("enable_blending", true, "fancymenu.backgrounds.glsl.enable_blending"));
    public final Property.BooleanProperty useInput = putProperty(Property.booleanProperty("use_input", true, "fancymenu.backgrounds.glsl.use_input"));
    public final Property.FloatProperty opacityMultiplier = putProperty(Property.floatProperty("opacity_multiplier", 1.0F, "fancymenu.backgrounds.glsl.opacity_multiplier"));
    public final Property.BooleanProperty showCompileErrors = putProperty(Property.booleanProperty("show_compile_errors", true, "fancymenu.backgrounds.glsl.show_compile_errors"));

    private final GlslShaderRuntime shaderRuntime = new GlslShaderRuntime();

    public GlslMenuBackground(@NotNull MenuBackgroundBuilder<GlslMenuBackground> builder) {
        super(builder);
    }

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.shaderSource.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((m, entry) -> tooltip("fancymenu.backgrounds.glsl.shader_source.desc"))
                .setIcon(MaterialIcons.TEXT_FIELDS);

        this.inlineShaderSource.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((m, entry) -> tooltip("fancymenu.backgrounds.glsl.inline_shader_source.desc"))
                .setIcon(MaterialIcons.CODE);

        this.preferInlineShaderSource.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((m, entry) -> tooltip("fancymenu.backgrounds.glsl.prefer_inline_shader_source.desc"))
                .setIcon(MaterialIcons.SWAP_HORIZ);

        menu.addSeparatorEntry("separator_after_source_selection");

        this.addCycleContextMenuEntryTo(menu,
                        "compile_mode",
                        COMPILE_MODES,
                        GlslMenuBackground.class,
                        GlslMenuBackground::getCompileMode,
                        GlslMenuBackground::setCompileMode,
                        (contextMenu, entry, mode) -> Component.translatable("fancymenu.backgrounds.glsl.compile_mode", getCompileModeDisplay(mode)))
                .setTooltipSupplier((m, entry) -> tooltip("fancymenu.backgrounds.glsl.compile_mode.desc"))
                .setIcon(MaterialIcons.TUNE);

        this.forceShadertoyCompatibility.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((m, entry) -> tooltip("fancymenu.backgrounds.glsl.force_shadertoy_compatibility.desc"))
                .setIcon(MaterialIcons.CODE);

        this.freezeTime.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((m, entry) -> tooltip("fancymenu.backgrounds.glsl.freeze_time.desc"))
                .setIcon(MaterialIcons.PAUSE);

        this.timeScale.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((m, entry) -> tooltip("fancymenu.backgrounds.glsl.time_scale.desc"))
                .setIcon(MaterialIcons.SPEED);

        menu.addSeparatorEntry("separator_before_render_settings");

        this.enableBlending.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((m, entry) -> tooltip("fancymenu.backgrounds.glsl.enable_blending.desc"))
                .setIcon(MaterialIcons.BLUR_ON);

        this.useInput.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((m, entry) -> tooltip("fancymenu.backgrounds.glsl.use_input.desc"))
                .setIcon(MaterialIcons.MOUSE);

        this.opacityMultiplier.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((m, entry) -> tooltip("fancymenu.backgrounds.glsl.opacity_multiplier.desc"))
                .setIcon(MaterialIcons.PALETTE);

        this.showCompileErrors.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((m, entry) -> tooltip("fancymenu.backgrounds.glsl.show_compile_errors.desc"))
                .setIcon(MaterialIcons.ERROR);

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        float resolvedOpacity = Mth.clamp(this.opacity * Math.max(0.0F, this.opacityMultiplier.getFloat()), 0.0F, 1.0F);

        boolean rendered = this.shaderRuntime.render(
                graphics,
                0,
                0,
                getScreenWidth(),
                getScreenHeight(),
                partial,
                resolveShaderSource(),
                new GlslShaderRuntime.RenderSettings(
                        this.getCompileMode(),
                        this.forceShadertoyCompatibility.getBoolean(),
                        Math.max(0.0F, this.timeScale.getFloat()),
                        this.freezeTime.getBoolean(),
                        this.enableBlending.getBoolean(),
                        this.useInput.getBoolean(),
                        resolvedOpacity
                )
        );

        if (!this.showCompileErrors.getBoolean()) {
            return;
        }

        if (!rendered && this.shaderRuntime.isSourceMissing()) {
            this.renderErrorOverlay(graphics, Component.translatable("fancymenu.backgrounds.glsl.error.no_source").getString());
            return;
        }

        String compileError = this.shaderRuntime.getLastCompileError();
        if (compileError != null && !compileError.isBlank()) {
            this.renderErrorOverlay(graphics, compileError);
        }

    }

    @Override
    public void onBeforeResizeScreen() {
        super.onBeforeResizeScreen();
        this.shaderRuntime.close();
    }

    @Override
    public void onCloseScreen(@Nullable net.minecraft.client.gui.screens.Screen closedScreen, @Nullable net.minecraft.client.gui.screens.Screen newScreen) {
        super.onCloseScreen(closedScreen, newScreen);
        this.shaderRuntime.close();
    }

    @Override
    public void onDisableOrRemove() {
        super.onDisableOrRemove();
        this.shaderRuntime.close();
    }

    @Nullable
    private String resolveShaderSource() {
        String inlineSource = this.inlineShaderSource.get();
        String fileSource = readSourceFromSupplier(this.shaderSource.get());

        if (this.preferInlineShaderSource.getBoolean()) {
            if (inlineSource != null && !inlineSource.isBlank()) {
                return inlineSource;
            }
            return fileSource;
        }

        if (fileSource != null && !fileSource.isBlank()) {
            return fileSource;
        }

        if (inlineSource != null && !inlineSource.isBlank()) {
            return inlineSource;
        }

        return null;
    }

    @Nullable
    private static String readSourceFromSupplier(@Nullable ResourceSupplier<IText> supplier) {
        if (supplier == null) {
            return null;
        }
        IText text = supplier.get();
        if (text == null || !text.isReady()) {
            return null;
        }
        List<String> lines = text.getTextLines();
        if (lines == null) {
            return null;
        }
        return String.join("\n", lines);
    }

    @NotNull
    private static UITooltip tooltip(@NotNull String key) {
        return UITooltip.of(LocalizationUtils.splitLocalizedLines(key));
    }

    private void renderErrorOverlay(@NotNull GuiGraphics graphics, @NotNull String message) {
        var font = Minecraft.getInstance().font;

        String[] split = message.replace("\r", "").split("\n");
        int maxLines = Math.min(split.length, 8);
        int maxWidth = Math.max(20, getScreenWidth() - 16);

        int y = 8;
        int boxWidth = 0;
        String[] lines = new String[maxLines];
        for (int i = 0; i < maxLines; i++) {
            String line = split[i];
            if (font.width(line) > maxWidth) {
                line = font.plainSubstrByWidth(line, maxWidth - font.width("..")) + "..";
            }
            lines[i] = line;
            boxWidth = Math.max(boxWidth, font.width(line));
        }

        int lineHeight = font.lineHeight + 1;
        int boxHeight = (maxLines * lineHeight) + 8;

        graphics.fill(4, 4, 12 + boxWidth, 8 + boxHeight, 0xCC000000);

        for (String line : lines) {
            graphics.drawString(font, line, 8, y, 0xFFFF6666, false);
            y += lineHeight;
        }
    }

    @NotNull
    private static Component getCompileModeDisplay(@NotNull GlslShaderRuntime.CompileMode mode) {
        return switch (mode) {
            case AUTO -> Component.translatable("fancymenu.backgrounds.glsl.compile_mode.auto");
            case DIRECT -> Component.translatable("fancymenu.backgrounds.glsl.compile_mode.direct");
            case SHADERTOY -> Component.translatable("fancymenu.backgrounds.glsl.compile_mode.shadertoy");
        };
    }

    @NotNull
    private GlslShaderRuntime.CompileMode getCompileMode() {
        String raw = this.compileMode.get();
        if (raw != null) {
            try {
                return GlslShaderRuntime.CompileMode.valueOf(raw.toUpperCase(Locale.ROOT));
            } catch (Exception ignored) {
            }
        }
        return GlslShaderRuntime.CompileMode.AUTO;
    }

    private void setCompileMode(@NotNull GlslShaderRuntime.CompileMode mode) {
        this.compileMode.set(mode.name().toLowerCase(Locale.ROOT));
    }

}
