package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.splash;

import com.google.common.collect.Lists;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class TitleScreenSplashItem extends DeepCustomizationItem {

    private static final Random RANDOM = new Random();
    private static final ResourceLocation SPLASH_TEXTS = new ResourceLocation("texts/splashes.txt");

    public static String cachedSplashText;

    public String splashTextFilePath;
    public int splashRotation = -20;
    public Color splashColor = new Color(255, 255, 0);
    public String splashColorHEX = "#ffff00";

    protected int lastSplashPosX = 0;
    protected int lastSplashPosY = 0;

    public TitleScreenSplashItem(DeepCustomizationElement parentElement, PropertiesSection item) {

        super(parentElement, item);

        this.splashTextFilePath = item.getEntryValue("splash_file_path");

        String splashRot = item.getEntryValue("splash_rotation");
        if ((splashRot != null) && MathUtils.isInteger(splashRot)) {
            splashRotation = Integer.parseInt(splashRot);
        }

        String splashCol = item.getEntryValue("splash_color");
        if (splashCol != null) {
            Color c = RenderUtils.getColorFromHexString(splashCol);
            if (c != null) {
                this.splashColor = c;
                this.splashColorHEX = splashCol;
            }
        }

    }

    //Only used in editor
    @Override
    public int getPosX(GuiScreen menu) {
        return this.lastSplashPosX - (this.getWidth() / 2);
    }

    //Only used in editor
    @Override
    public int getPosY(GuiScreen menu) {
        return this.lastSplashPosY - (this.getHeight() / 2);
    }

    @Override
    public void render(GuiScreen menu) throws IOException {

        this.setWidth(60);
        this.setHeight(30);

        if (!this.hidden) {
            GlStateManager.enableBlend();
            this.renderSplash(Minecraft.getMinecraft().fontRenderer, menu);
        }

    }

    protected void renderSplash(FontRenderer font, GuiScreen s) {

        float finalPosX = (s.width / 2 + 90);
        float finalPosY = 70.0F;

        int originX = 0;
        int originY = 0;

        if (orientation.equalsIgnoreCase("original")) {
            originX = (int) finalPosX;
            originY = (int) finalPosY;
        } else if (orientation.equalsIgnoreCase("top-left")) {
            ;
        } else if (orientation.equalsIgnoreCase("mid-left")) {
            originY = s.height / 2;
        } else if (orientation.equalsIgnoreCase("bottom-left")) {
            originY = s.height;
        } else if (orientation.equalsIgnoreCase("top-centered")) {
            originX = s.width / 2;
        } else if (orientation.equalsIgnoreCase("mid-centered")) {
            originX = s.width / 2;
            originY = s.height / 2;
        } else if (orientation.equalsIgnoreCase("bottom-centered")) {
            originX = s.width / 2;
            originY = s.height;
        } else if (orientation.equalsIgnoreCase("top-right")) {
            originX = s.width;
        } else if (orientation.equalsIgnoreCase("mid-right")) {
            originX = s.width;
            originY = s.height / 2;
        } else if (orientation.equalsIgnoreCase("bottom-right")) {
            originX = s.width;
            originY = s.height;
        }

        finalPosX = originX + posX;
        finalPosY = originY + posY;

        this.lastSplashPosX = (int) finalPosX;
        this.lastSplashPosY = (int) finalPosY;

        if (cachedSplashText == null) {
            this.cachedSplashText = getRandomSplashText();
        }
        if (this.cachedSplashText == null) {
            this.cachedSplashText = "§c< ERROR! UNABLE TO GET SPLASH TEXT! >";
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(finalPosX, finalPosY, 0.0F);
        GlStateManager.rotate((float)this.splashRotation, 0.0F, 0.0F, 1.0F);
        float f = 1.8F - MathHelper.abs(MathHelper.sin((float) (System.currentTimeMillis() % 1000L) / 1000.0F * ((float) Math.PI * 2F)) * 0.1F);
        f = f * 100.0F / (float) (font.getStringWidth(this.cachedSplashText) + 32);
        GlStateManager.scale(f, f, f);

        drawCenteredString(font, this.cachedSplashText, 0, -8, this.splashColor.getRGB());

        GlStateManager.popMatrix();

    }

    public String getRandomSplashText() {

        if ((splashTextFilePath != null) && !splashTextFilePath.replace(" ", "").equals("")) {
            File f = new File(splashTextFilePath);
            if (f.exists() && f.isFile() && f.getPath().toLowerCase().endsWith(".txt")) {
                List<String> l = FileUtils.getFileLines(f);
                if ((l != null) && !l.isEmpty()) {
                    int random = MathUtils.getRandomNumberInRange(0, l.size()-1);
                    return l.get(random);
                }
            }
        }

        String sp = "missingno";
        IResource iresource = null;
        try {
            List<String> list = Lists.<String>newArrayList();
            iresource = Minecraft.getMinecraft().getResourceManager().getResource(SPLASH_TEXTS);
            BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(iresource.getInputStream(), StandardCharsets.UTF_8));
            String s;
            while ((s = bufferedreader.readLine()) != null) {
                s = s.trim();

                if (!s.isEmpty()) {
                    list.add(s);
                }
            }
            if (!list.isEmpty()) {
                while (true) {
                    sp = list.get(RANDOM.nextInt(list.size()));

                    if (sp.hashCode() != 125780783) {
                        break;
                    }
                }
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());

            if (calendar.get(2) + 1 == 12 && calendar.get(5) == 24) {
                sp = "Merry X-mas!";
            }
            else if (calendar.get(2) + 1 == 1 && calendar.get(5) == 1) {
                sp = "Happy new year!";
            }
            else if (calendar.get(2) + 1 == 10 && calendar.get(5) == 31) {
                sp = "OOoooOOOoooo! Spooky!";
            }

        } catch (IOException var8) {
        } finally {
            IOUtils.closeQuietly(iresource);
        }

        return sp;

    }

}