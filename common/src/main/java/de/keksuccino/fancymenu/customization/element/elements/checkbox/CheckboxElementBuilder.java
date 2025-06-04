package de.keksuccino.fancymenu.customization.element.elements.checkbox;

import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.ExecutableBlockDeserializer;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CheckboxElementBuilder extends ElementBuilder<CheckboxElement, CheckboxEditorElement> {

    public CheckboxElementBuilder() {
        super("checkbox");
    }

    @Override
    public @NotNull CheckboxElement buildDefaultInstance() {
        CheckboxElement element = new CheckboxElement(this);
        element.baseWidth = 20;
        element.baseHeight = 20;
        return element;
    }

    @Override
    public CheckboxElement deserializeElement(@NotNull SerializedElement serialized) {
        
        CheckboxElement element = buildDefaultInstance();
        
        String checkboxExecutableBlockId = serialized.getValue("checkbox_element_executable_block_identifier");
        if (checkboxExecutableBlockId != null) {
            AbstractExecutableBlock b = ExecutableBlockDeserializer.deserializeWithIdentifier(serialized, checkboxExecutableBlockId);
            if (b instanceof GenericExecutableBlock g) {
                element.actionExecutor = g;
            }
        }
        
        element.hoverSound = deserializeAudioResourceSupplier(serialized.getValue("hoversound"));
        element.clickSound = deserializeAudioResourceSupplier(serialized.getValue("clicksound"));
        
        element.tooltip = serialized.getValue("description");
        
        String borderColorString = serialized.getValue("border_color");
        if (borderColorString != null) {
            element.borderColor = DrawableColor.of(borderColorString);
        }
        
        String backgroundColorString = serialized.getValue("background_color");
        if (backgroundColorString != null) {
            element.backgroundColor = DrawableColor.of(backgroundColorString);
        }
        
        element.checkmarkTexture = deserializeImageResourceSupplier(serialized.getValue("checkmark_texture"));
        
        element.navigatable = deserializeBoolean(element.navigatable, serialized.getValue("navigatable"));
        
        String activeStateRequirementContainerIdentifier = serialized.getValue("widget_active_state_requirement_container_identifier");
        if (activeStateRequirementContainerIdentifier != null) {
            LoadingRequirementContainer c = LoadingRequirementContainer.deserializeWithIdentifier(activeStateRequirementContainerIdentifier, serialized);
            if (c != null) {
                element.activeStateSupplier = c;
            }
        }

        element.prepareExecutableBlock();
        
        return element;
        
    }

    @Override
    public @Nullable CheckboxElement deserializeElementInternal(@NotNull SerializedElement serialized) {
        CheckboxElement element = super.deserializeElementInternal(serialized);
        if (element != null) element.prepareLoadingRequirementContainer();
        return element;
    }

    @Override
    protected @NotNull SerializedElement serializeElement(@NotNull CheckboxElement element, @NotNull SerializedElement serializeTo) {
        
        serializeTo.putProperty("checkbox_element_executable_block_identifier", element.actionExecutor.identifier);
        element.actionExecutor.serializeToExistingPropertyContainer(serializeTo);
        
        if (element.hoverSound != null) {
            serializeTo.putProperty("hoversound", element.hoverSound.getSourceWithPrefix());
        }
        
        if (element.clickSound != null) {
            serializeTo.putProperty("clicksound", element.clickSound.getSourceWithPrefix());
        }
        
        if (element.tooltip != null) {
            serializeTo.putProperty("description", element.tooltip);
        }
        
        if (element.borderColor != null) {
            serializeTo.putProperty("border_color", element.borderColor.getHex());
        }
        
        if (element.backgroundColor != null) {
            serializeTo.putProperty("background_color", element.backgroundColor.getHex());
        }
        
        if (element.checkmarkTexture != null) {
            serializeTo.putProperty("checkmark_texture", element.checkmarkTexture.getSourceWithPrefix());
        }
        
        serializeTo.putProperty("navigatable", "" + element.navigatable);
        
        serializeTo.putProperty("widget_active_state_requirement_container_identifier", element.activeStateSupplier.identifier);
        element.activeStateSupplier.serializeToExistingPropertyContainer(serializeTo);
        
        return serializeTo;
        
    }

    @Override
    public @NotNull CheckboxEditorElement wrapIntoEditorElement(@NotNull CheckboxElement element, @NotNull LayoutEditorScreen editor) {
        return new CheckboxEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.checkbox");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.checkbox.desc");
    }

}
