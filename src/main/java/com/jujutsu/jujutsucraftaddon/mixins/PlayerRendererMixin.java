package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.client.renderer.SukunaMarksLayer;
import com.jujutsu.jujutsucraftaddon.client.renderer.ZenithFourArmLayer;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerRenderer.class, priority = -10000)
public abstract class PlayerRendererMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void addSukunaLayers(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        PlayerRenderer self = (PlayerRenderer) (Object) this;
        self.addLayer(new SukunaMarksLayer(self));
        self.addLayer(new ZenithFourArmLayer(self));
    }

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
                }
            }
        });

        return result[0];
    }
}
