package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
        markdownRenderer.setText("## About [Hyperlink](https://github.com/Keksuccino/FancyMenu-Translations)\n" +
                "\n" +
                "^^^\n" +
                "Source _code for_ the FancyMenu ~Minecraft~ mod.\n" +
                "^^^\n" +
                "\n" +
                "|||\n" +
                "**The source %#bdfc00%code for the different%#% versions of FancyMenu (Forge, Fabric, multiple MC versions) is separated by branches.**\n" +
                "**For example, if you want to see the code for FancyMenu Forge MC 1.16, use the `forge-1.16` branch.**\n" +
                "|||\n" +
                "The text above should be on the right side.\n" +
                "\n" +
                "The following list contains items:\n" +
                "\n" +
                "- Some Item\n" +
                "- Another Itemmmm idfiduf lj erit zieruz tieruz tuirezt uidzf gjhdfgjhdgf jsdhgfe uzretfgd sjhfg sdjhgfd jhgfsdjhf gjhg hjsdgfjhs\n" +
                "  - Level 2 Item\n" +
                "- This is also an item\n" +
                "\n" +
                "---\n" +
                "\n" +
                "The thing above is a separation line.\n" +
                "\n" +
                "![](config/fancymenu/assets/back_norm.png)\n" +
                "\n" +
                "[![](https://upload.wikimedia.org/wikipedia/commons/thumb/4/48/RedCat_8727.jpg/1200px-RedCat_8727.jpg)](https://github.com/Keksuccino/FancyMenu-Translations)\n" +
                "\n" +
                "> This is a %#36ccd188%little quote.\n" +
                "I added this to test the quote feature.%#%\n" +
                "\n" +
                "Some text line before the code block.\n" +
                "This is a `single line code block`.\n" +
                "And this is the line after the code block.\n" +
                "\n" +
                "This is a\n" +
                "```\n" +
                "multi line code block `dfiduidfuiu` iu iou oiuoiui\n" +
                "iu iou oiuoiuioiou **iouoiu** iouoiu oiuoiu oi\n" +
                "iu iou oiuoiuioiou iouoiu _iouoiu_ oiuoiu oiuoiu oiuoi\n" +
                "```\n" +
                "Well, and this is the line after the multi line block.\n" +
                "\n" +
                "## Localization `Code Block`\n" +
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
                "FancyMenu %#36ccd188%is licensed under DSMSL (DON'T SNATCH MA STUFF LICENSE).\n" +
                "See%#% `LICENSE.md` for more information.\n" +
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
