package de.keksuccino.fancymenu.menu.animation.v2.resource.packresources;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;
import net.minecraft.server.packs.AbstractPackResources;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class AnimationPackResources extends AbstractPackResources {

    public File animationPackFileOrFolder;

    public AnimationPackResources(File animationPackFileOrFolder) {
        super(animationPackFileOrFolder);
        this.animationPackFileOrFolder = animationPackFileOrFolder;
    }

    @Nullable
    public PropertiesSection getAnimationPackMeta() {
        try {
            InputStream i = getResource("animation.properties");
            if (i != null) {
                File tempDir = new File(FancyMenu.INSTANCE_TEMP_DATA_DIR.getPath() + "/" + UUID.randomUUID());
                tempDir.mkdirs();
                File propsFile = new File(tempDir.getPath() + "/animation.properties");
                FileUtils.copyInputStreamToFile(i, propsFile);
                PropertiesSet set = PropertiesSerializer.getProperties(propsFile.getPath());
                propsFile.delete();
                tempDir.delete();
                if (set != null) {
                    List<PropertiesSection> secs = set.getPropertiesOfType("animation-meta");
                    if ((secs != null) && !secs.isEmpty()) {
                        return secs.get(0);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //TODO Forge Only - Mixins needed in Fabric version: Override 'updatePackList' in 'PackScreen'
    @Override
    public boolean isHidden() {
        return true;
    }

}
