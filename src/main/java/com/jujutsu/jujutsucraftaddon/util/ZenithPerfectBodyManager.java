package com.jujutsu.jujutsucraftaddon.util;

import com.jujutsu.jujutsucraftaddon.JujutsucraftaddonModNetworkHandler;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.procedures.CheckPerfectZenithConditionProcedure;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;


@Mod.EventBusSubscriber
public class ZenithPerfectBodyManager {
    
    private static final ResourceLocation ADV_PERFECT_BODY = ResourceLocation.tryBuild("jujutsucraftaddon", "perfect_zenith_body");

    private static final UUID HP_MODIFIER_ID = UUID.fromString("f4e5d6c7-b8a9-0123-4567-89abcdef0123");
    private static final UUID DEF_MODIFIER_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
            CheckPerfectZenithConditionProcedure.execute(event.player);
            processPerfectBody((ServerPlayer) event.player);
        }
    }

    public static void processPerfectBody(ServerPlayer player) {
        player.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
            boolean active = isAdvDone(player, ADV_PERFECT_BODY);

            if (vars.zenith_perfect_body != active) {
                vars.zenith_perfect_body = active;
                vars.syncPlayerVariablesToAll(player);
            }

            if (active) {
                applyStats(player);
                boostChants(player);
            } else {
                removeStats(player);
            }
        });
    }

    public static boolean hasAllRequirements(Entity entity) {
        if (entity == null) return false;
        if (entity.level().isClientSide()) {
            return entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .map(v -> v.zenith_perfect_body).orElse(false);
        }
        if (entity instanceof ServerPlayer player) {
            return isAdvDone(player, ADV_PERFECT_BODY);
        }
        return false;
    }

    private static boolean isAdvDone(ServerPlayer player, ResourceLocation id) {
        if (player.server == null) return false;
        Advancement adv = player.server.getAdvancements().getAdvancement(id);
        if (adv == null) return false;
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
        return progress.isDone();
    }

    private static void applyStats(LivingEntity entity) {
        AttributeInstance hp = entity.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && hp.getModifier(HP_MODIFIER_ID) == null) {
            hp.addTransientModifier(new AttributeModifier(HP_MODIFIER_ID, "Zenith HP Bonus", 200, AttributeModifier.Operation.ADDITION));
        }

        AttributeInstance def = entity.getAttribute(Attributes.ARMOR);
        if (def != null && def.getModifier(DEF_MODIFIER_ID) == null) {
            def.addTransientModifier(new AttributeModifier(DEF_MODIFIER_ID, "Zenith Defense Bonus", 20, AttributeModifier.Operation.ADDITION));
        }
    }

    private static void removeStats(LivingEntity entity) {
        AttributeInstance hp = entity.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.removeModifier(HP_MODIFIER_ID);
        
        AttributeInstance def = entity.getAttribute(Attributes.ARMOR);
        if (def != null) def.removeModifier(DEF_MODIFIER_ID);
    }

    private static void boostChants(Entity entity) {
        double cnt6 = entity.getPersistentData().getDouble("cnt6");
        if (cnt6 > 0 && cnt6 < 50) {
            entity.getPersistentData().putDouble("cnt6", cnt6 + 1.0);
        }
    }
}
