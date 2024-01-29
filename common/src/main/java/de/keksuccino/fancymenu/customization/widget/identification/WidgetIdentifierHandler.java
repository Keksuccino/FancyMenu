package de.keksuccino.fancymenu.customization.widget.identification;

import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.WidgetIdentificationContext;
import de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.WidgetIdentificationContextRegistry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import de.keksuccino.konkrete.math.MathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WidgetIdentifierHandler {

    public static boolean isIdentifierOfWidget(@NotNull String widgetIdentifier, @NotNull WidgetMeta meta) {
        widgetIdentifier = widgetIdentifier.replace("button_compatibility_id:", "");
        //TODO übernehmen (experimental)
        widgetIdentifier = widgetIdentifier.replace("vanillabtn:", "");
        if ((meta.getWidget() instanceof UniqueWidget u) && widgetIdentifier.equals(u.getWidgetIdentifierFancyMenu())) return true;
        if (MathUtils.isLong(widgetIdentifier)) {
            return widgetIdentifier.equals("" + meta.getLongIdentifier());
        }
        return widgetIdentifier.equals(meta.getUniversalIdentifier());
    }

    @Nullable
    public static String getUniversalIdentifierForWidgetMeta(@NotNull WidgetMeta meta) {
        if ((meta.getWidget() instanceof UniqueWidget u) && (u.getWidgetIdentifierFancyMenu() != null)) return u.getWidgetIdentifierFancyMenu();
        try {
            WidgetIdentificationContext c = WidgetIdentificationContextRegistry.getContextForScreen(meta.getScreen().getClass());
            if (c != null) {
                return c.getUniversalIdentifierForWidget(meta);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void setUniversalIdentifierOfWidgetMeta(@NotNull WidgetMeta meta) {
        meta.setUniversalIdentifier(getUniversalIdentifierForWidgetMeta(meta));
    }

}
