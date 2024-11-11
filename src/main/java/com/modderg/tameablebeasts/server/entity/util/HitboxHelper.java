package com.modderg.tameablebeasts.server.entity.util;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class HitboxHelper {
    private static final double d = 0.6000000238418579;
    private static final double angleVar = 0.2617993877991494;

    public HitboxHelper() {
    }

    public static void LargeAttack(DamageSource source, float damage, float knockback, PathfinderMob entityIn, Vec3 pos0, double radius, double angleFirst, double angleLast, double hInf, double hSup) {
        Vec2 knockVec = MathHelpers.OrizontalAimVector(MathHelpers.AimVector(new Vec3(-entityIn.position().x, -entityIn.position().y, -entityIn.position().z), new Vec3(-entityIn.getTarget().position().x, -entityIn.getTarget().position().y, -entityIn.getTarget().position().z)));
        Vec2 aim = MathHelpers.OrizontalAimVector(entityIn.getLookAngle());
        Level worldIn = entityIn.level();

        for(int i = 0; (double)i <= radius / 0.6000000238418579; ++i) {
            for(int j = 0; (double)j <= (angleLast - angleFirst) / 0.2617993877991494; ++j) {
                double angle = angleFirst + 0.2617993877991494 * (double)j;
                double x = pos0.x + (double)i * 0.6000000238418579 * ((double)aim.x * Math.cos(angle) - (double)aim.y * Math.sin(angle));
                double z = pos0.z + (double)i * 0.6000000238418579 * ((double)aim.y * Math.cos(angle) + (double)aim.x * Math.sin(angle));

                for(int k = 0; (double)k <= (hSup - hInf) / 0.6000000238418579; ++k) {
                    double y = pos0.y + hInf + (double)k * 0.6000000238418579;
                    AABB scanAbove = new AABB(x - 0.6000000238418579, y - 2.4000000953674316, z - 0.6000000238418579, x + 0.6000000238418579, y + 1.2000000476837158, z + 0.6000000238418579);
                    List<LivingEntity> entities = new ArrayList(worldIn.getEntitiesOfClass(LivingEntity.class, scanAbove));
                    if (!entities.isEmpty()) {
                        for(int n = 0; n < entities.size(); ++n) {
                            LivingEntity target = (LivingEntity)entities.get(n);
                            if (target != entityIn) {
                                target.hurt(source, damage);
                                target.setLastHurtByMob(entityIn);
                                target.knockback((double)knockback, (double)knockVec.x, (double)knockVec.y);
                            }
                        }
                    }
                }
            }
        }

    }

    public static void LargeAttackWithTargetCheck(DamageSource source, float damage, float knockback, PathfinderMob entityIn, Vec3 pos0, double radius, double angleFirst, double angleLast, double hInf, double hSup) {
        Vec2 knockVec = MathHelpers.OrizontalAimVector(MathHelpers.AimVector(new Vec3(-entityIn.position().x, -entityIn.position().y, -entityIn.position().z), new Vec3(-entityIn.getTarget().position().x, -entityIn.getTarget().position().y, -entityIn.getTarget().position().z)));
        Vec2 aim = MathHelpers.OrizontalAimVector(entityIn.getLookAngle());
        Level worldIn = entityIn.level();

        for(int i = 0; (double)i <= radius / 0.6000000238418579; ++i) {
            for(int j = 0; (double)j <= (angleLast - angleFirst) / 0.2617993877991494; ++j) {
                double angle = angleFirst + 0.2617993877991494 * (double)j;
                double x = pos0.x + (double)i * 0.6000000238418579 * ((double)aim.x * Math.cos(angle) - (double)aim.y * Math.sin(angle));
                double z = pos0.z + (double)i * 0.6000000238418579 * ((double)aim.y * Math.cos(angle) + (double)aim.x * Math.sin(angle));

                for(int k = 0; (double)k <= (hSup - hInf) / 0.6000000238418579; ++k) {
                    double y = pos0.y + hInf + (double)k * 0.6000000238418579;
                    AABB scanAbove = new AABB(x - 0.6000000238418579, y - 4.0, z - 0.6000000238418579, x + 0.6000000238418579, y + 2.0, z + 0.6000000238418579);
                    List<LivingEntity> entities = new ArrayList(worldIn.getEntitiesOfClass(LivingEntity.class, scanAbove));
                    if (!entities.isEmpty()) {
                        for(int n = 0; n < entities.size(); ++n) {
                            LivingEntity target = (LivingEntity)entities.get(n);
                            if (target == entityIn.getTarget()) {
                                target.hurt(source, damage);
                                target.setLastHurtByMob(entityIn);
                                target.knockback((double)knockback, (double)knockVec.x, (double)knockVec.y);
                            }
                        }
                    }
                }
            }
        }

    }

    public static void PivotedPolyHitCheck(LivingEntity source, Vec3 boxOffset, double attackWidth, double attackHeight, double attackLength, ServerLevel world, float damage, DamageSource damageSource, float knockback, boolean disableShield) {
        Vec3 sourcePos = source.position();
        double entityAngle = (double)source.getYRot();
        Vec3 truePos = sourcePos.add(boxOffset);
        double[] trueXZ = new double[]{truePos.x, truePos.z};
        AffineTransform.getRotateInstance(Math.toRadians(entityAngle), sourcePos.x, sourcePos.z).transform(trueXZ, 0, trueXZ, 0, 1);
        double[] transformedTrueXY = trueXZ;
        Vec3 rotatedPos = new Vec3(transformedTrueXY[0], truePos.y, transformedTrueXY[1]);
        BlockPos finalPos = BlockPos.containing(rotatedPos.x, rotatedPos.y, rotatedPos.z);
        AABB Hitbox = (new AABB(finalPos)).inflate(attackWidth, attackHeight, attackLength);
        List<LivingEntity> victims = new ArrayList(world.getEntitiesOfClass(LivingEntity.class, Hitbox));

        for(int i = 0; i < victims.size(); ++i) {
            LivingEntity victim = (LivingEntity)victims.get(i);
            if (victim != source) {
                if (victim instanceof Player && disableShield) {
                    disableShield((Player)victim, victim.getMainHandItem(), victim.getOffhandItem(), source);
                }

                Vec2 knockVec = MathHelpers.OrizontalAimVector(MathHelpers.AimVector(new Vec3(-source.position().x, -source.position().y, -source.position().z), new Vec3(-victim.position().x, -victim.position().y, -victim.position().z)));
                victim.hurt(damageSource, damage);
                victim.setLastHurtByMob(source);
                victim.knockback((double)knockback, (double)knockVec.x, (double)knockVec.y);
            }
        }

    }

    public static void disableShield(Player pPlayer, ItemStack mainHand, ItemStack offHand, Entity source) {
        if (!mainHand.isEmpty() && mainHand.is(Items.SHIELD) && pPlayer.isBlocking()) {
            pPlayer.getCooldowns().addCooldown(Items.SHIELD, 100);
            source.level().broadcastEntityEvent(pPlayer, (byte)30);
        } else if (!offHand.isEmpty() && offHand.is(Items.SHIELD) && pPlayer.isBlocking()) {
            pPlayer.getCooldowns().addCooldown(Items.SHIELD, 100);
            source.level().broadcastEntityEvent(pPlayer, (byte)30);
        }

    }

    public static void hitboxOutline(AABB box, ServerLevel world) {
        world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, box.maxX, box.maxY, box.maxZ, 1, 0.0, 0.0, 0.0, 0.0);
        world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, box.maxX, box.minY, box.minZ, 1, 0.0, 0.0, 0.0, 0.0);
        world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, box.maxX, box.minY, box.maxZ, 1, 0.0, 0.0, 0.0, 0.0);
        world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, box.maxX, box.maxY, box.minZ, 1, 0.0, 0.0, 0.0, 0.0);
        world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, box.minX, box.maxY, box.maxZ, 1, 0.0, 0.0, 0.0, 0.0);
        world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, box.minX, box.minY, box.minZ, 1, 0.0, 0.0, 0.0, 0.0);
        world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, box.minX, box.minY, box.maxZ, 1, 0.0, 0.0, 0.0, 0.0);
        world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, box.minX, box.maxY, box.minZ, 1, 0.0, 0.0, 0.0, 0.0);
    }

    public static void LongAttackWithTargetCheck(DamageSource source, float damage, float knockback, PathfinderMob entityIn, Vec3 pos0, double radius, double edgeS, double edgeR, double hInf, double hSup) {
        Vec2 knockVec = MathHelpers.OrizontalAimVector(MathHelpers.AimVector(new Vec3(-entityIn.position().x, -entityIn.position().y, -entityIn.position().z), new Vec3(-entityIn.getTarget().position().x, -entityIn.getTarget().position().y, -entityIn.getTarget().position().z)));
        Vec2 aim = MathHelpers.OrizontalAimVector(entityIn.getLookAngle());
        Level worldIn = entityIn.level();

        for(int i = 0; (double)i <= radius / 0.6000000238418579; ++i) {
            for(int j = Math.round((float)Math.round(edgeS / 0.6000000238418579)); (double)j <= edgeR / 0.6000000238418579; ++j) {
                double angle = edgeR * Math.PI * -4.0 / 4.0 + 0.2617993877991494 * (double)j;
                double x = pos0.x + (double)aim.x * (0.6000000238418579 * (double)i + 0.6000000238418579 * (double)j);
                double z = pos0.z + (double)aim.y * (0.6000000238418579 * (double)i + 0.6000000238418579 * (double)j);

                for(int k = 0; (double)k <= (hSup - hInf) / 0.6000000238418579; ++k) {
                    double y = pos0.y + hInf + (double)k * 0.6000000238418579;
                    AABB scanAbove = new AABB(x - 0.6000000238418579, y - 4.0, z - 0.6000000238418579, x + 0.6000000238418579, y + 2.0, z + 0.6000000238418579);
                    List<LivingEntity> entities = new ArrayList(worldIn.getEntitiesOfClass(LivingEntity.class, scanAbove));
                    if (!entities.isEmpty()) {
                        for(int n = 0; n < entities.size(); ++n) {
                            LivingEntity target = (LivingEntity)entities.get(n);
                            if (target == entityIn.getTarget()) {
                                target.hurt(source, damage);
                                target.setLastHurtByMob(entityIn);
                                target.knockback((double)knockback, (double)knockVec.x, (double)knockVec.y);
                            }
                        }
                    }
                }
            }
        }

    }
}
