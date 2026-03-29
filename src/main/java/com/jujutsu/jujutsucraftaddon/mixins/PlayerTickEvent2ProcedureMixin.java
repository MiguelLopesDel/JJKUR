package com.jujutsu.jujutsucraftaddon.mixins;

import com.jujutsu.jujutsucraftaddon.entity.PartialRikaEntity;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModEnchantments;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModGameRules;
import com.jujutsu.jujutsucraftaddon.init.JujutsucraftaddonModMobEffects;
import com.jujutsu.jujutsucraftaddon.network.JujutsucraftaddonModVariables;
import com.jujutsu.jujutsucraftaddon.procedures.*;
import net.mcreator.jujutsucraft.init.JujutsucraftModItems;
import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressedProcedure;
import net.mcreator.jujutsucraft.procedures.PlayerTickEvent2Procedure;
import net.mcreator.jujutsucraft.procedures.ReturnInsideItemProcedure;
import net.mcreator.jujutsucraft.procedures.StartGuardProcedure;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nullable;
import java.util.Objects;

@Mixin(value = PlayerTickEvent2Procedure.class, priority = -10000, remap = false)
public abstract class PlayerTickEvent2ProcedureMixin {

    /**
     * @author Satushi
     * @reason Optimized for v43 (2-tick interval) and integrated JJKUR unique passives
     */
    @Overwrite
    private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        ResourceLocation entityTypeKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityTypeKey != null && entityTypeKey.toString().startsWith("jujutsucraft") && !(entity instanceof PartialRikaEntity)) {
            return;
        }

        if (entity.isAlive() && entity instanceof ServerPlayer _serverPlayer && _playerTickCheck(_serverPlayer)) {
            JujutsucraftModVariables.PlayerVariables baseVars = _serverPlayer.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new JujutsucraftModVariables.PlayerVariables());
            JujutsucraftaddonModVariables.PlayerVariables addonVars = _serverPlayer.getCapability(JujutsucraftaddonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .orElse(new JujutsucraftaddonModVariables.PlayerVariables());

            boolean changeTechnique = false;
            boolean sync = false;

            // 1. Shift / Guard Logic
            if (entity.isShiftKeyDown()) {
                StartGuardProcedure.execute(world, entity);
                _serverPlayer.removeEffect(MobEffects.MOVEMENT_SPEED);
                if (!baseVars.flag_shift) {
                    baseVars.flag_shift = true;
                    changeTechnique = true;
                    sync = true;
                }
            } else if (baseVars.flag_shift) {
                baseVars.flag_shift = false;
                changeTechnique = true;
                sync = true;
            }

            // 2. Technique Change (ID 6 - Megumi)
            if (changeTechnique && (baseVars.PlayerCurseTechnique == 6.0 || baseVars.PlayerCurseTechnique2 == 6.0)) {
                String oldName = baseVars.PlayerSelectCurseTechniqueName;
                baseVars.noChangeTechnique = true;
                baseVars.syncPlayerVariables(entity);
                KeyChangeTechniqueOnKeyPressedProcedure.execute(world, x, y, z, entity);
                
                if (!oldName.equals(baseVars.PlayerSelectCurseTechniqueName)) {
                    if (!_serverPlayer.level().isClientSide() && _serverPlayer.getServer() != null) {
                        _serverPlayer.getServer().getCommands().performPrefixedCommand(
                            new CommandSourceStack(CommandSource.NULL, _serverPlayer.position(), _serverPlayer.getRotationVector(), 
                            (ServerLevel)_serverPlayer.level(), 4, _serverPlayer.getName().getString(), _serverPlayer.getDisplayName(), 
                            _serverPlayer.level().getServer(), _serverPlayer), "playsound ui.button.click master @s"
                        );
                    }
                }
            }

            // 3. Water Walking Logic
            if (addonVars.water == 1) {
                BlockPos belowPos = _serverPlayer.blockPosition().below();
                boolean isJustAboveWater = world.getBlockState(BlockPos.containing(x, y - 1, z)).getBlock() instanceof LiquidBlock;
                if (isJustAboveWater && !_serverPlayer.isInWater()) {
                    if (_serverPlayer.getDeltaMovement().y() <= 0) {
                        _serverPlayer.setDeltaMovement(_serverPlayer.getDeltaMovement().multiply(1.0, 0.0, 1.0));
                        _serverPlayer.setOnGround(true);
                        _serverPlayer.setPos(x, belowPos.getY() + 1.0, z);
                    }
                }
            }

            // 4. Periodic Events (Time-based)
            long gameTime = _serverPlayer.level().getGameTime();
            
            // BGM (7200 ticks)
            if (addonVars.BGM && gameTime % 7200 == 0) {
                if (!_serverPlayer.hasEffect(JujutsucraftaddonModMobEffects.MUSIC.get())) {
                    _serverPlayer.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.MUSIC.get(), 60, 1, false, false));
                }
            }

            // Megumi Shikigami Regeneration (12000 ticks)
            if (baseVars.PlayerCurseTechnique == 6 && gameTime % 12000 == 0) {
                if (world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_REGENERATE_SHIKIGAMI)) {
                    if (!_serverPlayer.hasEffect(JujutsucraftaddonModMobEffects.REGENERATE_SHIKIGAMI.get())) {
                        _serverPlayer.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.REGENERATE_SHIKIGAMI.get(), 60, 1, false, false));
                    }
                }
            }

            // Sukuna Possession (7200 ticks)
            if (gameTime % 7200 == 0 && world.getLevelData().getGameRules().getBoolean(JujutsucraftaddonModGameRules.JJKU_SUKUNA_POSSESSION_ENABLED)) {
                if (baseVars.BodyItem.getCount() >= 1.0) {
                    boolean enchained = _serverPlayer.getAdvancements().getOrStartProgress(Objects.requireNonNull(_serverPlayer.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsucraftaddon:enchained")))).isDone();
                    if (!enchained && !_serverPlayer.hasEffect(JujutsucraftaddonModMobEffects.SUKUNA_VC.get())) {
                        _serverPlayer.addEffect(new MobEffectInstance(JujutsucraftaddonModMobEffects.SUKUNA_VC.get(), 60, 1, false, false));
                    }
                }
            }

            // 5. Enchantment: CE Health Regeneration
            ItemStack chestArmor = _serverPlayer.getItemBySlot(EquipmentSlot.CHEST);
            int regenLevel = EnchantmentHelper.getItemEnchantmentLevel(JujutsucraftaddonModEnchantments.CE_HEALTH_REGENERATION.get(), chestArmor);
            if (regenLevel > 0 && _serverPlayer.getHealth() < _serverPlayer.getMaxHealth()) {
                if (!_serverPlayer.hasEffect(MobEffects.REGENERATION)) {
                    _serverPlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, regenLevel, false, true));
                }
                baseVars.PlayerCursePower -= 1.0;
                sync = true;
            }

            // 6. Addon State Procs
            if (entity.getPersistentData().getBoolean("PRESS_BURNOUT")) CounterBurnoutProcedure.execute(entity);
            if (entity.getPersistentData().getBoolean("PRESS_ULT")) UltimatesProcedure.execute(world, x, y, z, entity);
            if (entity.getPersistentData().getBoolean("Meditation")) MeditationPassiveProcedure.execute(world, entity);
            if (addonVars.RCTMasteryOn) AutoRCTNewProcedure.execute(entity);
            if (addonVars.InfusedDomain) ExtensionTickProcedure.execute(world, x, y, z, entity);

            if (sync) baseVars.syncPlayerVariables(entity);
        }
    }

    private static boolean _playerTickCheck(ServerPlayer player) {
        // v43 optimization: logic runs every 2nd tick
        return player.tickCount % 2 == 0;
    }
}
