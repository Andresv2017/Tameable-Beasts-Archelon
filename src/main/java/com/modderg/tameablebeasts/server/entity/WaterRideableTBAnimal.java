package com.modderg.tameablebeasts.server.entity;

import com.modderg.tameablebeasts.client.packet.CtoSSyncRiderWantsSwimming;
import com.modderg.tameablebeasts.server.packet.InitPackets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class WaterRideableTBAnimal extends WaterTBAnimal implements TBRideable {

    public boolean upInput = false;
    public boolean downInput = false;

    private boolean riderWantsSwimming = false;

    public void setRiderWantsSwimming(boolean swim) {this.riderWantsSwimming = swim;}
    public boolean getRiderWantsSwimming() {return !this.getPassengers().isEmpty() && this.riderWantsSwimming;}

    protected WaterRideableTBAnimal(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("SADDLE", this.hasSaddle());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SADDLE, false);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("SADDLE"))
            this.setSaddle(compound.getBoolean("SADDLE"));
    }

    @Override
    public String getRidingMessage(){
        return getJumpKeyName() + " to Ascend, " + getCrouchKeyName() + " to Plummet , " + getShiftKeyName() + " to Dismount";
    }

    @Override
    public InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (isOwnedBy(player)){
            if (this.hasSaddle() && !isFood(itemstack)){
                if(!player.isShiftKeyDown()){
                    player.startRiding(this);
                    return InteractionResult.sidedSuccess(this.level().isClientSide);
                }
            } else if (!this.isBaby() && isSaddle(itemstack)) {
                setSaddle(true);
                this.playSound(SoundEvents.HORSE_SADDLE, 0.15F, 1.0F);
                itemstack.shrink(1);
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }


    @Override
    public void travel(@NotNull Vec3 vec3) {
        if (this.isAlive()) {
            if (this.getControllingPassenger() instanceof Player passenger) {
                this.yRotO = getYRot();
                this.xRotO = getXRot();
                setRot(passenger.getYRot(), passenger.getXRot() * 0.5f);

                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.yBodyRot;

                float x = passenger.xxa * 0.5F, z = passenger.zza * 0.5F;

                if (z <= 0)
                    z *= 0.25f;

                float speed = this.isInWater() ? 0.05f : 0.1f;
                this.setSpeed(speed);

                double yMovement = this.isInWater() ? (upInput ? 0.1 : downInput ? -0.1 : 0) : vec3.y;

                Vec3 movement = new Vec3(x, yMovement, z);
                moveRelative(applyPotionEffectsToSpeed(speed), movement);
                move(MoverType.SELF, getDeltaMovement());

                // Sincronizar con el servidor si el jugador cambia el deseo de nadar
                if (this.level().isClientSide() && this.riderWantsSwimming != this.isInWater()) {
                    this.riderWantsSwimming = !this.riderWantsSwimming;
                    InitPackets.sendToServer(new CtoSSyncRiderWantsSwimming(this.getId(), this.riderWantsSwimming));
                }
            } else {
                super.travel(vec3);
            }
        }
    }


    public <T extends WaterRideableTBAnimal & GeoEntity> AnimationController<T> swimController(T entity) {
        return new AnimationController<>(entity, "movement", 5, event -> {
            if (entity.isSwimming()) {
                if (entity.isControlledByLocalInstance()) {
                    if (entity.downInput) {
                        event.getController().setAnimation(RawAnimation.begin().then("swim", Animation.LoopType.LOOP));
                    } else if (entity.upInput) {
                        event.getController().setAnimation(RawAnimation.begin().then("swim", Animation.LoopType.LOOP));
                    } else if (entity.isStill()) {
                        event.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
                    } else {
                        event.getController().setAnimation(RawAnimation.begin().then("swim", Animation.LoopType.LOOP));
                    }
                } else {
                    if (entity.isStill())
                        event.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
                    else
                        event.getController().setAnimation(RawAnimation.begin().then("swim", Animation.LoopType.LOOP));
                }
                return PlayState.CONTINUE;
            }
            return groundState(entity, event);
        });
    }
}
