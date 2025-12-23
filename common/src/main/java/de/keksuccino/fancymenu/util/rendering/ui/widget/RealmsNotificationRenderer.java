package de.keksuccino.fancymenu.util.rendering.ui.widget;

import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinRealmsNotificationsScreen;
import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderPipelines;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * A renderer for Realms notification icons.
 * This class extracts the notification rendering functionality from RealmsNotificationsScreen
 * and allows for custom positioning of icons.
 */
public class RealmsNotificationRenderer {

    private static final Identifier UNSEEN_NOTIFICATION_SPRITE = Identifier.withDefaultNamespace("icon/unseen_notification");
    private static final Identifier NEWS_SPRITE = Identifier.withDefaultNamespace("icon/news");
    private static final Identifier INVITE_SPRITE = Identifier.withDefaultNamespace("icon/invite");
    private static final Identifier TRIAL_AVAILABLE_SPRITE = Identifier.withDefaultNamespace("icon/trial_available");

    private final IMixinRealmsNotificationsScreen screenAccess;
    private final Minecraft minecraft;
    private final int screenWidth;
    private final int screenHeight;

    /**
     * Creates a new notification renderer for the given RealmsNotificationsScreen.
     *
     * @param screen The RealmsNotificationsScreen to get notification data from
     */
    public RealmsNotificationRenderer(@NotNull RealmsNotificationsScreen screen, int screenWidth, int screenHeight) {
        this.screenAccess = (IMixinRealmsNotificationsScreen)screen;
        this.minecraft = Minecraft.getInstance();
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    /**
     * Returns the default X position for the icons as implemented in RealmsNotificationsScreen.
     * This is the leftmost edge of the notification area (adjusted from original).
     */
    public int getDefaultPositionX() {
        // Convert the original right-side position to an equivalent left-side position
        int originalRightPosition = this.screenWidth / 2 + 100 - 3;
        return originalRightPosition - getTotalWidth();
    }

    /**
     * Returns the default Y position for the icons as implemented in RealmsNotificationsScreen.
     * This is the top edge of the notification area.
     */
    public int getDefaultPositionY() {
        int k = this.screenHeight / 4 + 48;
        return k + 48 + 2;
    }

    /**
     * Calculates the total width needed for all notification icons.
     * This is the maximum offset from the starting X position that will be used.
     */
    public int getTotalWidth() {
        int iconCount = 0;
        boolean hasUnseenNotifications = this.hasUnseenNotifications();
        boolean showOldNotifications = this.shouldShowOldNotifications();
        boolean hasUnreadNews = this.hasUnreadNews();
        int pendingInvites = this.getNumberOfPendingInvites();
        boolean trialAvailable = this.isTrialAvailable();
        if (hasUnseenNotifications) {
            iconCount++;
        }
        if (showOldNotifications) {
            if (hasUnreadNews) {
                iconCount++;
            }
            if (pendingInvites > 0) {
                iconCount++;
            }
            if (trialAvailable) {
                iconCount++;
            }
        }
        // For iconCount icons, we need (iconCount - 1) * 16 pixels of spacing
        // Plus an approximate width for the icons (using 14 as a reasonable estimate)
        return iconCount > 0 ? (iconCount - 1) * 16 + 14 : 0;
    }

    /**
     * Returns the maximum height needed for all notification icons.
     */
    public int getTotalHeight() {
        // The maximum height of any icon with its Y offset is 14 + 4 = 18 pixels
        return 18;
    }

    /**
     * Renders the notification icons at the specified position.
     * Icons are rendered from left to right.
     *
     * @param guiGraphics The GuiGraphics instance to render with
     * @param x The x position to render at (leftmost edge of icon area)
     * @param y The y position to render at (top edge of icon area)
     */
    public void renderIcons(GuiGraphics guiGraphics, int x, int y, int color) {

        // Get current state from the screen
        boolean hasUnseenNotifications = this.hasUnseenNotifications();
        boolean showOldNotifications = this.shouldShowOldNotifications();
        boolean hasUnreadNews = this.hasUnreadNews();
        int pendingInvites = this.getNumberOfPendingInvites();
        boolean trialAvailable = this.isTrialAvailable();

        int currentX = x;

        // Render icons in order from left to right
        if (trialAvailable && showOldNotifications) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, TRIAL_AVAILABLE_SPRITE, currentX, y + 4, 8, 8, color);
            currentX += 16;
        }

        if (pendingInvites > 0 && showOldNotifications) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, INVITE_SPRITE, currentX, y + 1, 14, 14, color);
            currentX += 16;
        }

        if (hasUnreadNews && showOldNotifications) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, NEWS_SPRITE, currentX, y + 1, 14, 14, color);
            currentX += 16;
        }

        if (hasUnseenNotifications) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, UNSEEN_NOTIFICATION_SPRITE, currentX, y + 3, 10, 10, color);
        }

    }

    /**
     * Renders the notification icons at the default position.
     *
     * @param guiGraphics The GuiGraphics instance to render with
     */
    public void renderIcons(GuiGraphics guiGraphics, int color) {
        // Check if client is valid before rendering
        boolean isValidClient = this.screenAccess.get_validClient_FancyMenu().getNow(false);
        if (isValidClient) {
            renderIcons(guiGraphics, getDefaultPositionX(), getDefaultPositionY(), color);
        }
    }

    /**
     * Determines whether old notifications should be shown.
     *
     * @return true if old notifications should be shown
     */
    public boolean shouldShowOldNotifications() {
        if (isEditor()) return true;
        // This mimics the logic in RealmsNotificationsScreen.getConfiguration()
        boolean isValidClient = this.screenAccess.get_validClient_FancyMenu().getNow(false);
        boolean inTitleScreen = this.minecraft.screen instanceof TitleScreen;

        if (!isValidClient || !inTitleScreen) {
            return false;
        }

        return this.minecraft.options.realmsNotifications().get();

    }

    /**
     * Get the current count of pending invites.
     */
    public int getNumberOfPendingInvites() {
        if (isEditor()) return 1;
        return this.screenAccess.get_numberOfPendingInvites_FancyMenu();
    }

    /**
     * Check if there are any unseen notifications.
     */
    public boolean hasUnseenNotifications() {
        if (isEditor()) return true;
        return this.screenAccess.get_hasUnseenNotifications();
    }

    /**
     * Check if there is unread news.
     */
    public boolean hasUnreadNews() {
        if (isEditor()) return true;
        return this.screenAccess.get_hasUnreadNews_FancyMenu();
    }

    /**
     * Check if a trial is available.
     */
    public boolean isTrialAvailable() {
        if (isEditor()) return true;
        return this.screenAccess.get_trialAvailable_FancyMenu();
    }

    protected static boolean isEditor() {
        return (Minecraft.getInstance().screen instanceof LayoutEditorScreen);
    }

}