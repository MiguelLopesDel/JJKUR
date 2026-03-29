package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.procedures.ConcorrentSpawnProcedure;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.ReturnConfigDoVanillaMobSpawningProcedure;
import net.mcreator.jujutsucraft.procedures.WhenPlayerJointheWorldProcedure;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WhenPlayerJointheWorldProcedure.class, priority = -10000)
public abstract class WhenPlayerJointheWorldProcedureMixin {

    /**
     * @author Satushi / Rigorous Restoration
     * @reason RESTORED: Total override behavior to ensure JJKUR custom initialization (ConcorrentSpawn).
     * FIXED: Manually integrated v43 Map Variable synchronization to prevent server configuration loss.
     */
    @Inject(at = @At("HEAD"), method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V", remap = false, cancellable = true)
    private static void execute(Event event, LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        // 1. TOTAL OVERRIDE: Skip base mod join logic to prevent double initialization
        ci.cancel();

        if (entity == null) return;

        // 2. v43 CRITICAL SYNC: Manually perform the map variable sync that was inside the base procedure
        if (world instanceof ServerLevel) {
            JujutsucraftModVariables.MapVariables mapVars = JujutsucraftModVariables.MapVariables.get(world);
            mapVars.config_doVanillaMobSpawning = ReturnConfigDoVanillaMobSpawningProcedure.execute();
            mapVars.syncData(world);
        }

        // 3. JJKUR INITIALIZATION: Run concurrent spawn for Players and Addon Mobs
        if (entity.isAlive()) {
            ResourceLocation entityTypeKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            // Players (minecraft) and non-base-mod entities use the addon initialization
            if (entityTypeKey != null && !entityTypeKey.getNamespace().equals("jujutsucraft")) {
                ConcorrentSpawnProcedure.execute(entity, world, x, y, z);
            }
        }
    }
}
