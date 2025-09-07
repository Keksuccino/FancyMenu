package de.keksuccino.fancymenu.customization.listener.gui;

import de.keksuccino.fancymenu.customization.listener.ListenerHandler;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import de.keksuccino.fancymenu.customization.layout.editor.actions.ManageActionsScreen;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
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

public class ManageListenersScreen extends CellScreen {

    @NotNull
    protected final Consumer<Boolean> callback;
    @NotNull
    protected final List<ListenerInstance> tempInstances = new ArrayList<>();
    @Nullable
    protected ListenerInstance selectedInstance;

    public ManageListenersScreen(@NotNull Consumer<Boolean> callback) {
        super(Component.translatable("fancymenu.listeners.manage"));
        this.callback = callback;
        // Create a copy of all listener instances to work with
        this.tempInstances.addAll(ListenerHandler.getInstances());
        this.setSearchBarEnabled(true);
        this.setDescriptionAreaEnabled(true);
    }

    @Override
    protected void initCells() {
        
        this.addSpacerCell(10);
        
        // Add all listener instances to the list
        for (ListenerInstance instance : this.tempInstances) {
            ListenerInstanceCell cell = new ListenerInstanceCell(instance);
            this.addCell(cell).setSelectable(true);
        }
        
        this.addStartEndSpacerCell();
        
    }

    @Override
    protected void initRightSideWidgets() {
        
        // Add listener button
        this.addRightSideButton(20, Component.translatable("fancymenu.listeners.manage.add"), button -> {
            ChooseListenerTypeScreen chooseScreen = new ChooseListenerTypeScreen(listener -> {
                if (listener != null) {
                    // Create new instance and open action editor
                    ListenerInstance newInstance = listener.createFreshInstance();
                    ManageActionsScreen actionsScreen = new ManageActionsScreen(newInstance.getActionScript(), updatedScript -> {
                        if (updatedScript != null) {
                            newInstance.setActionScript(updatedScript);
                            this.tempInstances.add(newInstance);
                            this.rebuild();
                        }
                        Minecraft.getInstance().setScreen(this);
                    });
                    Minecraft.getInstance().setScreen(actionsScreen);
                } else {
                    Minecraft.getInstance().setScreen(this);
                }
            });
            Minecraft.getInstance().setScreen(chooseScreen);
        });
        
        this.addRightSideDefaultSpacer();
        
        // Edit listener button
        this.addRightSideButton(20, Component.translatable("fancymenu.listeners.manage.edit"), button -> {
            if (this.selectedInstance != null) {
                ManageActionsScreen actionsScreen = new ManageActionsScreen(this.selectedInstance.getActionScript(), updatedScript -> {
                    if (updatedScript != null) {
                        this.selectedInstance.setActionScript(updatedScript);
                    }
                    Minecraft.getInstance().setScreen(this);
                });
                Minecraft.getInstance().setScreen(actionsScreen);
            }
        }).setIsActiveSupplier(consumes -> this.selectedInstance != null);
        
        // Remove listener button
        this.addRightSideButton(20, Component.translatable("fancymenu.listeners.manage.remove"), button -> {
            if (this.selectedInstance != null) {
                this.tempInstances.remove(this.selectedInstance);
                this.selectedInstance = null;
                this.rebuild();
            }
        }).setIsActiveSupplier(consumes -> this.selectedInstance != null);
        
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.updateSelectedInstance();
        super.render(graphics, mouseX, mouseY, partial);
    }

    @Override
    protected void onCancel() {
        this.callback.accept(false);
    }

    @Override
    protected void onDone() {
        // Clear existing instances
        for (ListenerInstance instance : new ArrayList<>(ListenerHandler.getInstances())) {
            ListenerHandler.removeInstance(instance.instanceIdentifier);
        }
        
        // Add all temp instances to handler
        for (ListenerInstance instance : this.tempInstances) {
            ListenerHandler.addInstance(instance);
        }
        
        this.callback.accept(true);
    }

    protected void updateSelectedInstance() {
        RenderCell selected = this.getSelectedCell();
        if (selected instanceof ListenerInstanceCell cell) {
            this.selectedInstance = cell.instance;
        } else {
            this.selectedInstance = null;
        }
    }

    public class ListenerInstanceCell extends LabelCell {
        
        @NotNull
        protected final ListenerInstance instance;
        
        public ListenerInstanceCell(@NotNull ListenerInstance instance) {

            super(buildLabel(instance));
            this.instance = instance;

            this.setDescriptionSupplier(this.instance.parent::getDescription);

        }
        
        @NotNull
        private static Component buildLabel(@NotNull ListenerInstance instance) {
            MutableComponent displayName = instance.parent.getDisplayName().copy()
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()));
            MutableComponent identifier = Component.literal(" [" + instance.instanceIdentifier + "]")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt()));
            return displayName.append(identifier);
        }
        
    }

}
