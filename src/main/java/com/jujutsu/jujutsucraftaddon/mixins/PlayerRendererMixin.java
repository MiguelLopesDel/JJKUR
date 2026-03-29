package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.JujutsucraftaddonMod;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = PlayerRenderer.class, priority = -10000)
public abstract class PlayerRendererMixin {

    /**
     * @author Satushi
     * @reason Intercepts and modifies the player's name tag for Kenjaku identity changes with debug logging
     */
    @ModifyVariable(
            method = "renderNameTag(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private Component modifyNameTag(Component component, AbstractClientPlayer abstractClientPlayer) {
        if (!abstractClientPlayer.isAlive()) return component;

        // Use a wrapper to allow modification inside lambda
        final Component[] result = {component};

        abstractClientPlayer.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
            if ("Kenjaku".equals(capability.Clans)) {
                String tag = capability.tag1;
                String skinName = null;

                if (tag != null) {
                    skinName = switch (tag) {
                        case "One" -> capability.SkinName1;
                        case "Two" -> capability.SkinName2;
                        case "Three" -> capability.SkinName3;
                        default -> null;
                    };
                }

                if (skinName != null && !skinName.isEmpty()) {
                    result[0] = Component.literal(skinName);
                    // DEBUG LOG: Only log once or under specific conditions to avoid spam
                    if (abstractClientPlayer.tickCount % 100 == 0) {
                        JujutsucraftaddonMod.LOGGER.info("Renderer Sync: Displaying identity [" + skinName + "] for Kenjaku player [" + abstractClientPlayer.getName().getString() + "]");
                    }
                }
            }
        });

        return result[0];
    }
}
