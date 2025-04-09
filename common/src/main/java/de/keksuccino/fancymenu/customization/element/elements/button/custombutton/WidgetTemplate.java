package de.keksuccino.fancymenu.customization.element.elements.button.custombutton;

import org.jetbrains.annotations.NotNull;

public interface WidgetTemplate {

    public boolean isTemplate();

    public void setIsTemplate(boolean isTemplate);

    public boolean isTemplateApplyWidth();

    public void setTemplateApplyWidth(boolean templateApplyWidth);

    public boolean isTemplateApplyHeight();

    public void setTemplateApplyHeight(boolean templateApplyHeight);

    public boolean isTemplateApplyPosX();

    public void setTemplateApplyPosX(boolean templateApplyPosX);

    public boolean isTemplateApplyPosY();

    public void setTemplateApplyPosY(boolean templateApplyPosY);

    public boolean isTemplateApplyOpacity();

    public void setTemplateApplyOpacity(boolean templateApplyOpacity);

    public boolean isTemplateApplyVisibility();

    public void setTemplateApplyVisibility(boolean templateApplyVisibility);

    public boolean isTemplateApplyLabel();

    public void setTemplateApplyLabel(boolean templateApplyLabel);

    public @NotNull ButtonElement.TemplateSharing getTemplateShareWith();

    public void setTemplateShareWith(@NotNull ButtonElement.TemplateSharing templateShareWith);

}
