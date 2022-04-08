package de.keksuccino.fancymenu.menu.fancy.item;

import com.google.common.collect.LinkedListMultimap;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.DynamicValueHelper;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class WebStringCustomizationItem extends CustomizationItemBase {

    public volatile LinkedListMultimap<String, Float> lines = LinkedListMultimap.create();
    private volatile boolean updating = false;
    public boolean multiline = false;
    public boolean shadow = false;
    public float scale = 1.0F;
    public Alignment alignment = Alignment.LEFT;
    public String rawURL = "";
    protected volatile int unscaledHeight = 1;
    protected volatile int unscaledWidth = 1;
    protected TextRenderer font = MinecraftClient.getInstance().textRenderer;

    public WebStringCustomizationItem(PropertiesSection item) {
        super(item);
        if ((this.action != null) && this.action.equalsIgnoreCase("addwebtext")) {
            this.value = item.getEntryValue("url");
            if (this.value != null) {
                this.rawURL = this.value;
                this.value = DynamicValueHelper.convertFromRaw(this.value);
            }

            String multi = item.getEntryValue("multiline");
            if ((multi != null) && multi.equalsIgnoreCase("true")) {
                this.multiline = true;
            }

            String sh = item.getEntryValue("shadow");
            if ((sh != null)) {
                if (sh.equalsIgnoreCase("true")) {
                    this.shadow = true;
                }
            }

            String sc = item.getEntryValue("scale");
            if ((sc != null) && MathUtils.isFloat(sc)) {
                this.scale = Float.parseFloat(sc);
            }

            String al = item.getEntryValue("alignment");
            if (al != null) {
                if (al.equalsIgnoreCase("right")) {
                    this.alignment = Alignment.RIGHT;
                }
                if (al.equalsIgnoreCase("centered")) {
                    this.alignment = Alignment.CENTERED;
                }
            }

            this.updateContent(this.value);

        }
    }

    @Override
    public void render(MatrixStack matrix, Screen menu) throws IOException {

        if (!this.shouldRender()) {
            return;
        }

        if (!this.updating) {

            RenderSystem.enableBlend();

            this.setWidth((int) (this.unscaledWidth * this.scale));
            this.setHeight((int) (this.unscaledHeight * this.scale));

            int i = 0;

            for (Map.Entry<String, Float> m : this.lines.entries()) {

                float sc = (this.scale * m.getValue());
                int x = (int) (this.getPosX(menu) / sc);
                int y = (int) (this.getPosY(menu) / sc);
                int stringWidth = (int) (font.getWidth(m.getKey()) * sc);

                if (this.alignment == Alignment.RIGHT) {
                    x = (int) (x + ((this.getWidth() - stringWidth) / sc));
                }

                if (this.alignment == Alignment.CENTERED) {
                    x = (int) (x + (((this.getWidth() - stringWidth) / sc) / 2));
                }

                matrix.push();
                matrix.scale(sc, sc, sc);
                if (this.shadow) {
                    font.drawWithShadow(matrix, "§f" + m.getKey(), x, y + (i / sc), MathHelper.ceil(this.opacity * 255.0F) << 24);
                } else {
                    font.draw(matrix, "§f" + m.getKey(), x, y + (i / sc), MathHelper.ceil(this.opacity * 255.0F) << 24);
                }
                matrix.pop();

                i += (10 * sc);

            }

            RenderSystem.disableBlend();
        }
    }

    @Override
    public int getPosX(Screen menu) {
        int x = super.getPosX(menu);
        if (this.alignment == Alignment.CENTERED) {
            x -= (this.getWidth() / 2);
        } else if (this.alignment == Alignment.RIGHT) {
            x -= this.getWidth();
        }
        return x;
    }

    @Override
    public boolean shouldRender() {
        if ((this.lines == null) || this.lines.isEmpty()) {
            return false;
        }
        return super.shouldRender();
    }

    public void updateContent(String url) {
        new Thread(() -> {

            updating = true;

            String old = value;
            int w = 1;
            int h = 1;

            value = url;
            if (isValidUrl()) {
                //Get raw github file
                if (value.toLowerCase().contains("/blob/") && (value.toLowerCase().startsWith("http://github.com/")
                        || value.toLowerCase().startsWith("https://github.com/") || value.toLowerCase().startsWith("http://www.github.com/")
                        || value.toLowerCase().startsWith("https://www.github.com/"))) {
                    String path = value.replace("//", "").split("/", 2)[1].replace("/blob/", "/");
                    value = "https://raw.githubusercontent.com/" + path;
                }

                //Get raw pastebin file
                if (!value.toLowerCase().contains("/raw/") && (value.toLowerCase().startsWith("http://pastebin.com/")
                        || value.toLowerCase().startsWith("https://pastebin.com/") || value.toLowerCase().startsWith("http://www.pastebin.com/")
                        || value.toLowerCase().startsWith("https://www.pastebin.com/"))) {
                    String path = value.replace("//", "").split("/", 2)[1];
                    value = "https://pastebin.com/raw/" + path;
                }
                try {

                    lines.clear();

                    BufferedReader r = new BufferedReader(new InputStreamReader(new URL(value).openStream(), StandardCharsets.UTF_8));
                    String s = r.readLine();
                    while (s != null) {
                        if (isEditorActive()) {
                            s = StringUtils.convertFormatCodes(s, "&", "§");
                        } else {
                            s = DynamicValueHelper.convertFromRaw(s);
                        }
                        float sc = getScaleMultiplicator(s);
                        s = getWithoutHeadlineCodes(s);
                        int i = (int) (font.getWidth(s) * sc);
                        if (i > w) {
                            w = i;
                        }
                        h += ((float) 10) * sc;
                        lines.put(s, sc);
                        if (!multiline) {
                            break;
                        }
                        s = r.readLine();
                    }
                    r.close();

                    unscaledWidth = w;
                    unscaledHeight = h;
                } catch (Exception e) {
                    lines.clear();
                    String s = Locals.localize("customization.items.webstring.unabletoload");
                    lines.put(s, 1.0F);
                    unscaledWidth = font.getWidth(s);
                    unscaledHeight = 10;
                    e.printStackTrace();
                }

            } else {
                lines.clear();
                String s = Locals.localize("customization.items.webstring.unabletoload");
                lines.put(s, 1.0F);
                unscaledWidth = font.getWidth(s);
                unscaledHeight = 10;

                System.out.println("########################## ERROR ##########################");
                System.out.println("[FM] Cannot load text content from " + value + "! Invalid URL!");
                System.out.println("###########################################################");

                value = old;
            }

            updating = false;

        }).start();
    }

    protected static float getScaleMultiplicator(String s) {
        if (s.startsWith("### ")) {
            return 1.1F;
        } else if (s.startsWith("## ")) {
            return 1.3F;
        } else if (s.startsWith("# ")) {
            return 1.5F;
        }
        return 1.0F;
    }

    protected static String getWithoutHeadlineCodes(String s) {
        if (s.startsWith("### ")) {
            return s.substring(4);
        } else if (s.startsWith("## ")) {
            return s.substring(3);
        } else if (s.startsWith("# ")) {
            return s.substring(2);
        }
        return s;
    }

    private boolean isValidUrl() {
        if ((this.value == null) || (!this.value.startsWith("http://") && !this.value.startsWith("https://")))
            return false;

		/*  I don't remember any possible template were u need alternative method.
			When u used HEAD and server used only GET alternative away don't worked  */
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(this.value).openConnection();
            c.setRequestMethod("GET"); // HEAD -> GET
            if (c.getResponseCode() == 200) return true;
        } catch (Exception e) {
            e.printStackTrace();
            FancyMenu.LOGGER.error(e.getMessage());
            /*  UNDER BIG QUESTION  */
//			System.out.println("Trying alternative method to check for existing url..");
//			try {
//				HttpURLConnection c = (HttpURLConnection) new URL(this.value).openConnection();
//				if (c.getResponseCode() == 200) return true;
//			} catch (Exception e2) {
//				e2.printStackTrace();
//			}
        }
        return false;
    }

}
