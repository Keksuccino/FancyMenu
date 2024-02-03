package de.keksuccino.fancymenu.util.minecraftuser;

import org.jetbrains.annotations.Nullable;

public class MinecraftUserMetadata {

    protected String uuid;
    protected String username;
    protected PlayerTexturesMetadata textures;

    @Nullable
    public String getUuid() {
        return uuid;
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    @Nullable
    public PlayerTexturesMetadata getTexturesMetadata() {
        return textures;
    }

}
