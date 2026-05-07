package com.jujutsu.jujutsucraftaddon.procedures;

import com.jujutsu.jujutsucraftaddon.procedures.opsukuna.OpSukunaEngine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;

public final class OpSukunaBrain {
    private OpSukunaBrain() {
    }

    public static boolean tryExecute(LevelAccessor world, double x, double y, double z, LivingEntity sukuna, LivingEntity target, CompoundTag nbt) {
        return OpSukunaEngine.tryExecute(world, x, y, z, sukuna, target, nbt);
    }

    public static CompoundTag save(LivingEntity sukuna) {
        return OpSukunaEngine.save(sukuna);
    }

    public static void load(LivingEntity sukuna, CompoundTag tag) {
        OpSukunaEngine.load(sukuna, tag);
    }

    public static String saveKey() {
        return OpSukunaEngine.saveKey();
    }
}
