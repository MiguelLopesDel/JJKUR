package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.JujutsucraftaddonMod;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.mcreator.jujutsucraft.procedures.SetupAnimationsProcedure;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SetupAnimationsProcedure.class, remap = false)
public abstract class JujutsucraftModAnimationMessageMixin {

    /**
     * @author Satushi
     * @reason Consolidated Mixin to handle animation loading for both base and addon namespaces with debug logging
     */
    @Inject(method = "setAnimationClientside", at = @At("HEAD"), cancellable = true)
    private static void onSetAnimationClientside(Player player, String anim, boolean override, CallbackInfo ci) {
        if (player instanceof AbstractClientPlayer player_) {
            ModifierLayer<IAnimation> animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player_)
                    .get(ResourceLocation.fromNamespaceAndPath("jujutsucraft", "player_animation"));
            
            if (animation == null) {
                JujutsucraftaddonMod.LOGGER.error("Animation Layer not found for player: " + player.getName().getString());
                return;
            }

            if (override || !animation.isActive()) {
                // 1. Try Base Namespace (jujutsucraft)
                ResourceLocation animBase = ResourceLocation.fromNamespaceAndPath("jujutsucraft", anim);
                KeyframeAnimation retrieved = PlayerAnimationRegistry.getAnimation(animBase);
                String usedNamespace = "jujutsucraft";

                // 2. If not found, try Addon Namespace (jujutsucraftaddon)
                if (retrieved == null) {
                    ResourceLocation animAddon = ResourceLocation.fromNamespaceAndPath("jujutsucraftaddon", anim);
                    retrieved = PlayerAnimationRegistry.getAnimation(animAddon);
                    usedNamespace = "jujutsucraftaddon";
                }

                if (retrieved != null) {
                    JujutsucraftaddonMod.LOGGER.info("Animation Sync: Playing [" + anim + "] from namespace [" + usedNamespace + "] for player [" + player.getName().getString() + "]");
                    animation.setAnimation(new KeyframeAnimationPlayer(retrieved));
                    ci.cancel(); // Prevent base mod from attempting to load it again
                } else {
                    JujutsucraftaddonMod.LOGGER.error("Animation Sync Error: Failed to find animation [" + anim + "] in BOTH namespaces!");
                }
            }
        }
    }
}
