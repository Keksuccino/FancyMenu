package de.keksuccino.fancymenu.customization.element.editor;

public class EditorElementSettings {

    protected AbstractEditorElement editorElement;

    private boolean destroyable = true;
    private boolean hideInsteadOfDestroy = false;
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
    private boolean movable = true;
    private boolean anchorPointCanBeChanged = true;
    private boolean allowElementAnchorPoint = true;
    private boolean allowVanillaAnchorPoint = false;
    private boolean enableLoadingRequirements = true;
    private boolean identifierCopyable = true;
    private boolean opacityChangeable = true;
    private boolean parallaxAllowed = true;
    private boolean autoSizingAllowed = true;
    private boolean stayOnScreenAllowed = true;
    private boolean stickyAnchorAllowed = true;
    private boolean inEditorColorSupported = false;

    private boolean skipReInit = false;

    public boolean isInEditorColorSupported() {
        return inEditorColorSupported;
    }

    public void setInEditorColorSupported(boolean inEditorColorSupported) {
        this.inEditorColorSupported = inEditorColorSupported;
        this.settingsChanged();
    }

    public boolean isParallaxAllowed() {
        return parallaxAllowed;
    }

    public void setParallaxAllowed(boolean parallaxAllowed) {
        this.parallaxAllowed = parallaxAllowed;
        this.settingsChanged();
    }

    public boolean isAutoSizingAllowed() {
        return autoSizingAllowed;
    }

    public void setAutoSizingAllowed(boolean autoSizingAllowed) {
        this.autoSizingAllowed = autoSizingAllowed;
        this.settingsChanged();
    }

    public boolean isStayOnScreenAllowed() {
        return stayOnScreenAllowed;
    }

    public void setStayOnScreenAllowed(boolean stayOnScreenAllowed) {
        this.stayOnScreenAllowed = stayOnScreenAllowed;
        this.settingsChanged();
    }

    public boolean isStickyAnchorAllowed() {
        return stickyAnchorAllowed;
    }

    public void setStickyAnchorAllowed(boolean stickyAnchorAllowed) {
        this.stickyAnchorAllowed = stickyAnchorAllowed;
        this.settingsChanged();
    }

    public boolean isDestroyable() {
        return destroyable;
    }

    public void setDestroyable(boolean destroyable) {
        this.destroyable = destroyable;
        this.settingsChanged();
    }

    public boolean shouldHideInsteadOfDestroy() {
        return this.hideInsteadOfDestroy;
    }

    public void setHideInsteadOfDestroy(boolean hideInsteadOfDestroy) {
        this.hideInsteadOfDestroy = hideInsteadOfDestroy;
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

    public boolean isOpacityChangeable() {
        return this.opacityChangeable;
    }

    public void setOpacityChangeable(boolean changeable) {
        this.opacityChangeable = changeable;
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

    public boolean isMovable() {
        return this.movable;
    }

    public void setMovable(boolean movable) {
        this.movable = movable;
        this.settingsChanged();
    }

    public boolean isAnchorPointChangeable() {
        return anchorPointCanBeChanged;
    }

    public void setAnchorPointChangeable(boolean changeable) {
        this.anchorPointCanBeChanged = changeable;
        this.settingsChanged();
    }

    public boolean isElementAnchorPointAllowed() {
        return allowElementAnchorPoint;
    }

    public void setElementAnchorPointAllowed(boolean allow) {
        this.allowElementAnchorPoint = allow;
        this.settingsChanged();
    }

    public boolean isVanillaAnchorPointAllowed() {
        return this.allowVanillaAnchorPoint;
    }

    public void setVanillaAnchorPointAllowed(boolean allow) {
        this.allowVanillaAnchorPoint = allow;
        this.settingsChanged();
    }

    public boolean isLoadingRequirementsEnabled() {
        return enableLoadingRequirements;
    }

    public void setLoadingRequirementsEnabled(boolean enabled) {
        this.enableLoadingRequirements = enabled;
        this.settingsChanged();
    }

    public boolean isIdentifierCopyable() {
        return this.identifierCopyable;
    }

    public void setIdentifierCopyable(boolean copyable) {
        this.identifierCopyable = copyable;
        this.settingsChanged();
    }

    /**
     * This skips the re-init of the element after changing settings.
     */
    public void setSkipReInitAfterSettingsChanged(boolean skip) {
        this.skipReInit = skip;
    }

    public void settingsChanged() {
        if ((this.editorElement != null) && !this.skipReInit) {
            this.editorElement.onSettingsChanged();
        }
    }

}
