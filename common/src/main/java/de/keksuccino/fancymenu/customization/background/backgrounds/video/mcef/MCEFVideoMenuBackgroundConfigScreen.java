package de.keksuccino.fancymenu.customization.background.backgrounds.video.mcef;

import de.keksuccino.fancymenu.customization.element.elements.video.SetVideoVolumeScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.SerializationUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.LocalizedGenericValueCycle;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;
import java.util.function.Consumer;

public class MCEFVideoMenuBackgroundConfigScreen extends CellScreen {

    @NotNull
    protected Consumer<MCEFVideoMenuBackground> callback;
    @NotNull MCEFVideoMenuBackground background;

    protected MCEFVideoMenuBackgroundConfigScreen(@NotNull MCEFVideoMenuBackground background, @NotNull Consumer<MCEFVideoMenuBackground> callback) {
        super(Component.translatable("fancymenu.backgrounds.video_mcef.configure"));
        this.background = background;
        this.callback = callback;
    }

    @Override
    protected void initCells() {

        this.addStartEndSpacerCell();

        this.addWidgetCell(new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.elements.video_mcef.set_source"), button -> {
            Minecraft.getInstance().setScreen(ResourceChooserScreen.video(null, source -> {
                if (source != null) {
                    this.background.rawVideoUrlSource = ResourceSource.of(source);
                }
                Minecraft.getInstance().setScreen(this);
            }).setSource((this.background.rawVideoUrlSource != null) ? this.background.rawVideoUrlSource.getSerializationSource() : null, false));
        }), true);

        this.addCycleButtonCell(CommonCycles.cycleEnabledDisabled("fancymenu.elements.video_mcef.loop", this.background.loop), true, (value, button) -> {
            this.background.loop = value.getAsBoolean();
        });

        this.addWidgetCell(new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.elements.video_mcef.volume"), button -> {
            Minecraft.getInstance().setScreen(new SetVideoVolumeScreen(this.background.volume, vol -> {
                if (vol != null) {
                    this.background.volume = vol;
                }
                Minecraft.getInstance().setScreen(this);
            }));
        }), true);

        LocalizedGenericValueCycle<SoundSource> soundSourceCycle = CommonCycles.cycle("fancymenu.elements.video_mcef.sound_channel", Arrays.asList(SoundSource.values()), this.background.soundSource)
                .setValueNameSupplier(consumes -> I18n.get("soundCategory." + consumes.getName()))
                .setValueComponentStyleSupplier(consumes -> Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()));
        this.addCycleButtonCell(soundSourceCycle, true, (value, button) -> {
            this.background.soundSource = value;
        });

        this.addCellGroupEndSpacerCell();

        this.addCycleButtonCell(CommonCycles.cycleEnabledDisabled("fancymenu.backgrounds.image.configure.parallax", this.background.parallaxEnabled), true, (value, button) -> {
            this.background.parallaxEnabled = value.getAsBoolean();
        });

        this.addWidgetCell(new ExtendedButton(0, 0, 0, 20, Component.translatable("fancymenu.backgrounds.image.configure.parallax_intensity"), var1 -> {
            final Screen currentScreen = Minecraft.getInstance().screen;
            TextEditorScreen s = TextEditorScreen.build(Component.translatable("fancymenu.backgrounds.image.configure.parallax_intensity"), null, callback -> {
                if (callback != null) {
                    this.background.parallaxIntensityString = callback;
                }
                Minecraft.getInstance().setScreen(currentScreen);
            });
            s.setText(this.background.parallaxIntensityString);
            Minecraft.getInstance().setScreen(s);
        }).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.backgrounds.image.configure.parallax_intensity.desc"))), true);

        WidgetCell invertParallaxCell = this.addCycleButtonCell(CommonCycles.cycleEnabledDisabled("fancymenu.backgrounds.image.configure.invert_parallax", this.background.invertParallax), true, (value, button) -> {
            this.background.invertParallax = value.getAsBoolean();
        });
        if (invertParallaxCell.widget instanceof ExtendedButton b) {
            b.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.backgrounds.image.configure.invert_parallax.desc")));
        }

        this.addStartEndSpacerCell();

    }

    @Override
    protected void init() {

        super.init();

        if (this.doneButton != null) {
            this.doneButton.setTooltipSupplier(consumes -> {
                if (this.background.rawVideoUrlSource == null) return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.backgrounds.video_mcef.configure.no_video"));
                return null;
            });
        }

    }

    @Override
    public boolean allowDone() {
        return (this.background.rawVideoUrlSource != null);
    }

    @Override
    protected void onCancel() {
        this.callback.accept(null);
    }

    @Override
    protected void onDone() {
        this.callback.accept(this.background);
    }

}