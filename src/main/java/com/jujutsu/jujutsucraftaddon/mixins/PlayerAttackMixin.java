package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.JujutsucraftaddonMod;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.network.PacketEffects;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class PlayerAttackMixin {

    @Inject(method = "startAttack", at = @At("HEAD"))
    private void onPlayerAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player != null && mc.level != null) {
            ItemStack mainHand = player.getMainHandItem();
            String itemName = ForgeRegistries.ITEMS.getKey(mainHand.getItem()).toString();

            // 1. Majima Knife Logic
            if (itemName.contains("knife")) {
                player.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
                    if ("Majima".equals(addonVars.Clans) && Math.random() < 0.025) {
                        JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new PacketEffects(4, player.getUUID()));
                        JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new PacketEffects(0, player.getUUID()));
                    }
                });
            }

            // 2. Custom Combat Styles
            player.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
                if (addonVars.Run == 1) {
                    if (mainHand.isEmpty()) {
                        handleUnarmedStyles(player, addonVars);
                    } else {
                        handleWeaponStyles(player, addonVars, itemName);
                    }
                }
            });
        }
    }

    private void handleUnarmedStyles(AbstractClientPlayer player, JujutsucraftaddonModVariables.PlayerVariables addonVars) {
        int style = (int) addonVars.Style;
        int lastIndex = (int) addonVars.AttackAnimation;
        int nextIndex;
        String animationBase;
        int maxIndex;

        switch (style) {
            case 0 -> { animationBase = "basicstyle"; maxIndex = 4; }
            case 1 -> { animationBase = "gojostyle"; maxIndex = 5; }
            case 2 -> { animationBase = "yujistyle"; maxIndex = 6; }
            case 3 -> { animationBase = "kashimo"; maxIndex = 5; }
            case 4 -> { animationBase = "jogo"; maxIndex = 5; }
            case 5 -> { animationBase = "maki"; maxIndex = 5; }
            case 6 -> { animationBase = "sukuna"; maxIndex = 26; }
            case 7 -> { animationBase = "itadorifist"; maxIndex = 5; }
            default -> { return; }
        }

        nextIndex = (lastIndex >= maxIndex) ? 1 : lastIndex + 1;
        addonVars.AttackAnimation = nextIndex;
        addonVars.syncPlayerVariables(player);

        String animationName = animationBase + nextIndex;

        // Special Chance Overrides
        if (style == 1 && Math.random() < 0.05) {
            animationName = "gojostylebarrage";
            JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new PacketEffects(9, player.getUUID()));
        } else if (style == 2) {
            if (Math.random() < 0.05) {
                animationName = "yujistylecombo";
                JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new PacketEffects(9, player.getUUID()));
            } else if (Math.random() < 0.066) {
                animationName = "yujistylemanjikick";
                JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new PacketEffects(9, player.getUUID()));
            }
        } else if (style == 7 && Math.random() < 0.01) {
            animationName = "itadorifistkoku";
            JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new PacketEffects(1, player.getUUID()));
        } else if (style == 5) {
            if (Math.random() < (1.0 / 20.0)) {
                JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new PacketEffects(5, player.getUUID()));
            } else if (Math.random() < (1.0 / 15.0)) {
                JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new PacketEffects(6, player.getUUID()));
            }
        }

        playAddonAnimation(player, animationName);
        
        if (Math.random() < 0.2) {
            JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new PacketEffects(8, player.getUUID()));
        }
        JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new PacketEffects(0, player.getUUID()));
    }

    private void handleWeaponStyles(AbstractClientPlayer player, JujutsucraftaddonModVariables.PlayerVariables addonVars, String itemName) {
        String animationBase = "";
        int maxIndex = 5;
        int effectId = -1;

        if (itemName.contains("nyoi")) { animationBase = "nyoi"; maxIndex = 9; effectId = 2; }
        else if (itemName.contains("hiten")) { animationBase = "hiten"; maxIndex = 5; }
        else if (itemName.contains("sword_of_extermination")) { 
            animationBase = "exterminationsword"; 
            if (Math.random() < 0.02) {
                playAddonAnimation(player, "exterminationswordslice");
                JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new PacketEffects(4, player.getUUID()));
                return;
            }
        }
        else if (itemName.contains("inverted_spear_of_heaven")) {
            animationBase = "inverted";
            if (Math.random() < 0.02) {
                playAddonAnimation(player, "invertedcut");
                JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new PacketEffects(3, player.getUUID()));
                return;
            }
        }
        else if (itemName.contains("supreme_martial_solution")) { animationBase = "kamutoke"; maxIndex = 4; effectId = 2; }
        else if (itemName.contains("itadori_arm")) {
            if (Math.random() < 0.01 && addonVars.Style == 7) {
                playAddonAnimation(player, "itadorifistkoku");
                JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new PacketEffects(1, player.getUUID()));
                return;
            }
            animationBase = "itadorifist";
        }
        else if (itemName.contains("split")) { animationBase = "split"; }
        else if (itemName.contains("playful")) {
            if (!isAnimationActive(player)) {
                playAddonAnimation(player, "ab" + Mth.nextInt(RandomSource.create(), 1, 5) + "player");
            }
            JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new PacketEffects(0, player.getUUID()));
            return;
        }

        if (!animationBase.isEmpty()) {
            int nextIndex = (addonVars.AttackAnimation >= maxIndex) ? 1 : (int)addonVars.AttackAnimation + 1;
            addonVars.AttackAnimation = nextIndex;
            addonVars.syncPlayerVariables(player);
            
            playAddonAnimation(player, animationBase + nextIndex);
            if (effectId != -1 && Math.random() < 0.025) {
                JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new PacketEffects(effectId, player.getUUID()));
            }
            JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new PacketEffects(0, player.getUUID()));
        } else {
            // Default Sword Logic
            if (Math.random() < 0.025 && addonVars.Style == 5) {
                playAddonAnimation(player, "swordultimate");
                JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new PacketEffects(7, player.getUUID()));
            } else if (!isAnimationActive(player)) {
                playAddonAnimation(player, "sword" + Mth.nextInt(RandomSource.create(), 1, 24));
                JujutsucraftaddonMod.PACKET_HANDLER.sendToServer(new PacketEffects(0, player.getUUID()));
            }
        }
    }

    private void playAddonAnimation(AbstractClientPlayer player, String animName) {
        ModifierLayer<IAnimation> animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player)
                .get(new ResourceLocation("jujutsucraftaddon", "player_animation"));
        if (animation != null) {
            animation.setAnimation(new KeyframeAnimationPlayer(PlayerAnimationRegistry.getAnimation(new ResourceLocation("jujutsucraftaddon", animName))));
        }
    }

    private boolean isAnimationActive(AbstractClientPlayer player) {
        ModifierLayer<IAnimation> animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player)
                .get(new ResourceLocation("jujutsucraftaddon", "player_animation"));
        return animation != null && animation.isActive();
    }
}
