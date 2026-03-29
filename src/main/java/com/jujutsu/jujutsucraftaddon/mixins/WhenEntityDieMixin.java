package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.entity.SukunaFushiguroEntity;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModGameRules;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.*;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = WhenEntityDieProcedure.class, priority = -10000)
public abstract class WhenEntityDieMixin {

    /**
     * @author Satushi
     * @reason Refactored for v43. Adds Sukuna coat drops and controls fame gain for Sukuna players.
     */
    @Inject(at = @At("HEAD"), method = "execute", remap = false, cancellable = true)
    private static void execute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null) return;

        double fame, fameBase, levelStrength, fameGrade, difficulty, num1;
        String targetJp, message, mvpMessage, str1;
        boolean killCurse = false;
        boolean logicA;

        // v43 Initial Animation Reset
        PlayAnimationEntity2Procedure.execute(entity, "empty");
        Entity killer = entity instanceof Mob _mob ? _mob.getTarget() : null;

        // 1. PROJECTILE & SHIKIGAMI CHECK
        if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge:ranged_ammo"))) || entity.getPersistentData().getBoolean("Shikigami")) {
            return;
        }

        // 2. ADDON DROP: Sukuna Coat (Black)
        if (entity instanceof SukunaFushiguroEntity || entity instanceof com.jujutsu.jujutsucraftaddon.entity.SukunaFushiguroEntity) {
            if (Math.random() < 0.001) { // 1/1000
                if (world instanceof ServerLevel _level) {
                    ItemEntity coat = new ItemEntity(_level, x, y, z, new ItemStack(JujutsucraftaddonModItems.SUKUNA_COAT_BLACK.get()));
                    coat.setPickUpDelay(10);
                    _level.addFreshEntity(coat);
                }
            }
        }

        // 3. PLAYER DEATH TRACKING
        if (entity instanceof Player) {
            Vec3 center = new Vec3(x, y, z);
            for (Entity found : world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(40.0), e -> true)) {
                if ((found instanceof Mob _mobEnt && _mobEnt.getTarget() == entity)) {
                    num1 = 1.0;
                    for (int i = 0; indexCheck(i, 128); i++) {
                        str1 = "pName" + Math.round(num1);
                        if (found.getPersistentData().getString(str1).isEmpty()) {
                            found.getPersistentData().putString(str1, entity.getDisplayName().getString());
                            break;
                        }
                        num1++;
                    }
                }
            }
        }

        // 4. FAME & PROGRESSION LOGIC (v43 standard + JJKUR modifications)
        if (killer instanceof LivingEntity && entity.getPersistentData().getDouble("cnt_target") > 3.0) {
            levelStrength = entity instanceof LivingEntity _liv && _liv.hasEffect(MobEffects.DAMAGE_BOOST) ? _liv.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0.0;
            fameBase = Math.pow(levelStrength + 1.0, 2.0) + 1.0;

            if (entity.getPersistentData().getBoolean("CursedSpirit")) {
                targetJp = Component.translatable("jujutsu.message.kill1_1").getString();
                killCurse = true;
            } else if (entity.getPersistentData().getBoolean("CurseUser")) {
                targetJp = Component.translatable("jujutsu.message.kill1_2").getString();
            } else if (entity.getPersistentData().getBoolean("JujutsuSorcerer")) {
                targetJp = Component.translatable("jujutsu.message.kill1_3").getString();
            } else if (entity.getPersistentData().getBoolean("jjkChara")) {
                targetJp = " ";
            } else {
                targetJp = "";
            }

            if (!targetJp.isEmpty()) {
                Vec3 center = new Vec3(x, y, z);
                for (Entity found : world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(32.0), e -> e instanceof Player)) {
                    Player p = (Player) found;
                    if (p.getAbilities().instabuild) continue;
                    if (isSpectator(p)) continue;

                    // JJKUR Special Case: Sukuna Advancement Control
                    if (manageSukunaPlayerOrNo(p)) continue;

                    // Standard Target Checks
                    boolean validTarget = (p.getPersistentData().getBoolean("JujutsuSorcerer") && killer.getPersistentData().getBoolean("JujutsuSorcerer")) ||
                                          (p.getPersistentData().getBoolean("CurseUser") && killer.getPersistentData().getBoolean("CurseUser")) ||
                                          (p.getPersistentData().getBoolean("CursedSpirit") && killer.getPersistentData().getBoolean("CursedSpirit")) ||
                                          killer instanceof Player || killer.getPersistentData().getBoolean("Player");

                    if (validTarget && (entity.getPersistentData().getDouble("friend_num") == 0.0 || entity.getPersistentData().getDouble("friend_num") != p.getPersistentData().getDouble("friend_num"))) {
                        
                        logicA = false;
                        num1 = 1.0;
                        for (int i = 0; indexCheck(i, 128); i++) {
                            str1 = entity.getPersistentData().getString("pName" + Math.round(num1));
                            if (str1.isEmpty()) break;
                            if (str1.equals(p.getDisplayName().getString())) { logicA = true; break; }
                            num1++;
                        }

                        if (!logicA) {
                            if (killer == p || killer instanceof Player || killer.getPersistentData().getBoolean("Player") || 
                               (killer.getPersistentData().getDouble("friend_num") != 0.0 && killer.getPersistentData().getDouble("friend_num") == p.getPersistentData().getDouble("friend_num"))) {
                                
                                mvpMessage = "[MVP]";
                                fame = fameBase;
                                
                                JujutsucraftModVariables.PlayerVariables pVars = p.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables());
                                pVars.PlayerExperience = Math.max(pVars.PlayerExperience, levelStrength);

                                if (killCurse) awardFameAdvancements(p, levelStrength);

                                BeatJujutsuSorcererProcedure.execute(entity, p);
                                BeatEnemyProcedure.execute(entity, p);
                                BeatOtherProcedure.execute(entity, p);
                            } else {
                                mvpMessage = "";
                                fame = Math.round(fameBase / 10.0);
                            }

                            if (world.getLevelData().getGameRules().getBoolean(JujutsucraftModGameRules.JUJUTSU_GAIN_FAME)) {
                                applyFameAndGrades(p, world, fame, targetJp, mvpMessage);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean indexCheck(int i, int limit) { return i < limit; }

    private static boolean isSpectator(Player p) {
        if (p instanceof ServerPlayer _sp) return _sp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;
        return false;
    }

    private static boolean manageSukunaPlayerOrNo(Player entity) {
        Level world = entity.level();
        boolean isSukuna = entity.hasEffect(JujutsucraftModMobEffects.SUKUNA_EFFECT.get());
        if (isSukuna) {
            // JJKUR Custom Gamerule
            return !world.getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_CAN_SUKUNA_GET_ADVANCEMENTS);
        }
        return false;
    }

    private static void awardFameAdvancements(Player p, double level) {
        if (!(p instanceof ServerPlayer _sp)) return;
        String id = "jujutsucraft:fame_4";
        if (level >= 10.0) id = "jujutsucraft:fame_special";
        else if (level >= 8.0) id = "jujutsucraft:fame_1";
        else if (level >= 5.0) id = "jujutsucraft:fame_2";
        else if (level >= 1.0) id = "jujutsucraft:fame_3";
        
        Advancement adv = _sp.server.getAdvancements().getAdvancement(new ResourceLocation(id));
        if (adv != null) {
            AdvancementProgress progress = _sp.getAdvancements().getOrStartProgress(adv);
            if (!progress.isDone()) {
                for (String criteria : progress.getRemainingCriteria()) _sp.getAdvancements().award(adv, criteria);
            }
        }
    }

    private static void applyFameAndGrades(Player p, LevelAccessor world, double fame, String targetJp, String mvpMessage) {
        JujutsucraftModVariables.PlayerVariables pVars = p.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new JujutsucraftModVariables.PlayerVariables());
        pVars.PlayerFame += fame;
        
        String suffix = p.getPersistentData().getBoolean("CursedSpirit") ? Component.translatable("jujutsu.message.kill4").getString() : Component.translatable("jujutsu.message.kill3").getString();
        String finalMsg = targetJp + suffix.replace("[point]", "§l" + Math.round(fame) + "§r");
        
        if (!p.level().isClientSide()) {
            p.displayClientMessage(Component.literal(mvpMessage + finalMsg), false);
        }

        // Grade Advancement Check
        double fameGrade = 0;
        if (hasAdv(p, "jujutsucraft:fame_special")) fameGrade = 10.0;
        else if (hasAdv(p, "jujutsucraft:fame_1")) fameGrade = 8.0;
        else if (hasAdv(p, "jujutsucraft:fame_2")) fameGrade = 6.0;
        else if (hasAdv(p, "jujutsucraft:fame_3")) fameGrade = 4.0;

        double diff = world.getLevelData().getGameRules().getInt(JujutsucraftModGameRules.JUJUTSUUPGRADEDIFFICULTY);
        String gradeAdv = null;
        
        if (pVars.PlayerFame >= 4000.0 * diff && fameGrade >= 10.0) gradeAdv = "jujutsucraft:sorcerer_grade_special";
        else if (pVars.PlayerFame >= 2750.0 * diff && fameGrade >= 8.0) gradeAdv = "jujutsucraft:sorcerer_grade_1";
        else if (pVars.PlayerFame >= 1750.0 * diff) gradeAdv = "jujutsucraft:sorcerer_grade_1_semi";
        else if (pVars.PlayerFame >= 1000.0 * diff && fameGrade >= 6.0) gradeAdv = "jujutsucraft:sorcerer_grade_2";
        else if (pVars.PlayerFame >= 500.0 * diff) gradeAdv = "jujutsucraft:sorcerer_grade_2_semi";
        else if (pVars.PlayerFame >= 200.0 * diff && fameGrade >= 4.0) gradeAdv = "jujutsucraft:sorcerer_grade_3";
        else if (pVars.PlayerFame >= 50.0 * diff) gradeAdv = "jujutsucraft:sorcerer_grade_4";

        if (gradeAdv != null && p instanceof ServerPlayer _sp) {
            Advancement adv = _sp.server.getAdvancements().getAdvancement(new ResourceLocation(gradeAdv));
            if (adv != null) {
                AdvancementProgress ap = _sp.getAdvancements().getOrStartProgress(adv);
                if (!ap.isDone()) {
                    for (String criteria : ap.getRemainingCriteria()) _sp.getAdvancements().award(adv, criteria);
                }
            }
        }
        pVars.syncPlayerVariables(p);
    }

    private static boolean hasAdv(Player p, String id) {
        if (p instanceof ServerPlayer _sp && _sp.server != null) {
            Advancement adv = _sp.server.getAdvancements().getAdvancement(new ResourceLocation(id));
            return adv != null && _sp.getAdvancements().getOrStartProgress(adv).isDone();
        }
        return false;
    }
}
