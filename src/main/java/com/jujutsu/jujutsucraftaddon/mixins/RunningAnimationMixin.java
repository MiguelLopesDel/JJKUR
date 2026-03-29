package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.PlayerAnimHandler;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class RunningAnimationMixin<T extends LivingEntity> extends HumanoidModel<T> {

    public RunningAnimationMixin(ModelPart root) {
        super(root);
    }

    /**
     * @author Satushi
     * @reason Pre-setup position reset for custom running loop
     */
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    public void onSetupAnimPre(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity.isAlive()) {
            PlayerAnimHandler.preSprintingAnim(entity, (PlayerModel<T>) (Object) this);
        }
    }

    /**
     * @author Satushi
     * @reason Apply custom sprinting animations after standard humanoid setup
     */
    @Inject(
        method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/HumanoidModel;setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", shift = At.Shift.AFTER)
    )
    public void onSetupAnimPost(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity.isAlive() && (limbSwing != 0.0F || ageInTicks != 0.0F || netHeadYaw != 0.0F || headPitch != 0.0F)) {
            PlayerAnimHandler.sprintingAnim(entity, (PlayerModel<T>) (Object) this);
            this.hat.copyFrom(this.head);
        }
    }
}
