package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.procedures.DismantleCutNerfed;
import net.mcreator.jujutsucraft.entity.SukunaFushiguroEntity;
import net.mcreator.jujutsucraft.entity.SukunaPerfectEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.item.HitenItem;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class HitenItemMixin {

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"))
    private void onSwing(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        ItemStack mainHandItem = entity.getMainHandItem();

        if (mainHandItem.getItem() instanceof HitenItem) {
            boolean isSukuna = false;
            JujutsucraftaddonModVariables.PlayerVariables addonVars = entity.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftaddonModVariables.PlayerVariables());
            
            if ("Sukuna".equals(addonVars.Clans)) {
                JujutsucraftModVariables.PlayerVariables baseVars = entity.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables());
                if (baseVars.PlayerCurseTechnique2 == 1.0) {
                    isSukuna = true;
                }
            }

            if (entity instanceof SukunaPerfectEntity || entity instanceof SukunaFushiguroEntity || isSukuna) {
                if (Math.random() < (1.0 / 40.0)) {
                    if (!entity.hasEffect(JujutsucraftModMobEffects.COOLDOWN_TIME.get())) {
                        DismantleCutNerfed.execute(entity.level(), entity);
                    }
                }
            }
        }
    }
}
