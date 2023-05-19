package de.keksuccino.fancymenu.customization.element.elements.animation;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import org.jetbrains.annotations.NotNull;

public class AnimationEditorElement extends AbstractEditorElement {

    public AnimationEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        //TODO add "set animation" entry that opens ChooseAnimation screen

        //TODO add "restore aspect ratio" entry

    }

}
