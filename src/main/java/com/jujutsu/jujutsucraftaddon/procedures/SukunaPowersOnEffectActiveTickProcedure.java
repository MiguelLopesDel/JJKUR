package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class SukunaPowersOnEffectActiveTickProcedure {
    public static void execute(LevelAccessor world, Entity entity) {
        if (entity == null)
            return;

        if (entity.getPersistentData().getBoolean("FlagSukuna")) {
            entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
                if (addonVars.InfusedDomain) {
                    if (entity instanceof ServerPlayer _plr1 && _plr1.level() instanceof ServerLevel) {
                        var advancement = _plr1.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraftaddon:cleave_web_advancement"));
                        if (advancement != null && _plr1.getAdvancements().getOrStartProgress(advancement).isDone()) {
                            if (entity.isShiftKeyDown()) {
                                entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                                    if (capability.PlayerSelectCurseTechniqueName.equals(Component.translatable("jujutsu.technique.cleave").getString())) {
                                        capability.PlayerSelectCurseTechniqueName = Component.translatable("dialoguecw").getString();
                                        capability.syncPlayerVariables(entity);
                                    }
                                });
                            }
                        }
                    }
                }
            });
        }
    }
}
