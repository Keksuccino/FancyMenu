package de.keksuccino.fancymenu.customization.listener.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.listener.ListenerRegistry;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.InitialWidgetFocusScreen;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChooseListenerTypeScreen extends PiPWindowBody implements InitialWidgetFocusScreen {

    public static final int PIP_WINDOW_WIDTH = 640;
    public static final int PIP_WINDOW_HEIGHT = 420;

    @NotNull
    protected final Consumer<AbstractListener> callback;
    @Nullable
    protected AbstractListener selectedListener;
    
    protected ScrollArea listenersScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ScrollArea descriptionScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedEditBox searchBar;
    protected ExtendedButton copyVariablesButton;
    protected ExtendedButton popOutDescriptionButton;

    public ChooseListenerTypeScreen(@NotNull Consumer<AbstractListener> callback) {
        super(Component.translatable("fancymenu.listeners.choose_type"));
        this.callback = callback;
    }

    @Override
    protected void init() {
        boolean blur = UIBase.shouldBlur();

        // Initialize search bar
        String oldSearchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        this.searchBar = new ExtendedEditBox(Minecraft.getInstance().font, 20 + 1, 50 + 15 + 1, (this.width / 2) - 40 - 2, 20 - 2, Component.empty());
        this.searchBar.setHintFancyMenu(consumes -> Component.translatable("fancymenu.listeners.choose_type.search"));
        this.searchBar.setValue(oldSearchValue);
        this.searchBar.setResponder(s -> this.updateListenersList());
        this.addRenderableWidget(this.searchBar);
        UIBase.applyDefaultWidgetSkinTo(this.searchBar, blur);
        this.setupInitialFocusWidget(this, this.searchBar);
        
        // Set positions for scroll areas
        this.listenersScrollArea.setSetupForBlurInterface(blur);
        this.listenersScrollArea.setWidth((this.width / 2) - 40, true);
        this.listenersScrollArea.setHeight(this.height - 85 - 25, true);
        this.listenersScrollArea.setX(20, true);
        this.listenersScrollArea.setY(50 + 15 + 25, true);
        this.addRenderableWidget(this.listenersScrollArea);
        
        this.descriptionScrollArea.setSetupForBlurInterface(blur);
        this.descriptionScrollArea.setWidth((this.width / 2) - 40, true);
        this.descriptionScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
        this.descriptionScrollArea.setX(this.width - 20 - this.descriptionScrollArea.getWidthWithBorder(), true);
        this.descriptionScrollArea.setY(50 + 15, true);
        this.descriptionScrollArea.horizontalScrollBar.active = false;
        this.addRenderableWidget(this.descriptionScrollArea);
        
        int copyButtonWidth = 200;
        int copyButtonX = this.width - 20 - copyButtonWidth;
        int copyButtonY = (int) (this.descriptionScrollArea.getYWithBorder() + this.descriptionScrollArea.getHeightWithBorder() + 5);
        this.copyVariablesButton = new ExtendedButton(
                copyButtonX,
                copyButtonY,
                copyButtonWidth,
                20,
                Component.translatable("fancymenu.listeners.choose_type.copy_variables"),
                button -> this.copyVariablesToClipboard()
        ).setIsActiveSupplier(consumes -> this.selectedListener != null);
        this.addRenderableWidget(this.copyVariablesButton);
        UIBase.applyDefaultWidgetSkinTo(this.copyVariablesButton, blur);

        int popOutButtonY = copyButtonY + 5 + 20;
        this.popOutDescriptionButton = new ExtendedButton(
                copyButtonX,
                popOutButtonY,
                copyButtonWidth,
                20,
                Component.translatable("fancymenu.listeners.choose_type.pop_out_description"),
                button -> this.openDescriptionPopout()
        ).setIsActiveSupplier(consumes -> this.selectedListener != null);
        this.addRenderableWidget(this.popOutDescriptionButton);
        UIBase.applyDefaultWidgetSkinTo(this.popOutDescriptionButton, blur);
        
        // Done button
        ExtendedButton doneButton = new ExtendedButton(
                this.width - 20 - 150, 
                this.height - 20 - 20, 
                150, 20, 
                Component.translatable("fancymenu.common_components.done"), 
                button -> this.closeWithResult(this.selectedListener)
        ).setIsActiveSupplier(consumes -> this.selectedListener != null);
        this.addRenderableWidget(doneButton);
        UIBase.applyDefaultWidgetSkinTo(doneButton, blur);
        
        // Cancel button
        ExtendedButton cancelButton = new ExtendedButton(
                this.width - 20 - 150,
                this.height - 20 - 20 - 5 - 20,
                150, 20,
                Component.translatable("fancymenu.common_components.cancel"),
                button -> this.closeWithResult(null)
        );
        this.addRenderableWidget(cancelButton);
        UIBase.applyDefaultWidgetSkinTo(cancelButton, blur);
        
        this.updateListenersList();
        this.setDescription(this.selectedListener);

    }

    @Override
    public void onWindowClosedExternally() {
        this.callback.accept(null);
    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        RenderSystem.enableBlend();
        
        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        graphics.drawString(this.font, titleComp, 20, 20, this.getGenericTextColor(), false);
        
        graphics.drawString(this.font, Component.translatable("fancymenu.listeners.choose_type.available"), 
                20, 50, this.getGenericTextColor(), false);
        
        Component descLabel = Component.translatable("fancymenu.listeners.choose_type.description");
        int descLabelWidth = this.font.width(descLabel);
        graphics.drawString(this.font, descLabel, this.width - 20 - descLabelWidth, 50, 
                this.getGenericTextColor(), false);

        this.performInitialWidgetFocusActionInRender();
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    protected void updateListenersList() {
        String searchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        if (searchValue.isBlank()) searchValue = null;
        
        this.listenersScrollArea.clearEntries();
        
        for (AbstractListener listener : ListenerRegistry.getListeners()) {
            if (!this.listenerFitsSearchValue(listener, searchValue)) continue;
            
            ListenerScrollEntry entry = new ListenerScrollEntry(
                    this.listenersScrollArea,
                    ((MutableComponent)listener.getDisplayName()).withColor(this.getLabelTextColor()),
                    UIBase.getUITheme().bullet_list_dot_color_1,
                    e -> {
                        this.selectedListener = listener;
                        this.setDescription(listener);
                    }
            );
            entry.listener = listener;
            entry.setTextBaseColor(this.getLabelTextColor());
            entry.setDoubleClickAction(() -> {
                if (this.selectedListener == listener) {
                    this.closeWithResult(this.selectedListener);
                }
            });
            this.listenersScrollArea.addEntry(entry);
        }
        
        // Select previously selected listener if still in list
        if (this.selectedListener != null) {
            for (ScrollAreaEntry e : this.listenersScrollArea.getEntries()) {
                if ((e instanceof ListenerScrollEntry entry) && (entry.listener == this.selectedListener)) {
                    e.setSelected(true);
                    break;
                }
            }
        }
    }

    protected void setDescription(@Nullable AbstractListener listener) {

        this.descriptionScrollArea.clearEntries();

        this.descriptionScrollArea.addEntry(new CellScreen.SpacerScrollAreaEntry(this.descriptionScrollArea, 5));

        if ((listener != null) && (listener.getDescription() != null)) {
            for (Component c : listener.getDescription()) {
                this.addDescriptionLine(c);
            }
        }

        this.descriptionScrollArea.addEntry(new CellScreen.SpacerScrollAreaEntry(this.descriptionScrollArea, 5));

    }

    protected void addDescriptionLine(@NotNull Component line) {
        List<Component> lines = new ArrayList<>();
        int maxWidth = (int)(this.descriptionScrollArea.getInnerWidth() - 15F);
        if (this.font.width(line) > maxWidth) {
            this.font.getSplitter().splitLines(line, maxWidth, Style.EMPTY).forEach(formatted -> {
                lines.add(TextFormattingUtils.convertFormattedTextToComponent(formatted));
            });
        } else {
            lines.add(line);
        }
        lines.forEach(component -> {
            TextScrollAreaEntry e = new TextScrollAreaEntry(this.descriptionScrollArea, component, (entry) -> {});
            e.setSelectable(false);
            e.setBackgroundColorHover(e.getBackgroundColorNormal());
            e.setPlayClickSound(false);
            e.setTextBaseColor(this.getLabelTextColor());
            this.descriptionScrollArea.addEntry(e);
        });
    }

    protected void copyVariablesToClipboard() {
        if (this.selectedListener == null) return;

        List<AbstractListener.CustomVariable> variables = this.selectedListener.getCustomVariables();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < variables.size(); i++) {
            if (i > 0) {
                builder.append(System.lineSeparator());
            }
            builder.append("$$").append(variables.get(i).name());
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(builder.toString());
    }

    protected void openDescriptionPopout() {
        if (this.selectedListener == null) return;

        AbstractListener listener = this.selectedListener;
        List<Component> cachedDescription = (listener.getDescription() != null) ? listener.getDescription() : List.of();
        Component title = Component.translatable("fancymenu.listeners.choose_type.description")
                .copy()
                .append(Component.literal(": "))
                .append(listener.getDisplayName().copy());
        ListenerDescriptionPopoutBody popout = new ListenerDescriptionPopoutBody(title, cachedDescription);
        ListenerDescriptionPopoutBody.openInWindow(popout, this.getWindow());
    }

    protected boolean listenerFitsSearchValue(@NotNull AbstractListener listener, @Nullable String searchValue) {
        if ((searchValue == null) || searchValue.isBlank()) return true;
        searchValue = searchValue.toLowerCase();
        
        // Check display name
        if (listener.getDisplayName().getString().toLowerCase().contains(searchValue)) return true;
        
        // Check description
        return this.listenerDescriptionContains(listener, searchValue);
    }

    protected boolean listenerDescriptionContains(@NotNull AbstractListener listener, @NotNull String searchValue) {
        List<Component> desc = listener.getDescription();
        if (desc != null) {
            for (Component c : desc) {
                if (c.getString().toLowerCase().contains(searchValue)) return true;
            }
        }
        return false;
    }

    protected void closeWithResult(@Nullable AbstractListener listener) {
        this.callback.accept(listener);
        this.closeWindow();
    }

    private int getGenericTextColor() {
        return UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_generic_text_color.getColorInt()
                : UIBase.getUITheme().ui_interface_generic_text_color.getColorInt();
    }

    private int getLabelTextColor() {
        return UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_widget_label_color_normal.getColorInt()
                : UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt();
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ChooseListenerTypeScreen screen, @Nullable PiPWindow parentWindow) {
        PiPWindow window = new PiPWindow(screen.getTitle())
                .setScreen(screen)
                .setForceFancyMenuUiScale(true)
                .setAlwaysOnTop(true)
                .setBlockMinecraftScreenInputs(true)
                .setForceFocus(true)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
        return window;
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ChooseListenerTypeScreen screen) {
        return openInWindow(screen, null);
    }

    public static class ListenerDescriptionPopoutBody extends PiPWindowBody {

        public static final int PIP_WINDOW_WIDTH = 360;
        public static final int PIP_WINDOW_HEIGHT = 260;

        @NotNull
        private final List<Component> descriptionLines;
        protected final ScrollArea descriptionScrollArea = new ScrollArea(0, 0, 0, 0);

        public ListenerDescriptionPopoutBody(@NotNull Component title, @NotNull List<Component> descriptionLines) {
            super(title);
            this.descriptionLines = new ArrayList<>(descriptionLines.size());
            for (Component component : descriptionLines) {
                if (component != null) {
                    this.descriptionLines.add(component.copy());
                }
            }
        }

        @Override
        protected void init() {
            boolean blur = UIBase.shouldBlur();

            this.descriptionScrollArea.setSetupForBlurInterface(blur);
            this.descriptionScrollArea.setWidth(this.width - 40, true);
            this.descriptionScrollArea.setHeight(this.height - 40, true);
            this.descriptionScrollArea.setX(20, true);
            this.descriptionScrollArea.setY(20, true);
            this.descriptionScrollArea.horizontalScrollBar.active = false;
            this.addRenderableWidget(this.descriptionScrollArea);

            this.populateDescription();
        }

        @Override
        public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        }

        private void populateDescription() {
            this.descriptionScrollArea.clearEntries();
            this.descriptionScrollArea.addEntry(new CellScreen.SpacerScrollAreaEntry(this.descriptionScrollArea, 5));
            for (Component line : this.descriptionLines) {
                this.addDescriptionLine(line);
            }
            this.descriptionScrollArea.addEntry(new CellScreen.SpacerScrollAreaEntry(this.descriptionScrollArea, 5));
        }

        private void addDescriptionLine(@NotNull Component line) {
            List<Component> lines = new ArrayList<>();
            int maxWidth = (int) (this.descriptionScrollArea.getInnerWidth() - 15F);
            if (this.font.width(line) > maxWidth) {
                this.font.getSplitter().splitLines(line, maxWidth, Style.EMPTY).forEach(formatted -> {
                    lines.add(TextFormattingUtils.convertFormattedTextToComponent(formatted));
                });
            } else {
                lines.add(line);
            }
            int textColor = this.getLabelTextColor();
            lines.forEach(component -> {
                TextScrollAreaEntry e = new TextScrollAreaEntry(this.descriptionScrollArea, component, (entry) -> {});
                e.setSelectable(false);
                e.setBackgroundColorHover(e.getBackgroundColorNormal());
                e.setPlayClickSound(false);
                e.setTextBaseColor(textColor);
                this.descriptionScrollArea.addEntry(e);
            });
        }

        private int getLabelTextColor() {
            return UIBase.shouldBlur()
                    ? UIBase.getUITheme().ui_blur_interface_widget_label_color_normal.getColorInt()
                    : UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt();
        }

        public static @NotNull PiPWindow openInWindow(@NotNull ListenerDescriptionPopoutBody screen, @Nullable PiPWindow anchorWindow) {
            PiPWindow window = new PiPWindow(screen.getTitle())
                    .setScreen(screen)
                    .setForceFancyMenuUiScale(true)
                    .setBlockMinecraftScreenInputs(false)
                    .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                    .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
            if (anchorWindow != null) {
                int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                int offset = 10;
                int x = anchorWindow.getX() + anchorWindow.getWidth() + offset;
                int y = anchorWindow.getY();
                if (x + window.getWidth() > screenWidth) {
                    x = anchorWindow.getX() - window.getWidth() - offset;
                }
                if (x < 0) {
                    x = Math.max(0, screenWidth - window.getWidth());
                }
                if (y + window.getHeight() > screenHeight) {
                    y = Math.max(0, screenHeight - window.getHeight());
                }
                window.setPosition(x, y);
                PiPWindowHandler.INSTANCE.openWindow(window, null);
                return window;
            }
            PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
            return window;
        }

        public static @NotNull PiPWindow openInWindow(@NotNull ListenerDescriptionPopoutBody screen) {
            return openInWindow(screen, null);
        }

    }

    public class ListenerScrollEntry extends TextListScrollAreaEntry {
        
        @Nullable
        public AbstractListener listener;
        protected long lastClickTime = 0;
        protected static final long DOUBLE_CLICK_TIME = 500; // milliseconds
        @Nullable
        protected Runnable doubleClickAction;
        
        public ListenerScrollEntry(ScrollArea parent, @NotNull Component text, @NotNull DrawableColor listDotColor, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
            super(parent, text, listDotColor, onClick);
        }
        
        public void setDoubleClickAction(@Nullable Runnable action) {
            this.doubleClickAction = action;
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            long currentTime = System.currentTimeMillis();
            
            // Check if this is a double-click
            if ((currentTime - this.lastClickTime < DOUBLE_CLICK_TIME) && (this.doubleClickAction != null)) {
                // Double-click detected - execute the double-click action
                this.doubleClickAction.run();
                this.lastClickTime = 0; // Reset to prevent triple clicks
                return;
            }
            
            this.lastClickTime = currentTime;
            
            // Normal single click behavior
            super.onClick(entry, mouseX, mouseY, button);
        }

    }

}


