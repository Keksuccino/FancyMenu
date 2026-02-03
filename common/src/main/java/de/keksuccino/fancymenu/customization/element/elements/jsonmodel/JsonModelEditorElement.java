package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class JsonModelEditorElement extends AbstractEditorElement<JsonModelEditorElement, JsonModelElement> {

    public JsonModelEditorElement(@NotNull JsonModelElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {
        super.init();

        ContextMenu modelMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("json_model_model", Component.translatable("fancymenu.elements.json_model.menu.model"), modelMenu)
                .setIcon(MaterialIcons.DATA_OBJECT);

        this.element.modelSource.buildContextMenuEntryAndAddTo(modelMenu, this)
                .setIcon(MaterialIcons.DATA_OBJECT);
        this.element.useModelDisplayTransform.buildContextMenuEntryAndAddTo(modelMenu, this)
                .setIcon(MaterialIcons.ROTATE_RIGHT);

        ContextMenu textureMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("json_model_texture", Component.translatable("fancymenu.elements.json_model.menu.texture"), textureMenu)
                .setIcon(MaterialIcons.IMAGE);

        this.element.textureSource.buildContextMenuEntryAndAddTo(textureMenu, this)
                .setIcon(MaterialIcons.IMAGE);
        this.element.useTextureOverride.buildContextMenuEntryAndAddTo(textureMenu, this)
                .setIcon(MaterialIcons.LINK);
        this.element.renderTranslucent.buildContextMenuEntryAndAddTo(textureMenu, this)
                .setIcon(MaterialIcons.TUNE);

        ContextMenu transformMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("json_model_transform", Component.translatable("fancymenu.elements.json_model.menu.transform"), transformMenu)
                .setIcon(MaterialIcons._3D_ROTATION);

        this.element.modelScale.buildContextMenuEntryAndAddTo(transformMenu, this)
                .setIcon(MaterialIcons.STRAIGHTEN);
        this.element.modelOffsetX.buildContextMenuEntryAndAddTo(transformMenu, this)
                .setIcon(MaterialIcons.MOVE);
        this.element.modelOffsetY.buildContextMenuEntryAndAddTo(transformMenu, this)
                .setIcon(MaterialIcons.MOVE);
        this.element.modelOffsetZ.buildContextMenuEntryAndAddTo(transformMenu, this)
                .setIcon(MaterialIcons.MOVE);
        this.element.modelRotationX.buildContextMenuEntryAndAddTo(transformMenu, this)
                .setIcon(MaterialIcons.ROTATE_RIGHT);
        this.element.modelRotationY.buildContextMenuEntryAndAddTo(transformMenu, this)
                .setIcon(MaterialIcons.ROTATE_RIGHT);
        this.element.modelRotationZ.buildContextMenuEntryAndAddTo(transformMenu, this)
                .setIcon(MaterialIcons.ROTATE_RIGHT);

        ContextMenu lightMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("json_model_lighting", Component.translatable("fancymenu.elements.json_model.menu.lighting"), lightMenu)
                .setIcon(MaterialIcons.LIGHTBULB);

        this.element.lightHue.buildContextMenuEntryAndAddTo(lightMenu, this)
                .setIcon(MaterialIcons.PALETTE);
        this.element.lightRotationX.buildContextMenuEntryAndAddTo(lightMenu, this)
                .setIcon(MaterialIcons.ROTATE_RIGHT);
        this.element.lightRotationY.buildContextMenuEntryAndAddTo(lightMenu, this)
                .setIcon(MaterialIcons.ROTATE_RIGHT);
        this.element.lightRotationZ.buildContextMenuEntryAndAddTo(lightMenu, this)
                .setIcon(MaterialIcons.ROTATE_RIGHT);
        this.element.light0X.buildContextMenuEntryAndAddTo(lightMenu, this)
                .setIcon(MaterialIcons.LIGHTBULB);
        this.element.light0Y.buildContextMenuEntryAndAddTo(lightMenu, this)
                .setIcon(MaterialIcons.LIGHTBULB);
        this.element.light0Z.buildContextMenuEntryAndAddTo(lightMenu, this)
                .setIcon(MaterialIcons.LIGHTBULB);
        this.element.light1X.buildContextMenuEntryAndAddTo(lightMenu, this)
                .setIcon(MaterialIcons.LIGHTBULB);
        this.element.light1Y.buildContextMenuEntryAndAddTo(lightMenu, this)
                .setIcon(MaterialIcons.LIGHTBULB);
        this.element.light1Z.buildContextMenuEntryAndAddTo(lightMenu, this)
                .setIcon(MaterialIcons.LIGHTBULB);
    }
}
