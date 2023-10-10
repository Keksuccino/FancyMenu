package de.keksuccino.fancymenu.customization.layout.editor;

import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfiguratorScreen;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

public class AutoScalingScreen extends ConfiguratorScreen {

    protected LayoutEditorScreen editor;
    protected Consumer<Boolean> callback;
    protected int autoScalingWidth;
    protected int autoScalingHeight;

    protected AutoScalingScreen(@NotNull LayoutEditorScreen editor, @NotNull Consumer<Boolean> callback) {
        super(Component.translatable("fancymenu.editor.auto_scaling.configure"));
        this.editor = editor;
        this.callback = callback;
        this.autoScalingWidth = Minecraft.getInstance().getWindow().getScreenWidth();
        this.autoScalingHeight = Minecraft.getInstance().getWindow().getScreenHeight();
    }

    @Override
    protected void initCells() {

        this.addSpacerCell(20);

        this.addLabelCell(Component.translatable("fancymenu.helper.editor.properties.autoscale.basesize.popup.desc.line1"));
        this.addLabelCell(Component.translatable("fancymenu.helper.editor.properties.autoscale.basesize.popup.desc.line2"));
        this.addLabelCell(Component.translatable("fancymenu.helper.editor.properties.autoscale.basesize.popup.desc.line3"));

        this.addDescriptionEndSeparatorCell();

        this.addLabelCell(Component.translatable("fancymenu.editor.auto_scaling.configure.width"));
        this.addTextInputCell(CharacterFilter.buildIntegerFiler(), false, false)
                .setText("" + this.autoScalingWidth)
                .setEditListener(s -> this.autoScalingWidth = MathUtils.isInteger(s) ? Integer.parseInt(s) : -1000);

        this.addCellGroupEndSpacerCell();

        this.addLabelCell(Component.translatable("fancymenu.editor.auto_scaling.configure.height"));
        this.addTextInputCell(CharacterFilter.buildIntegerFiler(), false, false)
                .setText("" + this.autoScalingHeight)
                .setEditListener(s -> this.autoScalingHeight = MathUtils.isInteger(s) ? Integer.parseInt(s) : -1000);

        this.addCellGroupEndSpacerCell();

        this.addLabelCell(Component.translatable("fancymenu.editor.auto_scaling.configure.current_screen_width", Component.literal("" + Minecraft.getInstance().getWindow().getScreenWidth()).setStyle(Style.EMPTY.withBold(false))).setStyle(Style.EMPTY.withBold(true)));
        this.addLabelCell(Component.translatable("fancymenu.editor.auto_scaling.configure.current_screen_height", Component.literal("" + Minecraft.getInstance().getWindow().getScreenHeight()).setStyle(Style.EMPTY.withBold(false))).setStyle(Style.EMPTY.withBold(true)));

        this.addSpacerCell(20);

    }

    @Override
    public boolean allowDone() {
        return (this.autoScalingWidth != -1000) && (this.autoScalingHeight != -1000);
    }

    @Override
    protected void onCancel() {
        this.callback.accept(false);
    }

    @Override
    protected void onDone() {
        this.editor.history.saveSnapshot();
        this.editor.layout.autoScalingWidth = this.autoScalingWidth;
        this.editor.layout.autoScalingHeight = this.autoScalingHeight;
        this.callback.accept(true);
    }

}
