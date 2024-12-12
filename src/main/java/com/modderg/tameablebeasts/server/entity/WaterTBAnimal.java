package com.modderg.tameablebeasts.server.entity;

import com.modderg.tameablebeasts.server.entity.goals.TBFollowOwnerGoal;
import com.modderg.tameablebeasts.server.entity.navigation.TBGroundPathNavigation;
import com.modderg.tameablebeasts.server.entity.navigation.TBSwimmingPathNavigation;
import com.modderg.tameablebeasts.server.entity.navigation.TBWaterMoveControl;
import com.modderg.tameablebeasts.server.packet.InitPackets;
import com.modderg.tameablebeasts.server.packet.StoCSyncSwimming;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class WaterTBAnimal extends TBAnimal {

    protected boolean isSwimming = true;

    public boolean isSwimming() {
        return isSwimming;
    }

    public void setIsSwimming(boolean swimming) {
        this.isSwimming = swimming;
    }

    private static final EntityDataAccessor<Boolean> GOAL_WANT_SWIMMING = SynchedEntityData.defineId(WaterTBAnimal.class, EntityDataSerializers.BOOLEAN);

    public void setGoalsRequireSwimming(boolean swimming) {
        this.getEntityData().set(GOAL_WANT_SWIMMING, swimming);
    }

    public boolean getGoalsRequireSwimming() {
        return this.getEntityData().get(GOAL_WANT_SWIMMING);
    }

    protected TBFollowOwnerGoal followOwnerGoal;

    protected WaterTBAnimal(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0f); // Permite la navegación en el agua
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("GOAL_WANT_SWIMMING", this.getGoalsRequireSwimming());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(GOAL_WANT_SWIMMING, !this.onGround());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);

        if (compound.contains("GOAL_WANT_SWIMMING"))
            this.setGoalsRequireSwimming(compound.getBoolean("GOAL_WANT_SWIMMING"));
    }

    protected void registerGoals() {
        super.registerGoals();
        followOwnerGoal = new TBFollowOwnerGoal(this, 1.0D, 10f, 6F, true);
        this.goalSelector.addGoal(0, followOwnerGoal);
    }

    protected Boolean shouldSwim() {
        return !isOrderedToSit() && this.isInWater(); // Asegura que solo quiera nadar si está en agua
    }

    public boolean isStill() {
        return !(this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-3D);
    }

    int updateSwimCount = 5;

    @Override
    public void tick() {
        if (!level().isClientSide() && updateSwimCount++ % 20 == 0 && this.shouldSwim() != isSwimming())
            switchNavigation();
        super.tick();
    }

    @Override
    public void travel(@NotNull Vec3 movementVector) {
        if (this.isInWater()) {
            if (!isSwimming) {
                switchNavigation(); // Cambia a modo de natación si no está activado
                this.setDeltaMovement(Vec3.ZERO); // Reinicia la velocidad para suavizar el cambio
            }
            // Movimiento en agua
            this.moveRelative(0.0F, movementVector); // Velocidad rápida en agua
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.8F)); // Resistencia en agua
        } else {
            if (isSwimming) {
                switchNavigation(); // Cambia a modo de tierra si no está activado
                this.setDeltaMovement(Vec3.ZERO); // Reinicia la velocidad para suavizar el cambio
            }
            // Velocidad lenta en tierra
            this.moveRelative(0.05F, movementVector); // Velocidad lenta en tierra
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.6F)); // Resistencia en tierra
        }
    }

    protected void switchNavigation() {
        if (this.isInWater()) {
            this.moveControl = new TBWaterMoveControl(this, 20, false);
            this.navigation = new TBSwimmingPathNavigation(this, this.level()).canFloat(true);
        } else {
            this.moveControl = new MoveControl(this);
            this.navigation = new TBGroundPathNavigation(this, this.level());
        }

        isSwimming = moveControl instanceof TBWaterMoveControl;
        this.setNoGravity(isSwimming);

        if (followOwnerGoal != null) { // Verifica que no sea nulo
            followOwnerGoal.refreshNavigatorPath();
        }

        InitPackets.sendToAll(new StoCSyncSwimming(this.getId(), isSwimming));
    }


    public <T extends WaterTBAnimal & GeoEntity> PlayState swimState(T entity, software.bernie.geckolib.core.animation.AnimationState<T> event) {
        if (entity.isStill())
            event.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
        else
            event.getController().setAnimation(RawAnimation.begin().then("swim", Animation.LoopType.LOOP));

        return PlayState.CONTINUE;
    }

    public <T extends WaterTBAnimal & GeoEntity> AnimationController<T> swimController(T entity) {
        return new AnimationController<>(entity, "movement", 5, event -> {
            if (entity.isSwimming())
                return swimState(entity, event);
            return groundState(entity, event);
        });
    }

}
