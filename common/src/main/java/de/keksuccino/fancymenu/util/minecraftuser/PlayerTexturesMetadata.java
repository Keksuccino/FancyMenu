package de.keksuccino.fancymenu.util.minecraftuser;

import org.jetbrains.annotations.Nullable;

public class PlayerTexturesMetadata {

    protected boolean custom;
    protected boolean slim;
    protected SkinTextureMetadata skin;
    protected CapeTextureMetadata cape;

    public boolean isCustom() {
        return custom;
    }

    public boolean isSlim() {
        return slim;
    }

    @Nullable
    public SkinTextureMetadata getSkin() {
        return skin;
    }

    @Nullable
    public CapeTextureMetadata getCape() {
        return cape;
    }

}
