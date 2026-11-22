package de.keksuccino.fancymenu.customization.variables;

import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Converts the value of a text variable to the entries of a list: it splits the text at a separator, or reads it as a JSON array if it
 * already is one, and previews the entries.
 */
public class ConvertToListWindowBody extends ConvertVariableWindowBody<List<String>> {

    private static final String DEFAULT_SEPARATOR = ",";
    /** Previews of huge values only show this many entries. */
    private static final int MAX_PREVIEW_ENTRIES = 500;

    @NotNull
    private final String value;
    /** The entries of the value if it is a JSON array, or {@code null} if it isn't one. */
    @Nullable
    private final List<String> jsonEntries;
    private boolean readJson;
    private boolean trimEntries = true;
    private boolean skipEmptyEntries = true;
    @Nullable
    private List<String> entries;

    public ConvertToListWindowBody(@NotNull String value, @NotNull Consumer<List<String>> callback) {
        super(Component.translatable("fancymenu.overlay.menu_bar.variables.manage.convert_to_list.title"), DEFAULT_SEPARATOR, callback);
        this.value = Objects.requireNonNull(value);
        this.jsonEntries = VariableLists.parseText(value);
        this.readJson = this.jsonEntries != null;
    }

    @Override
    protected void initOptions() {

        this.separatorBox.setIsActiveSupplier(consumes -> !this.readJson);

        CycleButton<CommonCycles.CycleOnOff> trimButton = new CycleButton<>(0, 0, 0, ROW_HEIGHT, CommonCycles.cycleOnOff("fancymenu.overlay.menu_bar.variables.manage.convert_to_list.trim_entries", this.trimEntries), (value, button) -> {
            this.trimEntries = value.getAsBoolean();
            this.onOptionsChanged();
        });
        trimButton.setIsActiveSupplier(consumes -> !this.readJson);
        trimButton.setUITooltip(UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.variables.manage.convert_to_list.trim_entries.desc")));

        CycleButton<CommonCycles.CycleOnOff> skipEmptyButton = new CycleButton<>(0, 0, 0, ROW_HEIGHT, CommonCycles.cycleOnOff("fancymenu.overlay.menu_bar.variables.manage.convert_to_list.skip_empty_entries", this.skipEmptyEntries), (value, button) -> {
            this.skipEmptyEntries = value.getAsBoolean();
            this.onOptionsChanged();
        });
        skipEmptyButton.setIsActiveSupplier(consumes -> !this.readJson);
        skipEmptyButton.setUITooltip(UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.variables.manage.convert_to_list.skip_empty_entries.desc")));

        this.addOptionRow(trimButton, skipEmptyButton);

        if (this.jsonEntries != null) {
            // The value already is a JSON array (like the text form of a list), so its entries get used unless the user says otherwise
            CycleButton<CommonCycles.CycleOnOff> jsonButton = new CycleButton<>(0, 0, 0, ROW_HEIGHT, CommonCycles.cycleOnOff("fancymenu.overlay.menu_bar.variables.manage.convert_to_list.read_json", this.readJson), (value, button) -> {
                this.readJson = value.getAsBoolean();
                this.onOptionsChanged();
            });
            jsonButton.setUITooltip(UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.variables.manage.convert_to_list.read_json.desc")));
            this.addOptionRow(jsonButton);
        }

    }

    @Override
    protected void onOptionsChanged() {
        super.onOptionsChanged();
        this.entries = null;
    }

    @NotNull
    private List<String> getEntries() {
        List<String> result = this.entries;
        if (result == null) {
            result = ((this.jsonEntries != null) && this.readJson) ? this.jsonEntries : VariableTextUtils.splitToEntries(this.value, this.separator, this.trimEntries, this.skipEmptyEntries);
            this.entries = result;
        }
        return result;
    }

    @Override
    protected @NotNull Component getSeparatorTooltip() {
        return Component.translatable(this.readJson ? "fancymenu.overlay.menu_bar.variables.manage.convert_to_list.separator.json" : "fancymenu.overlay.menu_bar.variables.manage.convert_to_list.separator.desc");
    }

    @Override
    protected void buildPreview(@NotNull ScrollArea area, float width) {
        List<String> previewEntries = this.getEntries();
        int shown = Math.min(previewEntries.size(), MAX_PREVIEW_ENTRIES);
        float indexColumnWidth = VariableRowEntry.getIndexColumnWidth(shown - 1);
        int rowHeight = this.getPreviewRowHeight();
        for (int i = 0; i < shown; i++) {
            area.addEntry(makeStatic(new PreviewRow(area, rowHeight, i, previewEntries.get(i), indexColumnWidth)));
        }
        if (previewEntries.size() > shown) {
            area.addEntry(makeStatic(new PreviewRow(area, rowHeight, -1, I18n.get("fancymenu.overlay.menu_bar.variables.manage.convert_to_list.more_entries", previewEntries.size() - shown), indexColumnWidth)));
        }
    }

    @Override
    protected @Nullable String getPreviewInfo() {
        String count = ManageVariablesWindowBody.getEntryCountText(this.getEntries().size());
        return this.readJson ? I18n.get("fancymenu.overlay.menu_bar.variables.manage.convert_to_list.from_json", count) : count;
    }

    @Override
    protected @NotNull Component getEmptyPreviewHint() {
        return Component.translatable("fancymenu.overlay.menu_bar.variables.manage.empty_list");
    }

    @Override
    protected @NotNull List<String> buildResult() {
        return List.copyOf(this.getEntries());
    }

    /**
     * A row of the preview: the index and the text of an entry, or a muted note without index.
     */
    private static class PreviewRow extends VariableRowEntry {

        private final int index;
        @NotNull
        private final String text;
        private final float indexColumnWidth;

        private PreviewRow(@NotNull ScrollArea parent, int height, int index, @NotNull String text, float indexColumnWidth) {
            super(parent, height);
            this.index = index;
            this.text = text;
            this.indexColumnWidth = indexColumnWidth;
        }

        @Override
        protected float getLeadingWidth() {
            return this.indexColumnWidth;
        }

        @Override
        protected void renderLeading(@NotNull GuiGraphics graphics, float x, float centerY) {
            if (this.index >= 0) renderIndex(graphics, String.valueOf(this.index), x, this.indexColumnWidth, centerY);
        }

        @Override
        protected @NotNull List<Segment> buildSegments() {
            if (this.index < 0) return List.of(new Segment(this.text, TextStyle.MUTED));
            if (this.text.isEmpty()) return List.of(new Segment(I18n.get("fancymenu.overlay.menu_bar.variables.manage.empty_value"), TextStyle.MUTED));
            List<Segment> segments = new ArrayList<>();
            addSingleLineSegments(segments, this.text, TextStyle.LABEL);
            return segments;
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
        }

    }

}
