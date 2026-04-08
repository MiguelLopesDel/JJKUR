package com.jujutsu.jujutsucraftaddon.util;

import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ZenithHWBManager {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
            processHWB(event.player);
        }
    }

    private static void processHWB(Player player) {
        player.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
            if (addonVars.hwb_active) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects.HWB.get(), 5, 0, false, false, true));


                player.getCapability(net.mcreator.jujutsucraft.network.JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(baseVars -> {
                    double cost = 10.0;

                    if (baseVars.PlayerCursePower >= cost) {
                        baseVars.PlayerCursePower -= cost;
                        baseVars.syncPlayerVariables(player);
                    } else {
                        addonVars.hwb_active = false;
                        addonVars.syncPlayerVariables(player);
                        player.displayClientMessage(Component.literal("§cEnergy depleted! HWB deactivated."), true);
                    }
                });
            }
        });
    }

    public static void toggleHWB(Player player) {
        player.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
            vars.hwb_active = !vars.hwb_active;
            vars.syncPlayerVariables(player);

            String status = vars.hwb_active ? "§aActivated" : "§cDeactivated";
            player.displayClientMessage(Component.literal("Hollow Wicker Basket: " + status), true);
        });
    }
}
