package de.keksuccino.fancymenu.customization.listener.gui;

import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseIfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.IfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.WhileExecutableBlock;
import de.keksuccino.fancymenu.customization.listener.ListenerHandler;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import de.keksuccino.fancymenu.customization.layout.editor.actions.ManageActionsScreen;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementGroup;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementInstance;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
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
    protected @Nullable List<Component> getCurrentDescription() {

        List<Component> desc = super.getCurrentDescription();
        if (desc == null) return null;
        if (this.selectedInstance == null) return null;

        List<Component> newDesc = new ArrayList<>();

        newDesc.add(Component.translatable("fancymenu.listeners.manage.description.listener_desc").withStyle(ChatFormatting.BOLD));
        newDesc.add(Component.empty());
        newDesc.addAll(desc);

        newDesc.add(Component.empty());
        newDesc.add(Component.empty());

        newDesc.add(Component.translatable("fancymenu.listeners.manage.description.actions").withStyle(ChatFormatting.BOLD));
        newDesc.add(Component.empty());

        List<Component> actionLines = this.buildActionScriptDescription(this.selectedInstance.getActionScript(), 0);
        newDesc.addAll(actionLines);

        return newDesc;

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

    @NotNull
    protected List<Component> buildActionScriptDescription(@NotNull GenericExecutableBlock block, int indentLevel) {
        List<Component> lines = new ArrayList<>();
        
        for (Executable executable : block.getExecutables()) {
            lines.addAll(this.buildExecutableDescription(executable, indentLevel));
        }
        
        if (lines.isEmpty()) {
            String indent = "  ".repeat(Math.max(0, indentLevel));
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().listing_dot_color_1.getColorInt()))
                    .append(Component.translatable("fancymenu.editor.action.screens.manage_screen.info.value.none")
                            .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt()))));
        }
        
        return lines;
    }
    
    @NotNull
    protected List<Component> buildExecutableDescription(@NotNull Executable executable, int indentLevel) {
        List<Component> lines = new ArrayList<>();
        String indent = "  ".repeat(Math.max(0, indentLevel));
        
        if (executable instanceof ActionInstance actionInstance) {
            // Action display name
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().listing_dot_color_2.getColorInt()))
                    .append(actionInstance.action.getActionDisplayName().copy()
                            .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()))));
            
            // Action value (indented more)
            String cachedValue = actionInstance.value;
            String valueString = ((cachedValue != null) && actionInstance.action.hasValue()) 
                    ? cachedValue 
                    : I18n.get("fancymenu.editor.action.screens.manage_screen.info.value.none");
            lines.add(Component.literal(indent + "    ◦ ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().listing_dot_color_1.getColorInt()))
                    .append(Component.literal(I18n.get("fancymenu.editor.action.screens.manage_screen.info.value") + " ")
                            .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())))
                    .append(Component.literal(valueString)
                            .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()))));
            
        } else if (executable instanceof IfExecutableBlock ifBlock) {
            String requirements = this.buildRequirementsString(ifBlock);
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()))
                    .append(Component.translatable("fancymenu.editor.actions.blocks.if", Component.literal(requirements))
                            .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()))));
            
            // Add nested executables
            for (Executable nested : ifBlock.getExecutables()) {
                lines.addAll(this.buildExecutableDescription(nested, indentLevel + 1));
            }
            
            // Handle appended blocks (else if, else)
            AbstractExecutableBlock appended = ifBlock.getAppendedBlock();
            while (appended != null) {
                lines.addAll(this.buildAppendedBlockDescription(appended, indentLevel));
                appended = appended.getAppendedBlock();
            }
            
        } else if (executable instanceof WhileExecutableBlock whileBlock) {
            String requirements = this.buildRequirementsString(whileBlock);
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()))
                    .append(Component.translatable("fancymenu.editor.actions.blocks.while", Component.literal(requirements))
                            .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()))));
            
            // Add nested executables
            for (Executable nested : whileBlock.getExecutables()) {
                lines.addAll(this.buildExecutableDescription(nested, indentLevel + 1));
            }
            
        } else if (executable instanceof AbstractExecutableBlock) {
            // For any other abstract executable blocks
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()))
                    .append(Component.literal("[UNKNOWN BLOCK]")
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.RED))));
        }
        
        return lines;
    }
    
    @NotNull
    protected List<Component> buildAppendedBlockDescription(@NotNull AbstractExecutableBlock block, int indentLevel) {
        List<Component> lines = new ArrayList<>();
        String indent = "  ".repeat(Math.max(0, indentLevel));
        
        if (block instanceof ElseIfExecutableBlock elseIfBlock) {
            String requirements = this.buildRequirementsString(elseIfBlock);
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()))
                    .append(Component.translatable("fancymenu.editor.actions.blocks.else_if", Component.literal(requirements))
                            .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()))));
            
            // Add nested executables
            for (Executable nested : elseIfBlock.getExecutables()) {
                lines.addAll(this.buildExecutableDescription(nested, indentLevel + 1));
            }
            
        } else if (block instanceof ElseExecutableBlock elseBlock) {
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()))
                    .append(Component.translatable("fancymenu.editor.actions.blocks.else")
                            .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()))));
            
            // Add nested executables
            for (Executable nested : elseBlock.getExecutables()) {
                lines.addAll(this.buildExecutableDescription(nested, indentLevel + 1));
            }
        }
        
        return lines;
    }
    
    @NotNull
    protected String buildRequirementsString(@NotNull IfExecutableBlock block) {
        String requirements = "";
        for (LoadingRequirementGroup g : block.condition.getGroups()) {
            if (!requirements.isEmpty()) requirements += ", ";
            requirements += g.identifier;
        }
        for (LoadingRequirementInstance i : block.condition.getInstances()) {
            if (!requirements.isEmpty()) requirements += ", ";
            requirements += i.requirement.getDisplayName();
        }
        return requirements.isEmpty() ? "none" : requirements;
    }
    
    @NotNull
    protected String buildRequirementsString(@NotNull ElseIfExecutableBlock block) {
        String requirements = "";
        for (LoadingRequirementGroup g : block.condition.getGroups()) {
            if (!requirements.isEmpty()) requirements += ", ";
            requirements += g.identifier;
        }
        for (LoadingRequirementInstance i : block.condition.getInstances()) {
            if (!requirements.isEmpty()) requirements += ", ";
            requirements += i.requirement.getDisplayName();
        }
        return requirements.isEmpty() ? "none" : requirements;
    }
    
    @NotNull
    protected String buildRequirementsString(@NotNull WhileExecutableBlock block) {
        String requirements = "";
        for (LoadingRequirementGroup g : block.condition.getGroups()) {
            if (!requirements.isEmpty()) requirements += ", ";
            requirements += g.identifier;
        }
        for (LoadingRequirementInstance i : block.condition.getInstances()) {
            if (!requirements.isEmpty()) requirements += ", ";
            requirements += i.requirement.getDisplayName();
        }
        return requirements.isEmpty() ? "none" : requirements;
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
