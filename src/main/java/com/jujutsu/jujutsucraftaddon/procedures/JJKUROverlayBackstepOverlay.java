package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.util.JJKUClientConfig;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraftforge.fml.loading.FMLPaths;
import java.io.File;

public class JJKUROverlayBackstepOverlay {
    private static boolean warnedModernJujutsu = false;

    public static void execute(RenderGuiEvent.Pre event, CallbackInfo ci) {
        if (!warnedModernJujutsu && ModList.get().isLoaded("slyrienmodern")) {
            Player player = Minecraft.getInstance().player;
            if (player != null && JJKUClientConfig.SHOW_BACKSTEP_HUD.get()) {
                File configDir = FMLPaths.CONFIGDIR.get().toFile();
                String folderPath = configDir.getAbsolutePath();

                MutableComponent message = Component.literal("Modern Jujutsu detected! ")
                    .append(Component.literal("[Disable JJKU Backstep HUD]")
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

        if (!JJKUClientConfig.SHOW_BACKSTEP_HUD.get())
            return;

        ci.cancel();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        Player entity = Minecraft.getInstance().player;
        if (entity != null) {
            int w = event.getWindow().getGuiScaledWidth();
            int h = event.getWindow().getGuiScaledHeight();

            if (OverlayHaveTechniqueProcedure.execute(entity)) {
                // v43 New Guard Logic
                if (LogicCantJustGuardProcedure.execute(entity)) {
                    event.getGuiGraphics().blit(new ResourceLocation("jujutsucraft:textures/screens/icon_guard_cooldown.png"), w / 2 - 8, h - 53, 0.0F, 0.0F, 16, 16, 16, 16);
                }

                if (LogicGuardProcedure.execute(entity)) {
                    event.getGuiGraphics().blit(new ResourceLocation("jujutsucraft:textures/screens/guard.png"), w / 2 - 8, h - 53, 0.0F, 0.0F, 16, 16, 16, 16);
                }

                if (LogicJustGuardProcedure.execute(entity)) {
                    event.getGuiGraphics().blit(new ResourceLocation("jujutsucraft:textures/screens/guard_just.png"), w / 2 - 16, h - 61, 0.0F, 0.0F, 32, 32, 32, 32);
                }

                // Standard Movement Overlay
                event.getGuiGraphics().blit(new ResourceLocation("jujutsucraft:textures/screens/icon_backstep.png"), w / 2 + 6, h - 53, 0.0F, 0.0F, 16, 16, 16, 16);

                if (ODoubleJumpDispProcedure.execute(entity)) {
                    event.getGuiGraphics().blit(new ResourceLocation("jujutsucraft:textures/screens/icon_double_jump.png"), w / 2 + 6, h - 62, 0.0F, 0.0F, 16, 16, 16, 16);
                }

                if (OFlyDispProcedure.execute(entity)) {
                    event.getGuiGraphics().blit(new ResourceLocation("jujutsucraft:textures/screens/icon_fly.png"), w / 2 + 6, h - 71, 0.0F, 0.0F, 16, 16, 16, 16);
                }

                // Text Rendering
                event.getGuiGraphics().drawString(Minecraft.getInstance().font, OBackstepProcedure.execute(entity), w / 2 + 19, h - 48, -1, false);

                if (ODoubleJumpDispProcedure.execute(entity)) {
                    event.getGuiGraphics().drawString(Minecraft.getInstance().font, ODoubleJumpProcedure.execute(entity), w / 2 + 19, h - 57, -1, false);
                }

                if (OFlyDispProcedure.execute(entity)) {
                    event.getGuiGraphics().drawString(Minecraft.getInstance().font, OFlyProcedure.execute(entity), w / 2 + 19, h - 66, -1, false);
                }
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
