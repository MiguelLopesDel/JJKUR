package com.jujutsu.jujutsucraftaddon.util;

import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class ZenithCooldownSystem {

    public static void tickCooldowns(LivingEntity entity) {
        entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
            CompoundTag nbt = entity.getPersistentData();

            if (!addonVars.zenith_perfect_body) {
                clearBank(nbt);
                nbt.putInt("z_cd_state", 0);
                return;
            }

            MobEffect magicCD = JujutsucraftModMobEffects.COOLDOWN_TIME.get();
            MobEffect combatCD = JujutsucraftModMobEffects.COOLDOWN_TIME_COMBAT.get();

            boolean hasMagic = entity.hasEffect(magicCD);
            boolean hasCombat = entity.hasEffect(combatCD);
            boolean hasAnyCD = hasMagic || hasCombat;

            int state = nbt.getInt("z_cd_state");

            if (state == 1) {
                double storedCD = nbt.getDouble("z_cd_stored");
                if (storedCD > 0) {
                    nbt.putDouble("z_cd_stored", storedCD - 1);
                } else {
                    clearBank(nbt);
                    nbt.putInt("z_cd_state", 0);
                    state = 0;
                }
            }

            if (state == 2 && !hasAnyCD) {
                nbt.putInt("z_cd_state", 0);
                state = 0;
            }

            int finalState = state;
            entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(pVars -> {
                String currentSkill = pVars.PlayerSelectCurseTechniqueName;
                if (currentSkill == null) currentSkill = "";

                String lastHeldSkill = nbt.getString("z_cd_last_held");

                if (lastHeldSkill.isEmpty() && !currentSkill.isEmpty()) {
                    nbt.putString("z_cd_last_held", currentSkill);
                    return;
                }


                if (finalState == 0 && hasAnyCD && !currentSkill.equals(lastHeldSkill)) {
                    MobEffect activeType = hasCombat ? combatCD : magicCD;
                    MobEffectInstance activeEffect = entity.getEffect(activeType);

                    nbt.putString("z_cd_skill_a", lastHeldSkill);
                    nbt.putDouble("z_cd_stored", activeEffect.getDuration());
                    nbt.putInt("z_cd_amp", activeEffect.getAmplifier());
                    nbt.putBoolean("z_cd_is_combat", hasCombat);
                    nbt.putInt("z_cd_state", 1);

                    entity.removeEffect(activeType);
                }

                else if (finalState == 1) {
                    String skillA = nbt.getString("z_cd_skill_a");

                    if (currentSkill.equals(skillA)) {
                        restoreMaxCooldown(entity, nbt, magicCD, combatCD, hasMagic, hasCombat);
                        clearBank(nbt);
                        nbt.putInt("z_cd_state", 2);
                    }

                    else if (hasAnyCD && !nbt.getBoolean("PRESS_Z")) {
                        restoreMaxCooldown(entity, nbt, magicCD, combatCD, hasMagic, hasCombat);
                        clearBank(nbt);
                        nbt.putInt("z_cd_state", 2);
                    }
                }

                nbt.putString("z_cd_last_held", currentSkill);
            });
        });
    }

    private static void restoreMaxCooldown(LivingEntity entity, CompoundTag nbt, MobEffect magicCD, MobEffect combatCD, boolean hasMagic, boolean hasCombat) {
        double storedCD = nbt.getDouble("z_cd_stored");
        int storedAmp = nbt.getInt("z_cd_amp");
        boolean storedIsCombat = nbt.getBoolean("z_cd_is_combat");

        MobEffect currentActiveType = hasCombat ? combatCD : magicCD;
        MobEffectInstance currentEffect = entity.getEffect(currentActiveType);
        double currentCD = currentEffect != null ? currentEffect.getDuration() : 0;

        double finalCD = Math.max(storedCD, currentCD);

        if (finalCD > 0) {
            MobEffect finalType = (storedCD > currentCD) ? (storedIsCombat ? combatCD : magicCD) : currentActiveType;
            int finalAmp = (storedCD > currentCD) ? storedAmp : (currentEffect != null ? currentEffect.getAmplifier() : 0);

            entity.removeEffect(magicCD);
            entity.removeEffect(combatCD);

            entity.addEffect(new MobEffectInstance(finalType, (int) finalCD, finalAmp, false, false));
            nbt.putDouble("COOLDOWN_TICKS", finalCD);
        }
    }

    private static void clearBank(CompoundTag nbt) {
        nbt.remove("z_cd_skill_a");
        nbt.remove("z_cd_stored");
        nbt.remove("z_cd_amp");
        nbt.remove("z_cd_is_combat");
    }
}