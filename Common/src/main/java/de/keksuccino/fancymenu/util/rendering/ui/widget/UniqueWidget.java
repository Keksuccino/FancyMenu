package de.keksuccino.fancymenu.util.rendering.ui.widget;

import org.jetbrains.annotations.Nullable;

public interface UniqueWidget<T> {

    T setIdentifier(@Nullable String identifier);

    String getIdentifier();

}
