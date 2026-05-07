package com.jujutsu.jujutsucraftaddon.procedures.opsukuna;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

final class OpSukunaMovementController {
    private OpSukunaMovementController() {
    }

static void moveTactically(LivingEntity sukuna, OpSukunaSnapshot s, OpSukunaMovement movement) {
    Vec3 self = sukuna.position();
    Vec3 target = s.target.position();
    Vec3 toward = OpSukunaEngineCore.horizontal(target.subtract(self));
    if (toward.lengthSqr() < 1.0E-4) {
        toward = new Vec3(1.0, 0.0, 0.0);
    }
    Vec3 away = toward.scale(-1.0);
    Vec3 lateral = new Vec3(-toward.z, 0.0, toward.x).normalize().scale(s.strafeSide);
    Vec3 destination;
    double speed = 1.95;
    double impulse = 0.44;

    if (OpSukunaEngineCore.isVoidDomainMovementLocked(s)) {
        if (sukuna instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        Vec3 current = sukuna.getDeltaMovement();
        sukuna.setDeltaMovement(0.0, current.y, 0.0);
        return;
    }

    if (movement == OpSukunaMovement.PROJECTILE_DODGE) {
        destination = self.add(s.projectileDodgeVector.scale(8.0)).add(toward.scale(1.5));
        speed = 1.75;
        impulse = 0.54;
        s.memory.lastProjectileDodgeTick = s.tick;
    } else if (movement == OpSukunaMovement.PURPLE_EVADE) {
        Vec3 purple = s.purpleDodgeVector.lengthSqr() > 1.0E-4 ? s.purpleDodgeVector : lateral;
        Vec3 cover = s.clearShot ? away.scale(2.5) : Vec3.ZERO;
        destination = self.add(purple.scale(10.0 + s.purpleRisk * 5.0)).add(cover);
        if (s.sukuna.onGround() && s.purpleRisk > 0.72 && OpSukunaEngineCore.isWalkableDestination(s.world, s.sukuna, self.add(0.0, 1.15, 0.0))) {
            destination = destination.add(0.0, 1.15, 0.0);
            s.sukuna.setDeltaMovement(s.sukuna.getDeltaMovement().add(0.0, 0.28, 0.0));
        }
        speed = 2.05;
        impulse = 0.58;
        s.memory.lastPurpleEvadeTick = s.tick;
    } else if (movement == OpSukunaMovement.DOMAIN_ESCAPE) {
        Vec3 exit = s.domainEscapeVector.lengthSqr() > 1.0E-4 ? s.domainEscapeVector : away;
        destination = self.add(exit.scale(s.catastrophicDomain ? 12.0 : 8.0)).add(lateral.scale(2.5));
        speed = 1.9;
        impulse = 0.5;
    } else if (movement == OpSukunaMovement.MAINTAIN_RANGE) {
        if (s.itadoriModulo && (s.pathBlocked || s.distance > 16.0)) {
            destination = self.add(lateral.scale(4.5)).add(toward.scale(s.distance > 13.0 ? 3.0 : 0.9));
            speed = 1.75;
            impulse = 0.44;
        } else {
        double error = s.distance - s.idealDistance;
        Vec3 group = s.groupEscapeVector.scale(s.groupPressure * 5.0);
        Vec3 adjust = error < 0.0 ? away.scale(Math.min(8.0, -error + 2.0)) : toward.scale(Math.min(8.0, error + 2.0));
        destination = self.add(adjust).add(lateral.scale(3.5)).add(group);
        speed = 1.65;
        impulse = 0.42;
        }
    } else if (movement == OpSukunaMovement.CUT_OFF_ESCAPE) {
        Vec3 predicted = s.target.position().add(s.target.getDeltaMovement().scale(14.0));
        destination = predicted.add(lateral.scale(-2.0)).add(toward.scale(1.5));
        speed = 1.8;
        impulse = 0.52;
    } else if (movement == OpSukunaMovement.BAIT_WHIFF) {
        double desired = s.itadoriModulo ? 11.5 : 9.5;
        Vec3 adjust = s.distance < desired ? away.scale(desired - s.distance + 2.5) : toward.scale(Math.min(3.0, s.distance - desired));
        destination = self.add(adjust).add(lateral.scale(s.itadoriModulo ? 3.5 : 5.5));
        speed = 1.82;
        impulse = 0.54;
    } else {
        double inward = s.distance > s.idealDistance + 3.0 ? 2.5 : 0.6;
        destination = self.add(lateral.scale(s.itadoriModulo ? 4.0 : 6.0)).add(toward.scale(inward)).add(s.groupEscapeVector.scale(s.groupPressure * 3.0));
        speed = 1.75;
        impulse = 0.5;
    }

    destination = refineDestination(sukuna, s, destination, toward, lateral, away);
    faceTarget(sukuna, s.target);
    moveTo(sukuna, destination, speed, impulse);
    s.memory.lastMoveTick = s.tick;
    s.memory.rememberDestination(destination);
}

static Vec3 refineDestination(LivingEntity sukuna, OpSukunaSnapshot s, Vec3 destination, Vec3 toward, Vec3 lateral, Vec3 away) {
    if (s.stuckLevel > 0.62 && s.tick - s.memory.lastEscapeTick > 8.0) {
        if (sukuna instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        s.memory.strafeSide = s.memory.strafeSide == 0 ? -s.strafeSide : -s.memory.strafeSide;
        s.memory.lastEscapeTick = s.tick;
        Vec3 escapeLateral = lateral.normalize().scale(s.memory.strafeSide);
        return bestVisibleDestination(s, List.of(
                sukuna.position().add(escapeLateral.scale(5.0)).add(away.scale(1.5)),
                sukuna.position().add(escapeLateral.scale(3.5)).add(toward.scale(2.5)),
                sukuna.position().add(away.scale(4.0)),
                sukuna.position().add(toward.scale(3.0)).add(escapeLateral.scale(2.0))));
    }
    if (OpSukunaEngineCore.isWalkableDestination(s.world, sukuna, destination) && !OpSukunaEngineCore.pathBlocked(s.world, sukuna, destination)) {
        return destination;
    }
    s.memory.strafeSide = s.memory.strafeSide == 0 ? -s.strafeSide : -s.memory.strafeSide;
    Vec3 altLateral = lateral.normalize().scale(s.memory.strafeSide);
    return bestVisibleDestination(s, List.of(
            sukuna.position().add(altLateral.scale(4.0)).add(toward.scale(s.itadoriModulo ? 1.5 : 0.5)),
            sukuna.position().add(altLateral.scale(-4.0)).add(toward.scale(1.0)),
            sukuna.position().add(away.scale(3.0)).add(altLateral.scale(2.0)),
            sukuna.position().add(toward.scale(3.0)).add(altLateral.scale(2.0))));
}

static Vec3 bestVisibleDestination(OpSukunaSnapshot s, List<Vec3> candidates) {
    Vec3 fallback = candidates.isEmpty() ? s.sukuna.position() : candidates.get(0);
    for (Vec3 candidate : candidates) {
        if (OpSukunaEngineCore.isWalkableDestination(s.world, s.sukuna, candidate)
                && !OpSukunaEngineCore.pathBlocked(s.world, s.sukuna, candidate)
                && OpSukunaEngineCore.hasClearMovementSegment(s.world, s.sukuna, candidate, 1.2)) {
            return candidate;
        }
    }
    return fallback;
}

static void moveTo(LivingEntity sukuna, Vec3 destination, double speed) {
    moveTo(sukuna, destination, speed, 0.36);
}

static void moveTo(LivingEntity sukuna, Vec3 destination, double speed, double impulseScale) {
    Vec3 self = sukuna.position();
    if (sukuna instanceof Mob mob) {
        mob.getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
    }
    Vec3 impulse = destination.subtract(self);
    if (impulse.lengthSqr() > 1.0E-4) {
        double wallPenalty = OpSukunaEngineCore.hasClearMovementSegment(sukuna.level(), sukuna, destination, 1.0) ? 1.0 : 0.35;
        Vec3 flat = OpSukunaEngineCore.horizontal(impulse).scale(Mth.clamp(impulseScale * wallPenalty, 0.0, 0.62));
        sukuna.setDeltaMovement(sukuna.getDeltaMovement().add(flat.x, 0.0, flat.z));
    }
}

static void faceTarget(LivingEntity sukuna, LivingEntity target) {
    Vec3 delta = target.position().subtract(sukuna.position());
    if (delta.lengthSqr() < 1.0E-4) {
        return;
    }
    float yaw = (float) (Mth.atan2(delta.z, delta.x) * (180.0 / Math.PI)) - 90.0F;
    float pitch = (float) (-(Mth.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)) * (180.0 / Math.PI)));
    sukuna.setYRot(yaw);
    sukuna.setXRot(Mth.clamp(pitch, -70.0F, 70.0F));
    sukuna.setYBodyRot(yaw);
    sukuna.setYHeadRot(yaw);
}

}
