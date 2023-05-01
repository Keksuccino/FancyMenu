package de.keksuccino.fancymenu.customization.backend.item.v2.items.ticker;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.customization.backend.button.ButtonScriptEngine.ActionContainer;
import de.keksuccino.fancymenu.customization.backend.item.v2.items.IActionExecutorItem;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;


public class TickerCustomizationItem extends CustomizationItem implements IActionExecutorItem {

    
    public volatile List<ActionContainer> actions = new ArrayList<>();
    public volatile long tickDelayMs = 0;
    public volatile boolean isAsync = false;
    public volatile TickMode tickMode = TickMode.NORMAL;

    protected volatile boolean ready = false;
    protected volatile boolean ticked = false;
    protected volatile long lastTick = -1;
    protected volatile TickerItemThreadController asyncThreadController = null;

    public TickerCustomizationItem(CustomizationItemContainer parentContainer, PropertiesSection item) {

        super(parentContainer, item);

        Map<Integer, ActionContainer> tempActions = new HashMap<>();
        for (Map.Entry<String, String> m : item.getEntries().entrySet()) {
            //tickeraction_<index>_ACTION
            if (m.getKey().startsWith("tickeraction_")) {
                String index = m.getKey().split("_", 3)[1];
                String tickerAction = m.getKey().split("_", 3)[2];
                String actionValue = m.getValue();
                if (MathUtils.isInteger(index)) {
                    tempActions.put(Integer.parseInt(index), new ActionContainer(tickerAction, actionValue));
                }
            }
        }
        List<Integer> indexes = new ArrayList<>();
        indexes.addAll(tempActions.keySet());
        Collections.sort(indexes);
        this.actions.clear();
        for (int i : indexes) {
            this.actions.add(tempActions.get(i));
        }

        String tickDelayMsString = item.getEntryValue("tick_delay");
        if ((tickDelayMsString != null) && MathUtils.isLong(tickDelayMsString)) {
            this.tickDelayMs = Long.parseLong(tickDelayMsString);
        }

        String isAsyncString = item.getEntryValue("is_async");
        if ((isAsyncString != null) && isAsyncString.equalsIgnoreCase("true")) {
            this.isAsync = true;
        }

        String tickModeString = item.getEntryValue("tick_mode");
        if (tickModeString != null) {
            TickMode t = TickMode.getByName(tickModeString);
            if (t != null) {
                this.tickMode = t;
            }
        }

    }

    public void tick() {

        if (this.ready && this.shouldRender()) {

            if (this.ticked && (this.tickMode == TickMode.ON_MENU_LOAD)) {
                return;
            }
            if ((this.tickMode == TickMode.ONCE_PER_SESSION) && TickerCustomizationItemContainer.cachedOncePerSessionItems.contains(this.actionId)) {
                return;
            }
            if (this.tickMode == TickMode.ONCE_PER_SESSION) {
                TickerCustomizationItemContainer.cachedOncePerSessionItems.add(this.actionId);
            } else {
                TickerCustomizationItemContainer.cachedOncePerSessionItems.remove(this.actionId);
            }
            long now = System.currentTimeMillis();
            if ((this.tickDelayMs <= 0) || ((this.lastTick + this.tickDelayMs) <= now)) {
                this.lastTick = now;
                this.ticked = true;
                for (ActionContainer a : this.actions) {
                    a.execute();
                }
            }

        }

    }

    @Override
    public void render(PoseStack matrix, Screen menu) throws IOException {

        this.ready = true;

        if (isEditorActive()) {
            RenderSystem.enableBlend();
            fill(matrix, this.getPosX(menu), this.getPosY(menu), this.getPosX(menu) + this.getWidth(), this.getPosY(menu) + this.getHeight(), Color.ORANGE.getRGB());
            drawCenteredString(matrix, Minecraft.getInstance().font, "§l" + Locals.localize("fancymenu.customization.items.ticker"), this.getPosX(menu) + (this.getWidth() / 2), this.getPosY(menu) + (this.getHeight() / 2) - (Minecraft.getInstance().font.lineHeight / 2), -1);
        } else if (!this.isAsync) {
            this.tick();
        }

        //Start thread if not in editor and isAsync
        if (this.isAsync && ((this.asyncThreadController == null) || !this.asyncThreadController.running)) {
            if (!isEditorActive()) {
                this.asyncThreadController = new TickerItemThreadController();
                TickerCustomizationItemContainer.cachedThreadControllers.add(this.asyncThreadController);
                new Thread(() -> {
                    while ((this.asyncThreadController != null) && this.asyncThreadController.running && this.isAsync) {
                        this.tick();
                        try {
                            //Sleep 50ms to tick 20 times per second (like normal MC menus)
                            Thread.sleep(50);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }

        //Stop thread if !isAsync
        if (!this.isAsync && (this.asyncThreadController != null)) {
            this.asyncThreadController.running = false;
        }

    }

    
    @Override
    public List<ActionContainer> getActionList() {
        return this.actions;
    }

    
//    public static class ActionContainer {
//
//        public volatile String action;
//        public volatile String value;
//
//        public ActionContainer(@NotNull String action, @Nullable String value) {
//            this.action = action;
//            this.value = value;
//        }
//
//        public void execute() {
//            try {
//                ButtonScriptEngine.runButtonAction(this.action, this.value);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//    }

    public static class TickerItemThreadController {

        public volatile boolean running = true;

    }

    public static enum TickMode {

        NORMAL("normal"),
        ONCE_PER_SESSION("once_per_session"),
        ON_MENU_LOAD("on_menu_load");

        public final String name;

        TickMode(String name) {
            this.name = name;
        }

        @Nullable
        public static TickMode getByName(String name) {
            for (TickMode t : TickMode.values()) {
                if (t.name.equals(name)) {
                    return t;
                }
            }
            return null;
        }

    }

}
