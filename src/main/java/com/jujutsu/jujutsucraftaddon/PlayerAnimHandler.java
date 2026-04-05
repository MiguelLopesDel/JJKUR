package com.jujutsu.jujutsucraftaddon;

import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class PlayerAnimHandler {

    /**
     * @author Satushi / Audit Correction
     * @reason Resets all model transformations to prevent "stuck" animations after sprinting stops.
     * This method is called at HEAD of setupAnim.
     */
    public static void preSprintingAnim(Entity entity, PlayerModel<?> playerModel) {
        playerModel.head.setPos(0.0F, 0.0F, 0.0F);
        playerModel.body.setPos(0.0F, 0.0F, 0.0F);
        playerModel.rightArm.setPos(-5.0F, 2.0F, 0.0F);
        playerModel.leftArm.setPos(5.0F, 2.0F, 0.0F);

        playerModel.body.xRot = 0.0F;
        playerModel.body.yRot = 0.0F;
        playerModel.body.zRot = 0.0F;
    }

    /**
     * @author Satushi / Audit Correction
     * @reason Applies dynamic Ninja Run animation with dual-hand item detection.
     * This method is called at POST of setupAnim.
     */
    public static void sprintingAnim(Entity entity, PlayerModel<?> playerModel) {
        if (entity instanceof LivingEntity living) {
            living.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(addonVars -> {
                if (addonVars.Run == 1.0 && living.isSprinting() && !living.isVisuallySwimming() && !living.isCrouching()) {
                    float animationProgress = (float) (living.tickCount % 120) / 40.0F;
                    boolean isHoldingItem = !living.getMainHandItem().isEmpty() || !living.getOffhandItem().isEmpty();

                    float armRotationY = (float) Math.sin(animationProgress * Math.PI * 2) * 0.25F;
                    float armRotationX = 1.2F;

                    if (isHoldingItem) {
                        armRotationY = (float) Math.sin(animationProgress * Math.PI * 2) * 0.35F;
                        armRotationX = 1.0F;
                    }

                    playerModel.rightArm.xRot = armRotationX;
                    playerModel.rightArm.yRot = armRotationY;
                    playerModel.rightArm.zRot = 0.0F;

                    playerModel.leftArm.xRot = armRotationX;
                    playerModel.leftArm.yRot = -armRotationY;
                    playerModel.leftArm.zRot = 0.0F;

                    playerModel.rightArm.setPos(-5.0F, 3.5F, -5.0F);
                    playerModel.leftArm.setPos(5.0F, 3.5F, -5.0F);

                    playerModel.head.setPos(0.0F, 1.13F, -6.0F);

                    playerModel.body.xRot = 0.5F;
                    playerModel.body.setPos(0.0F, 2.0F, -5.5F);
                }
            });
        }
    }
}
