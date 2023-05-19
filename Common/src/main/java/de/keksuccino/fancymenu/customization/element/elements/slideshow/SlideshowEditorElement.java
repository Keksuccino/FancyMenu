package de.keksuccino.fancymenu.customization.element.elements.slideshow;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import org.jetbrains.annotations.NotNull;

public class SlideshowEditorElement extends AbstractEditorElement {

    public SlideshowEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        //TODO add "set slideshow" entry that opens ChooseSlideshow screen

        //TODO add "restore aspect ratio" entry

    }

}
