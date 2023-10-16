package de.keksuccino.fancymenu.util.resources.text;

import de.keksuccino.fancymenu.util.resources.Resource;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public interface IText extends Resource {

    /**
     * Due to the nature of some types of {@link IText}, the return value of this method could change
     * asynchronously, so make sure to always cache the value before working with it.
     */
    @Nullable
    List<String> getTextLines();

}
