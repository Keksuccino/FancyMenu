package de.keksuccino.fancymenu.util.minecraftuser.v2;

import org.jetbrains.annotations.Nullable;
import java.util.UUID;

//TODO übernehmen
public class UserProfile {

    protected String id;
    protected String name;

    @Nullable
    public UUID getUUID() {
        if (this.id == null) return null;
        return UUID.fromString(this.id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }

    @Nullable
    public String getName() {
        return this.name;
    }

}
