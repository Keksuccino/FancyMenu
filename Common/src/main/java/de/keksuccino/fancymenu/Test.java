package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinScreen;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.UIComponent;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextListScrollAreaEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation FM_LOGO_LOCATION = new ResourceLocation("fancymenu", "textures/fancymenu_logo.png");

    protected MarkdownRenderer markdownRenderer = new MarkdownRenderer();
    protected boolean init = false;

    @EventListener
    public void onInitScreen(InitOrResizeScreenEvent.Post e) {

        this.markdownRenderer = new MarkdownRenderer();
        markdownRenderer.setOptimalWidth(300);
        markdownRenderer.setX(40);
        markdownRenderer.setY(40);
        markdownRenderer.setText("## About\n" +
                "\n" +
                "Source ~code for the~ FancyMenu _Minecraft mod_.\n" +
                "\n" +
                "**The source code for the different versions of FancyMenu (Forge, Fabric, multiple MC versions) is separated by branches.**\n" +
                "**For example, if you want to see the code for FancyMenu Forge MC 1.16, use the `forge-1.16` branch.**\n" +
                "\n" +
                "> This is a little quote.\n" +
                "I use this to test the quote formatting.\n" +
                "\n" +
                "## Localization\n" +
                "\n" +
                "You want to help me **translate the mod** to your language? That's great!\n" +
                "There is a [separate repository for FancyMenu translations](https://github.com/Keksuccino/FancyMenu-Translations) with all important details you need to know!\n" +
                "\n" +
                "Thank you so much for giving more people the opportunity to use the mod in their language!\n" +
                "\n" +
                "## Download\n" +
                "\n" +
                "FancyMenu is available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fancymenu-forge)!\n" +
                "\n" +
                "## Licensing\n" +
                "\n" +
                "FancyMenu is licensed under DSMSL (DON'T SNATCH MA STUFF LICENSE).\n" +
                "See `LICENSE.md` for more information.\n" +
                "\n" +
                "## Copyright\n" +
                "\n" +
                "- FancyMenu Copyright © 2020-2022 Keksuccino.\n");

        e.getWidgets().add(0, markdownRenderer);

    }

    @EventListener(priority = -2000)
    public void onRenderPost(RenderScreenEvent.Post e) {

        markdownRenderer.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartial());

    }

}
