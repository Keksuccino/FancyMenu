package de.keksuccino.fancymenu.util.rendering.gui;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.WidgetWithVanillaTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarrationSupplier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VanillaTooltip implements NarrationSupplier {

    private static final int MAX_WIDTH = 170;

    private final Component message;
    @Nullable
    private List<FormattedCharSequence> cachedTooltip;
    @Nullable
    private Language splitWithLanguage;
    @Nullable
    private final Component narration;
    @Nullable
    private List<MutableComponent> cachedTooltipComponents;

    private VanillaTooltip(Component message, @Nullable Component narration) {
        this.message = message;
        this.narration = narration;
    }

    public static VanillaTooltip create(Component message, @Nullable Component narration) {
        return new VanillaTooltip(message, narration);
    }

    public static VanillaTooltip create(Component message) {
        return new VanillaTooltip(message, message);
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        if (this.narration != null) {
            narrationElementOutput.add(NarratedElementType.HINT, this.narration);
        }
    }

    public List<MutableComponent> toComponentList(Minecraft minecraft) {
        Language language = Language.getInstance();
        if (this.cachedTooltipComponents == null || language != this.splitWithLanguage) {
            this.cachedTooltipComponents = TextFormattingUtils.lineWrapComponents(this.message, MAX_WIDTH);
            this.splitWithLanguage = language;
        }
        return this.cachedTooltipComponents;
    }

    public List<FormattedCharSequence> toCharSequence(Minecraft minecraft) {
        Language language = Language.getInstance();
        if (this.cachedTooltip == null || language != this.splitWithLanguage) {
            this.cachedTooltip = splitTooltip(minecraft, this.message);
            this.splitWithLanguage = language;
        }
        return this.cachedTooltip;
    }

    public static List<FormattedCharSequence> splitTooltip(Minecraft minecraft, Component message) {
        return minecraft.font.split(message, MAX_WIDTH);
    }

    public static void renderScreenTooltips(@NotNull Screen screen, @NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        for (GuiEventListener listener : screen.children()) {
            if ((listener instanceof WidgetWithVanillaTooltip l) && (listener instanceof AbstractWidget w)) {
                if (!w.visible || !((IMixinAbstractWidget)w).getIsHoveredFancyMenu()) continue;
                VanillaTooltip t = l.getVanillaTooltip_FancyMenu();
                if (t != null) {
                    screen.renderTooltip(pose, t.toCharSequence(Minecraft.getInstance()), mouseX, mouseY);
                    return;
                }
            }
        }
    }

}
