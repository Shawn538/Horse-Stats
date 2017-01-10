package com.gmail.nuclearcat1337.horse_stats;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.text.DecimalFormat;
import java.util.logging.Logger;

/*
Created by Mr_Little_Kitty on 12/17/2015
*/
@Mod(modid = HorseStats.MODID, name = HorseStats.MODNAME, version = HorseStats.MODVERSION)
public class HorseStats
{
    public static final String MODID = "horsestats";
    public static final String MODNAME = "Horse Stats";
    public static final String MODVERSION = "2.0.0";

    private static Minecraft mc = Minecraft.getMinecraft();

    @Mod.Instance(MODID)
    public static HorseStats instance;
    public static Logger logger = Logger.getLogger("HorseStats");

    private DecimalFormat decimalFormat;


    @Mod.EventHandler
    public void preInitialize(FMLPreInitializationEvent event)
    {
        logger.info("HorseStats: pre-Initializing");
        File file = new File(Minecraft.getMinecraft().mcDataDir, "/mods/"+MODNAME);
        if(!file.exists())
            file.mkdir();

//        data = new ConfigData(new File(file,"/config.txt").toString());
//        decimalFormat = GetDecimalFormat(data.getNumberOfDecimals());
    }

    @Mod.EventHandler
    public void initialize(FMLInitializationEvent event)
    {
        logger.info("HorseStats: Initializing");

        //Self registers with forge to receive proper events
        new KeyHandler();

        MinecraftForge.EVENT_BUS.register(this);
    }

    private boolean shouldRenderStats()
    {
        //TODO---This is a placeholder method and needs to use a config for some shit
        return false;
    }

    private int getMaxNumberOfOverlays()
    {
        //TODO---This is a placeholder method and needs to use a config for some shit
        return 0;
    }

    private int getRenderDistanceSquared()
    {
        //TODO---This is a placeholder method and needs to use a config for some shit
        return 0;
    }

    private Threshold getSpeedThreshold()
    {
        //TODO---This is a placeholder method and needs to use a config for some shit
        return null;
    }

    private Threshold getJumpThreshold()
    {
        //TODO---This is a placeholder method and needs to use a config for some shit
        return null;
    }

    private Threshold getHealthThreshold()
    {
        //TODO---This is a placeholder method and needs to use a config for some shit
        return null;
    }






    @SubscribeEvent
    public void RenderWorldLastEvent(RenderWorldLastEvent event)
    {
        if(mc.inGameHasFocus && shouldRenderStats())
        {
            for (int i = 0; i < mc.theWorld.loadedEntityList.size(); i++)
            {
                Object object = mc.theWorld.loadedEntityList.get(i);

                if(object == null || !(object instanceof EntityHorse))
                {
                    continue;
                }

                RenderHorseInfoInWorld((EntityHorse)object, event.getPartialTicks());
            }
        }
    }

    private void RenderHorseInfoInWorld(EntityHorse horse, float partialTickTime)
    {
        //if the player is in the world
        //and not looking at a menu
        //and F3 not pressed
        //if ((mc.inGameHasFocus || mc.currentScreen == null || mc.currentScreen instanceof GuiChat) && !mc.gameSettings.showDebugInfo)
        if ((mc.inGameHasFocus || mc.currentScreen == null || mc.currentScreen instanceof GuiChat))
        {
            if (horse.getRidingEntity() instanceof EntityPlayer)
                return;    //don't render stats of the horse/animal we are currently riding

            //only show entities that are close by
            double distanceFromMe = mc.thePlayer.getDistanceSqToEntity(horse);

            if (distanceFromMe > getRenderDistanceSquared())
                return;

            RenderHorseOverlay(horse, partialTickTime);
        }
    }

    protected void RenderHorseOverlay(EntityHorse animal, float partialTickTime)
    {
        float x = (float)animal.posX;
        float y = (float)animal.posY;
        float z = (float)animal.posZ;

        //a positive value means the horse has bred recently
        int animalGrowingAge = animal.getGrowingAge();

        EntityHorse horse = (EntityHorse)animal;

        String[] overlayText = new String[animalGrowingAge < 0 ? 4  : 3];

        overlayText[0] = (getSpeedThreshold().format(decimalFormat,Util.GetEntityMaxSpeed(horse)) +" m/s");
        overlayText[1] = (getHealthThreshold().format(decimalFormat,Util.GetEntityMaxHP(horse)) + " hp");
        overlayText[2] = (getJumpThreshold().format(decimalFormat,Util.GetHorseMaxJump(horse)) + " jump");

        if (animalGrowingAge < 0)
            overlayText[3] = (Util.GetHorseBabyGrowingAgeAsPercent(horse) + "%");

        RenderFloatingText(overlayText, x, y, z, 0xFFFFFF, true, partialTickTime);
    }

    private static final int TEXT_RENDER_DISTANCE = 20;

    private static final float MIN_TEXT_RENDER_SCALE = 0.0075f;
    private static final float MAX_TEXT_RENDER_SCALE = 0.04f;

    private static final float SCALE_STEP = (MAX_TEXT_RENDER_SCALE-MIN_TEXT_RENDER_SCALE)/TEXT_RENDER_DISTANCE;

    private static void RenderFloatingText(String[] text, float x, float y, float z, int color, boolean renderBlackBackground, float partialTickTime)
    {
        //Thanks to Electric-Expansion mod for the majority of this code
        //https://github.com/Alex-hawks/Electric-Expansion/blob/master/src/electricexpansion/client/render/RenderFloatingText.java

        RenderManager renderManager = mc.getRenderManager();

        float playerX = (float) (mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTickTime);
        float playerY = (float) (mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTickTime);
        float playerZ = (float) (mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTickTime);

        float dx = x-playerX;
        float dy = y-playerY;
        float dz = z-playerZ;
        float distance = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        float scale = MIN_TEXT_RENDER_SCALE + (distance*SCALE_STEP);//.01f; //Min font scale for max text render distance

        GL11.glColor4f(1f, 1f, 1f, 0.5f);
        GL11.glPushMatrix();
        GL11.glTranslatef(dx, dy, dz);
        GL11.glRotatef(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-scale, -scale, scale);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        int textWidth = 0;
        for (String thisMessage : text)
        {
            int thisMessageWidth = mc.fontRendererObj.getStringWidth(thisMessage);

            if (thisMessageWidth > textWidth)
                textWidth = thisMessageWidth;
        }

        int lineHeight = 10;

        if(renderBlackBackground)
        {
            int stringMiddle = textWidth / 2;

            Tessellator tessellator = Tessellator.getInstance();
            VertexBuffer worldrenderer = tessellator.getBuffer();

            GlStateManager.disableTexture2D();

            //This code taken from 1.8.8 net.minecraft.client.renderer.entity.Render.renderLivingLabel()
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
            worldrenderer.pos((double) (-stringMiddle - 1), (double) (-1), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldrenderer.pos((double) (-stringMiddle - 1), (double) (8 + lineHeight*(text.length-1)), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldrenderer.pos((double) (stringMiddle + 1), (double) (8 + lineHeight*(text.length-1)), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldrenderer.pos((double) (stringMiddle + 1), (double) (-1), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();

            tessellator.draw();

            GlStateManager.enableTexture2D();
        }

        int i = 0;
        for(String message : text)
        {
            int messageWidth = mc.fontRendererObj.getStringWidth(message);
            mc.fontRendererObj.drawString(message,  0-(messageWidth / 2), i*lineHeight, color);
            i++;
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }
}