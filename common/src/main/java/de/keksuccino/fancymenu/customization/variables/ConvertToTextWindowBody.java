package de.keksuccino.fancymenu.customization.variables;

import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Converts the entries of a list variable to the value of a text variable: it joins them with a separator, or keeps the JSON text of
 * the list, and previews the text.
 */
public class ConvertToTextWindowBody extends ConvertVariableWindowBody<String> {

    private static final String DEFAULT_SEPARATOR = ", ";
    /** Previews of huge texts only show the start of them. */
    private static final int MAX_PREVIEW_CHARS = 5000;

    @NotNull
    private final List<String> entries;
    private boolean keepJson = false;
    @Nullable
    private String text;

    public ConvertToTextWindowBody(@NotNull List<String> entries, @NotNull Consumer<String> callback) {
        super(Component.translatable("fancymenu.overlay.menu_bar.variables.manage.convert_to_text.title"), DEFAULT_SEPARATOR, callback);
        this.entries = List.copyOf(entries);
    }

    @Override
    protected void initOptions() {
        this.separatorBox.setIsActiveSupplier(consumes -> !this.keepJson);
        CycleButton<CommonCycles.CycleOnOff> jsonButton = new CycleButton<>(0, 0, 0, ROW_HEIGHT, CommonCycles.cycleOnOff("fancymenu.overlay.menu_bar.variables.manage.convert_to_text.keep_json", this.keepJson), (value, button) -> {
            this.keepJson = value.getAsBoolean();
            this.onOptionsChanged();
        });
        jsonButton.setUITooltip(UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.variables.manage.convert_to_text.keep_json.desc")));
        this.addOptionRow(jsonButton);
    }

    @Override
    protected void onOptionsChanged() {
        super.onOptionsChanged();
        this.text = null;
    }

    @NotNull
    private String getText() {
        String result = this.text;
        if (result == null) {
            result = this.keepJson ? VariableLists.toText(this.entries) : VariableTextUtils.joinEntries(this.entries, this.separator);
            this.text = result;
        }
        return result;
    }

    @Override
    protected @NotNull Component getSeparatorTooltip() {
        return Component.translatable("fancymenu.overlay.menu_bar.variables.manage.convert_to_text.separator.desc");
    }

    @Override
    protected void buildPreview(@NotNull ScrollArea area, float width) {
        String previewText = this.getText();
        boolean cut = previewText.length() > MAX_PREVIEW_CHARS;
        if (cut) previewText = previewText.substring(0, MAX_PREVIEW_CHARS) + VariableTextUtils.ELLIPSIS;
        if (previewText.isEmpty()) return;
        int textColor = VariableRowEntry.TextStyle.LABEL.getColor();
        // The text wraps like it would in a text field, and its line breaks start new lines
        for (String line : previewText.split("\r\n|\r|\n", -1)) {
            List<MutableComponent> wrapped = UIBase.lineWrapUIComponentsNormal(Component.literal(line), width - 10.0F);
            if (wrapped.isEmpty()) wrapped = List.of(Component.empty());
            for (MutableComponent wrappedLine : wrapped) {
                TextScrollAreaEntry entry = makeStatic(new TextScrollAreaEntry(area, wrappedLine, e -> {}));
                entry.setTextBaseColor(textColor);
                area.addEntry(entry);
            }
        }
    }

    @Override
    protected @Nullable String getPreviewInfo() {
        return null;
    }

    @Override
    protected @NotNull Component getEmptyPreviewHint() {
        return Component.translatable("fancymenu.overlay.menu_bar.variables.manage.empty_value");
    }

    @Override
    protected @NotNull String buildResult() {
        return this.getText();
    }

}
