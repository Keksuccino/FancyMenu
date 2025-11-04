package de.keksuccino.fancymenu.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.Nullable;

public class MixinCacheCommon {

    @Nullable
    public static PoseStack cached_screen_render_pose_stack = null;
    public static int cached_screen_render_mouseX;
    public static int cached_screen_render_mouseY;
    public static float cached_screen_render_partial;

}
