package de.keksuccino.fancymenu.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.Nullable;

public class MixinCacheCommon {

    @Nullable
    public static PoseStack current_screen_render_pose_stack = null;

}
