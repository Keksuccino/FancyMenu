package de.keksuccino.fancymenu.customization.element;

public class EditorElementSettings {

    protected AbstractEditorElement editorElement;

    private boolean destroyable = true;
    private boolean stretchable = true;
    private boolean orderable = true;
    private boolean copyable = true;
    private boolean delayable = true;
    private boolean fadeable = true;
    private boolean resizeable = true;
    private boolean supportsAdvancedPositioning = true;
    private boolean supportsAdvancedSizing = true;
    private boolean resizeableX = true;
    private boolean resizeableY = true;
    private boolean dragable = true;
    private boolean orientationCanBeChanged = true;
    private boolean enableElementIdCopyButton = true;
    private boolean allowOrientationByElement = true;
    private boolean enableLoadingRequirements = true;

    public boolean isDestroyable() {
        return destroyable;
    }

    public void setDestroyable(boolean destroyable) {
        this.destroyable = destroyable;
        this.settingsChanged();
    }

    public boolean isStretchable() {
        return stretchable;
    }

    public void setStretchable(boolean stretchable) {
        this.stretchable = stretchable;
        this.settingsChanged();
    }

    public boolean isOrderable() {
        return orderable;
    }

    public void setOrderable(boolean orderable) {
        this.orderable = orderable;
        this.settingsChanged();
    }

    public boolean isCopyable() {
        return copyable;
    }

    public void setCopyable(boolean copyable) {
        this.copyable = copyable;
        this.settingsChanged();
    }

    public boolean isDelayable() {
        return delayable;
    }

    public void setDelayable(boolean delayable) {
        this.delayable = delayable;
        this.settingsChanged();
    }

    public boolean isFadeable() {
        return fadeable;
    }

    public void setFadeable(boolean fadeable) {
        this.fadeable = fadeable;
        this.settingsChanged();
    }

    public boolean isResizeable() {
        return resizeable;
    }

    public void setResizeable(boolean resizeable) {
        this.resizeable = resizeable;
        this.settingsChanged();
    }

    public boolean isAdvancedPositioningSupported() {
        return supportsAdvancedPositioning;
    }

    public void setAdvancedPositioningSupported(boolean supported) {
        this.supportsAdvancedPositioning = supported;
        this.settingsChanged();
    }

    public boolean isAdvancedSizingSupported() {
        return supportsAdvancedSizing;
    }

    public void setAdvancedSizingSupported(boolean supported) {
        this.supportsAdvancedSizing = supported;
        this.settingsChanged();
    }

    public boolean isResizeableX() {
        return resizeableX;
    }

    public void setResizeableX(boolean resizeableX) {
        this.resizeableX = resizeableX;
        this.settingsChanged();
    }

    public boolean isResizeableY() {
        return resizeableY;
    }

    public void setResizeableY(boolean resizeableY) {
        this.resizeableY = resizeableY;
        this.settingsChanged();
    }

    public boolean isDragable() {
        return dragable;
    }

    public void setDragable(boolean dragable) {
        this.dragable = dragable;
        this.settingsChanged();
    }

    public boolean isOrientationChangeable() {
        return orientationCanBeChanged;
    }

    public void setOrientationChangeable(boolean changeable) {
        this.orientationCanBeChanged = changeable;
        this.settingsChanged();
    }

    public boolean isElementIdCopyButtonEnabled() {
        return enableElementIdCopyButton;
    }

    public void setElementIdCopyButtonEnabled(boolean enabled) {
        this.enableElementIdCopyButton = enabled;
        this.settingsChanged();
    }

    public boolean isOrientationByElementAllowed() {
        return allowOrientationByElement;
    }

    public void setOrientationByElementAllowed(boolean allow) {
        this.allowOrientationByElement = allow;
        this.settingsChanged();
    }

    public boolean isLoadingRequirementsEnabled() {
        return enableLoadingRequirements;
    }

    public void setLoadingRequirementsEnabled(boolean enabled) {
        this.enableLoadingRequirements = enabled;
        this.settingsChanged();
    }

    protected void settingsChanged() {
        if (this.editorElement != null) {
            this.editorElement.onSettingsChanged();
        }
    }

}
