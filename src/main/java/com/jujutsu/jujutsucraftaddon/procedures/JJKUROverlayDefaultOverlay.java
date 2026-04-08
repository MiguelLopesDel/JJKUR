package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.util.JJKUClientConfig;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraftforge.fml.loading.FMLPaths;
import java.io.File;

public class JJKUROverlayDefaultOverlay {
    private static boolean warnedModernJujutsu = false;

    public static void onExecute(RenderGuiEvent.Pre event, CallbackInfo ci) {
        if (!warnedModernJujutsu && ModList.get().isLoaded("slyrienmodern")) {
            Player player = Minecraft.getInstance().player;
            if (player != null && JJKUClientConfig.SHOW_HUD.get()) {
                File configDir = FMLPaths.CONFIGDIR.get().toFile();
                String folderPath = configDir.getAbsolutePath();

                MutableComponent message = Component.literal("Modern Jujutsu detected! ")
                    .append(Component.literal("[Disable JJKU HUD]")
                        .withStyle(style -> style
                            .withColor(ChatFormatting.YELLOW)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, folderPath))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to try opening the config folder"))))
                    )
                    .append(Component.literal("  "))
                    .append(Component.literal("[Copy Path]")
                        .withStyle(style -> style
                            .withColor(ChatFormatting.AQUA)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, folderPath))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy folder path if opening fails"))))
                    )
                    .append(Component.literal(" (Edit 'jujutsucraftaddon-client.toml')"));

                player.sendSystemMessage(message);
                warnedModernJujutsu = true;
            }
        }

        if (!JJKUClientConfig.SHOW_HUD.get())
            return;

        ci.cancel();
        int w = event.getWindow().getGuiScaledWidth();
        int h = event.getWindow().getGuiScaledHeight();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        Player entity = Minecraft.getInstance().player;
        if (entity != null) {
            if (OverlayHaveTechniqueProcedure.execute(entity)) {
                // Restore Center-Relative Positions (Baseline Style)
                event.getGuiGraphics().blit(ResourceLocation.parse("jujutsucraft:textures/screens/icon_magic.png"), w / 2 + -200, h / 2 + 104, 0.0F, 0.0F, 16, 16, 16, 16);

                if (LogicCooldownProcedure.execute(entity)) {
                    event.getGuiGraphics().blit(ResourceLocation.parse("jujutsucraft:textures/screens/icon_cooldown.png"), w / 2 + -200, h / 2 + 104, 0.0F, 0.0F, 16, 16, 16, 16);
                }

                event.getGuiGraphics().blit(ResourceLocation.parse("jujutsucraft:textures/screens/icon_punch.png"), w / 2 + -214, h / 2 + 104, 0.0F, 0.0F, 16, 16, 16, 16);
                if (LogicCooldownCombatProcedure.execute(entity)) {
                    event.getGuiGraphics().blit(ResourceLocation.parse("jujutsucraft:textures/screens/icon_cooldown.png"), w / 2 + -213, h / 2 + 104, 0.0F, 0.0F, 16, 16, 16, 16);
                }

                // Skill Icons
                if (OverlayDomainSkillProcedure.execute(entity)) {
                    event.getGuiGraphics().blit(ResourceLocation.parse("jujutsucraft:textures/screens/icon_domain_expansion2.png"), w / 2 + -216, h / 2 + 82, 0.0F, 0.0F, 32, 32, 32, 32);
                }
                if (OverlayCursedSkillProcedure.execute(entity)) {
                    event.getGuiGraphics().blit(ResourceLocation.parse("jujutsucraft:textures/screens/icon_magic.png"), w / 2 + -215, h / 2 + 82, 0.0F, 0.0F, 32, 32, 32, 32);
                }
                if (OverlayDefaultSkillProcedure.execute(entity)) {
                    event.getGuiGraphics().blit(ResourceLocation.parse("jujutsucraft:textures/screens/icon_default.png"), w / 2 + -216, h / 2 + 82, 0.0F, 0.0F, 32, 32, 32, 32);
                }
                if (OverlayPhysicalSkillProcedure.execute(entity)) {
                    event.getGuiGraphics().blit(ResourceLocation.parse("jujutsucraft:textures/screens/icon_punch.png"), w / 2 + -216, h / 2 + 82, 0.0F, 0.0F, 32, 32, 32, 32);
                }
                if (OverlayPassiveProcedure.execute(entity)) {
                    event.getGuiGraphics().blit(ResourceLocation.parse("jujutsucraft:textures/screens/icon_passive.png"), w / 2 + -216, h / 2 + 82, 0.0F, 0.0F, 32, 32, 32, 32);
                }
                if (OverlayCooldown2Procedure.execute(entity)) {
                    event.getGuiGraphics().blit(ResourceLocation.parse("jujutsucraft:textures/screens/icon_cooldown.png"), w / 2 + -216, h / 2 + 82, 0.0F, 0.0F, 32, 32, 32, 32);
                }

                // Text Information
                event.getGuiGraphics().drawString(Minecraft.getInstance().font, OCursePowerProcedure.execute(entity), w / 2 + -180, h / 2 + 86, -205, false);
                event.getGuiGraphics().drawString(Minecraft.getInstance().font, OTechniqueNameProcedure.execute(entity), w / 2 + -180, h / 2 + 95, -205, false);
                event.getGuiGraphics().drawString(Minecraft.getInstance().font, OCostProcedure.execute(entity), w / 2 + -180, h / 2 + 104, -205, false);

                if (OverlayCooldown2TimerProcedure.execute(entity)) {
                    event.getGuiGraphics().drawString(Minecraft.getInstance().font, OCoolTimeSelectingProcedure.execute(entity), w / 2 + -203, h / 2 + 95, -205, false);
                }
                if (LogicCooldownCombatProcedure.execute(entity)) {
                    event.getGuiGraphics().drawString(Minecraft.getInstance().font, OCoolTimeCombatProcedure.execute(entity), w / 2 + -208, h / 2 + 107, -205, false);
                }
                if (LogicCooldownMagicOnlyProcedure.execute(entity)) {
                    event.getGuiGraphics().drawString(Minecraft.getInstance().font, OCoolTimeProcedure.execute(entity), w / 2 + -194, h / 2 + 107, -205, false);
                }

                // v43 Keys Display (Adapted to Center-Relative)
                event.getGuiGraphics().drawString(Minecraft.getInstance().font, OUseProcedure.execute(entity), w / 2 + -128, h / 2 + 117, -6710887, false);
                if (LogicDoubleCursedTechniqueProcedure.execute(entity)) {
                    event.getGuiGraphics().drawString(Minecraft.getInstance().font, OSwitchProcedure.execute(entity), w / 2 + -182, h / 2 + 117, -6736897, false);
                }
                event.getGuiGraphics().drawString(Minecraft.getInstance().font, OChangeProcedure.execute(entity), w / 2 + -128, h / 2 + 108, -6710887, false);
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
