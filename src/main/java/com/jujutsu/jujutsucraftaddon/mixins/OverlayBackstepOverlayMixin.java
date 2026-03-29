package com.jujutsu.jujutsucraftaddon.mixins;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mcreator.jujutsucraft.client.screens.OverlayBackstepOverlay;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OverlayBackstepOverlay.class, priority = -1000)
public abstract class OverlayBackstepOverlayMixin {

    @Inject(method = "eventHandler", at = @At("HEAD"), cancellable = true, remap = false)
    private static void eventHandler(RenderGuiEvent.Pre event, CallbackInfo ci) {
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
