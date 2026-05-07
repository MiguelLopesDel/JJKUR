package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.procedures.OpSukunaBrain;
import com.jujutsu.jujutsucraftaddon.procedures.opsukuna.OpSukunaBrainMemory;
import com.jujutsu.jujutsucraftaddon.util.OpSukunaBrainMemoryHolder;
import net.mcreator.jujutsucraft.entity.SukunaFushiguroEntity;
import net.mcreator.jujutsucraft.entity.SukunaPerfectEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntity.class, priority = -10000)
public abstract class OpSukunaBrainMemoryMixin implements OpSukunaBrainMemoryHolder {
    @Unique
    private OpSukunaBrainMemory jjkur$opSukunaBrainMemory;

    @Override
    public OpSukunaBrainMemory jjkur$getOpSukunaBrainMemory() {
        if (jjkur$opSukunaBrainMemory == null) {
            jjkur$opSukunaBrainMemory = new OpSukunaBrainMemory();
        }
        return jjkur$opSukunaBrainMemory;
    }

    @Override
    public void jjkur$setOpSukunaBrainMemory(OpSukunaBrainMemory memory) {
        jjkur$opSukunaBrainMemory = memory == null ? new OpSukunaBrainMemory() : memory;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void jjkur$saveOpSukunaBrain(CompoundTag tag, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof LivingEntity sukuna && jjkur$isOpSukuna(sukuna)) {
            CompoundTag memory = OpSukunaBrain.save(sukuna);
            if (!memory.isEmpty()) {
                tag.put(OpSukunaBrain.saveKey(), memory);
            }
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void jjkur$loadOpSukunaBrain(CompoundTag tag, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof LivingEntity sukuna && jjkur$isOpSukuna(sukuna) && tag.contains(OpSukunaBrain.saveKey())) {
            OpSukunaBrain.load(sukuna, tag.getCompound(OpSukunaBrain.saveKey()));
        }
    }

    @Unique
    private static boolean jjkur$isOpSukuna(LivingEntity entity) {
        return entity instanceof SukunaPerfectEntity
                || entity instanceof SukunaFushiguroEntity;
    }
}
