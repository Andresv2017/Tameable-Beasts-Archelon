package com.modderg.tameablebeasts.server.entity.custom;

import com.modderg.tameablebeasts.server.entity.goals.WaterMountLookControl;
import com.modderg.tameablebeasts.server.entity.util.HitboxHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import com.modderg.tameablebeasts.client.events.ModEventClient;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

//TODO: Try to fix the head in the renderer
public class ArchelonEntity extends WaterAnimal implements GeoEntity, OwnableEntity, NeutralMob {

    static final TargetingConditions SWIM_WITH_PLAYER_TARGETING = TargetingConditions.forNonCombat().range(15.0D).ignoreLineOfSight();

    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(10, 29);
    @Nullable
    private UUID persistentAngerTarget;

    public float prevTilt;
    public float tilt;

    public Vec3 movement;

    private int rand;
    public ArchelonEntity(EntityType<? extends WaterAnimal> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 50, 10, 0.005F, 0.002F, true);
        this.lookControl = new WaterMountLookControl(this,10);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        rand = 0;
        this.setTextureId(this.random.nextInt(3));
    }

    private static final EntityDataAccessor<Boolean> FROM_EGG = SynchedEntityData.defineId(ArchelonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FROM_BOOK = SynchedEntityData.defineId(ArchelonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_SADDLED = SynchedEntityData.defineId(ArchelonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HAS_ARMOR = SynchedEntityData.defineId(ArchelonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> EATING_TIME = SynchedEntityData.defineId(ArchelonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OUT_OF_WATER_RIDING_TICKS = SynchedEntityData.defineId(ArchelonEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Optional<UUID>> DATA_OWNERUUID_ID = SynchedEntityData.defineId(ArchelonEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> IS_TAMED = SynchedEntityData.defineId(ArchelonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ANIMATION_STATE = SynchedEntityData.defineId(ArchelonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_REMAINING_ANGER_TIME = SynchedEntityData.defineId(ArchelonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BASE_VARIANT = SynchedEntityData.defineId(ArchelonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PATTERN_VARIANT = SynchedEntityData.defineId(ArchelonEntity.class, EntityDataSerializers.INT);

    protected static final RawAnimation BOOK = RawAnimation.begin().thenLoop("animation.ophthalmo.book");
    protected static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.ophthalmo.swim");
    protected static final RawAnimation SWIM_FAST = RawAnimation.begin().thenLoop("animation.ophthalmo.swim_fast");

    protected static final RawAnimation SWIM_SLOW = RawAnimation.begin().thenLoop("animation.ophthalmo.swim_slow");
    protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ophthalmo.idle");
    protected static final RawAnimation FLOP = RawAnimation.begin().thenLoop("animation.ophthalmo.flop");
    protected static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.ophthalmo.attack");
    protected static final RawAnimation ATTACK2 = RawAnimation.begin().thenPlay("animation.ophthalmo.attack2");
    protected static final RawAnimation EAT = RawAnimation.begin().thenPlay("animation.ophthalmo.eating");
    protected static final RawAnimation SWIM_HEADLESS = RawAnimation.begin().thenLoop("animation.ophthalmo.swim_headless");
    protected static final RawAnimation IDLE_HEADLESS = RawAnimation.begin().thenLoop("animation.ophthalmo.idle_headless");

    private static final EntityDataAccessor<Integer> TEXTURE_ID = SynchedEntityData.defineId(ArchelonEntity.class, EntityDataSerializers.INT);
    public void setTextureId(int i){
        this.getEntityData().set(TEXTURE_ID, i);
    }
    public int getTextureID(){
        return this.getEntityData().get(TEXTURE_ID);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new TryFindWaterGoal(this));
        this.goalSelector.addGoal(3, new OphthalmoSwimWithPlayerGoal(this, 2));
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0D, 10));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(5, new OphthalmoJumpGoal(this, 3));
        this.goalSelector.addGoal(2, new OphthalmoMeleeAttackGoal(this, (double)1F, true));
        if (!this.hasControllingPassenger()){
            this.targetSelector.addGoal(3, new OphthalmoOwnerHurtByTargetGoal(this));
            this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        }
        this.targetSelector.addGoal(8, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    public static AttributeSupplier.Builder setCustomAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FROM_EGG, false);
        this.entityData.define(FROM_BOOK, false);
        this.entityData.define(IS_SADDLED, false);
        this.entityData.define(HAS_ARMOR, false);
        this.entityData.define(EATING_TIME, 0);
        this.entityData.define(OUT_OF_WATER_RIDING_TICKS, 0);
        this.entityData.define(IS_TAMED, false);
        this.entityData.define(DATA_OWNERUUID_ID, Optional.empty());
        this.entityData.define(ANIMATION_STATE, 0);
        this.entityData.define(DATA_REMAINING_ANGER_TIME, 0);
        this.entityData.define(TEXTURE_ID, 0);
        this.entityData.define(BASE_VARIANT, 0);
        this.entityData.define(PATTERN_VARIANT, 0);
    }

    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("Tame", this.isTamed());
        pCompound.putBoolean("FromEgg", this.isFromEgg());
        pCompound.putBoolean("IsSaddled", this.getIsSaddled());
        pCompound.putBoolean("HasArmor", this.getHasArmor());
        pCompound.putInt("TEXTURE_ID", this.getTextureID());

        if (this.getOwnerUUID() != null) {
            pCompound.putUUID("Owner", this.getOwnerUUID());
        }

        this.addPersistentAngerSaveData(pCompound);

        pCompound.putInt("BaseColor", this.getBaseColor());
        pCompound.putInt("Pattern", this.getPattern());
    }

    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setTamed(pCompound.getBoolean("Tame"));
        this.setIsFromEgg(pCompound.getBoolean("FromEgg"));
        this.setIsSaddled(pCompound.getBoolean("IsSaddled"));
        this.setHasArmor(pCompound.getBoolean("HasArmor"));
        if (pCompound.contains("TEXTURE_ID"))
            this.setTextureId(pCompound.getInt("TEXTURE_ID"));

        UUID uuid;
        if (pCompound.hasUUID("Owner")) {
            uuid = pCompound.getUUID("Owner");
        } else {
            String s = pCompound.getString("Owner");
            uuid = OldUsersConverter.convertMobOwnerIfNecessary(this.getServer(), s);
        }

        if (uuid != null) {
            this.setOwnerUUID(uuid);
        }

        this.readPersistentAngerSaveData(this.level(), pCompound);

        this.setBaseColor(pCompound.getInt("BaseColor"));
        this.setPattern(pCompound.getInt("Pattern"));
    }

    protected void dropEquipment() {
        super.dropEquipment();
        if (this.getIsSaddled()) {
            this.spawnAtLocation(Items.SADDLE);
        }
        if (this.getHasArmor()){
            this.spawnAtLocation(getItemBySlot(EquipmentSlot.CHEST));
        }
    }


    public int getRemainingPersistentAngerTime() {
        return this.entityData.get(DATA_REMAINING_ANGER_TIME);
    }

    public void setRemainingPersistentAngerTime(int pTime) {
        this.entityData.set(DATA_REMAINING_ANGER_TIME, pTime);
    }

    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    @Nullable
    public UUID getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    public void setPersistentAngerTarget(@Nullable UUID pTarget) {
        this.persistentAngerTarget = pTarget;
    }


    public boolean canAttack(LivingEntity entity) {
        boolean prev = super.canAttack(entity);
        if (prev && this.isBaby()) {
            return false;
        } else {
            return !this.isOwnedBy(entity) && prev;
        }
    }

    @Nullable
    @Override
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNERUUID_ID).orElse((UUID)null);
    }

    public void setOwnerUUID(@Nullable UUID pUuid) {
        this.entityData.set(DATA_OWNERUUID_ID, Optional.ofNullable(pUuid));
    }

    public boolean isOwnedBy(LivingEntity pEntity) {
        return pEntity == this.getOwner();
    }

    public void setTamed(boolean pTamed) {
        this.entityData.set(IS_TAMED, pTamed);
    }

    public boolean isTamed() {
        return this.entityData.get(IS_TAMED);
    }

    public void setIsFromEgg(boolean pTamed) {
        this.entityData.set(FROM_EGG, pTamed);
    }

    public boolean isFromEgg() {
        return this.entityData.get(FROM_EGG);
    }

    public void setBaseColor(int color) {
        this.entityData.set(BASE_VARIANT, color);
    }

    public int getBaseColor() {
        return this.entityData.get(BASE_VARIANT);
    }

    public void setPattern(int pattern) {
        this.entityData.set(PATTERN_VARIANT, pattern);
    }

    public int getPattern() {
        return this.entityData.get(PATTERN_VARIANT);
    }

    public void tame(Player pPlayer) {
        this.setTamed(true);
        this.setOwnerUUID(pPlayer.getUUID());
        this.level().broadcastEntityEvent(this, (byte)7);
    }

    public boolean isAlliedTo(Entity entityIn) {
        if (this.isTamed()) {
            LivingEntity livingentity = this.getOwner();
            if (entityIn == livingentity) {
                return true;
            }

            if (entityIn instanceof TamableAnimal) {
                return ((TamableAnimal)entityIn).isOwnedBy(livingentity);
            }

            if (livingentity != null) {
                return livingentity.isAlliedTo(entityIn);
            }
        }

        return entityIn.is(this);
    }


    public boolean wantsToAttack(LivingEntity pTarget, LivingEntity pOwner) {
        if (!(pTarget instanceof Creeper) && !(pTarget instanceof Ghast) && pTarget.isInWater()) {
            if (pTarget instanceof Wolf) {
                Wolf wolf = (Wolf)pTarget;
                return !wolf.isTame() || wolf.getOwner() != pOwner;
            } else if (pTarget instanceof Player && pOwner instanceof Player && !((Player)pOwner).canHarmPlayer((Player)pTarget)) {
                return false;
            } else if (pTarget instanceof AbstractHorse && ((AbstractHorse)pTarget).isTamed()) {
                return false;
            } else {
                return !(pTarget instanceof TamableAnimal) || !((TamableAnimal)pTarget).isTame();
            }
        } else {
            return false;
        }
    }

    public boolean doHurtTarget(Entity entityIn) {
        if (super.doHurtTarget(entityIn) && this.getAttackSound() != null) {

            this.playSound(this.getAttackSound(), 0.1F, 1.0F);

            return true;
        } else {
            return false;
        }
    }

    public int getEatingTime() {
        return this.entityData.get(EATING_TIME);
    }

    public void setEatingTime(int eat) {
        this.entityData.set(EATING_TIME, eat);
    }

    public int getOutOfWaterRidingTicks() {
        return this.entityData.get(OUT_OF_WATER_RIDING_TICKS);
    }

    public void setOutOfWaterRidingTicks(int ticks) {
        this.entityData.set(OUT_OF_WATER_RIDING_TICKS, ticks);
    }

    public void setIsSaddled(boolean pSaddled) {
        this.entityData.set(IS_SADDLED, pSaddled);
    }

    public boolean getIsSaddled() {
        return this.entityData.get(IS_SADDLED);
    }

    public boolean isSaddleable() {
        return this.isAlive() && this.isTamed();
    }

    public boolean canWearArmor() {
        return isTamed();
    }

    public boolean isArmor(ItemStack pStack) {
        return pStack.is(Items.SADDLE);
    }

    public void setHasArmor(boolean pHasArmor) {
        this.entityData.set(HAS_ARMOR, pHasArmor && this.isWearingArmor());
    }

    public boolean getHasArmor() {
        return this.entityData.get(HAS_ARMOR);
    }

    public boolean isWearingArmor() {
        return !this.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
    }

    public void equipArmor(Player pPlayer, ItemStack pArmor) {
        if (this.isArmor(pArmor)) {
            this.setArmor(pArmor, pPlayer);
            this.setHasArmor(isWearingArmor());
        }
    }

    private void setArmor(ItemStack pArmor, Player pPlayer) {
//        if (pPlayer.getAbilities().instabuild){
        this.setItemSlot(EquipmentSlot.CHEST, pArmor.copyWithCount(1));
//        }else {
//            //this.setItemSlot(EquipmentSlot.CHEST, pArmor.copyWithCount(1));
//            this.setItemSlot(EquipmentSlot.CHEST, pArmor);
//            //pArmor.shrink(1);
//        }
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
    }

    protected void doPlayerRide(Player pPlayer) {
        if (!this.level().isClientSide) {
            pPlayer.setYRot(this.getYRot());
            pPlayer.setXRot(this.getXRot());
            pPlayer.startRiding(this);
        }
    }

    public boolean dismountsUnderwater() {
        return false;
    }

    public int getMaxAirSupply() {
        return 2500;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    public boolean isFood(ItemStack pStack) {
        Item item = pStack.getItem();
        return item.isEdible() && pStack.getFoodProperties(this).isMeat();
    }

    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        return new WaterBoundPathNavigation(this, pLevel);
    }

    public boolean hurt (DamageSource pSource, float pAmount){

        Entity entity = pSource.getEntity();
        if (this.getHasArmor() && entity != null
                && !(pSource.is(DamageTypes.MAGIC) || pSource.is(DamageTypes.WITHER) || pSource.is(DamageTypes.FELL_OUT_OF_WORLD))){
            this.damageItem(this.getItemBySlot(EquipmentSlot.CHEST));
            for(int j = 0; j < 5; ++j) {
                this.level().addParticle(ParticleTypes.CRIT, this.getRandomX(1.0), this.getRandomY(), this.getRandomZ(1.0), 0.0, 0.0, 0.0);
            }
            return false;
        }
        else {
            return super.hurt(pSource, pAmount);
        }

    }

    @Override
    public void tick() {
        super.tick();

        if (!this.isNoAi()){

            if (this.isVehicle()){
                movement = new Vec3(this.getX() - this.xo, this.getY() - this.yo, this.getZ() - this.zo);
            }

            if (isVehicle() && getControllingPassenger() != null && this.level().isClientSide) {
                float added = (float) position().y() * (float) movement.y();
                float xTilt = Mth.clamp(added, -25.0F, 20.0F);

                setXRot(-Mth.lerp(getXRot(), xTilt, xTilt));
            }

            if (this.level().isClientSide && this.isInWater() && this.getDeltaMovement().lengthSqr() > 0.03D) {
                Vec3 vec3 = this.getViewVector(0.0F);
                float f = Mth.cos(this.getYRot() * ((float)Math.PI / 180F)) * 0.3F;
                float f1 = Mth.sin(this.getYRot() * ((float)Math.PI / 180F)) * 0.3F;
                float f2 = 1.2F - this.random.nextFloat() * 0.7F;

                for(int i = 0; i < 2; ++i) {
                    this.level().addParticle(ParticleTypes.DOLPHIN, this.getX() - vec3.x * (double)f2 + (double)f, this.getY() - vec3.y, this.getZ() - vec3.z * (double)f2 + (double)f1, 0.0D, 0.0D, 0.0D);
                    this.level().addParticle(ParticleTypes.DOLPHIN, this.getX() - vec3.x * (double)f2 - (double)f, this.getY() - vec3.y, this.getZ() - vec3.z * (double)f2 - (double)f1, 0.0D, 0.0D, 0.0D);
                }
            }

            if (this.getEatingTime()>0) {
                switch(getEatingTime()){
                    case 5, 10, 15: playSound(SoundEvents.GENERIC_EAT);
                }
                int prevEat = this.getEatingTime();
                this.setEatingTime(prevEat-1);
            }

            if (this.isVehicle()){
                if (this.isInWater()){
                    if (this.getOutOfWaterRidingTicks() > 0){
                        this.setOutOfWaterRidingTicks(0);
                    }
                } else {
                    int prev = this.getOutOfWaterRidingTicks();
                    this.setOutOfWaterRidingTicks(prev+1);
                }
            } else if (this.getOutOfWaterRidingTicks() > 0) {
                this.setOutOfWaterRidingTicks(0);
            }
        }
    }

    @Override
    protected void tickRidden(Player pPlayer, Vec3 pTravelVector) {
        super.tickRidden(pPlayer, pTravelVector);
        //Vec2 vec2 = this.getRiddenRotation(pPlayer);
        this.setRot(pPlayer.getYRot(), getXRot());
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
    }

//    protected Vec2 getRiddenRotation(LivingEntity pEntity) {
//        return new Vec2((float) -this.getDeltaMovement().y * 80, pEntity.getYRot());
//    }

    @Override
    protected float getWaterSlowDown() {
        return 1F;
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (!this.level().isClientSide && !this.isInWater() && this.onGround() && this.isVehicle()){
            this.ejectPassengers();
        }

//        if (!this.level().isClientSide && this.isVehicle()){
//            this.xRotO = (float) (-this.getDeltaMovement().y * 80);
//        }
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        double offsetX = 0.0; // Ajuste horizontal
        double offsetY = -0.4; // Ajuste vertical
        double offsetZ = 0.0; // Ajuste en la dirección frontal/trasera

        double posX = this.getX() + offsetX;
        double posY = this.getY() + this.getPassengersRidingOffset() + offsetY;
        double posZ = this.getZ() + offsetZ;

        moveFunction.accept(passenger, posX, posY, posZ);
    }


    public void travel(Vec3 vec3d) {

        float speed = this.getSpeed();

        if (isControlledByLocalInstance() && getControllingPassenger() != null && getControllingPassenger() instanceof Player rider) {
            speed = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.2F; // Multiplica por un factor menor para reducir la velocidad


            double moveX = vec3d.x;
            double moveY = vec3d.y;
            double moveZ = rider.zza;

            yHeadRot = rider.yHeadRot;

            if (this.level().isClientSide){
                getLookControl().setLookAt(position().add(0.0D, 2.0D,0.0D));
            }

            if (isControlledByLocalInstance()) {

                if (isInWater()) {
                    moveX = rider.xxa * 3F;
                    moveZ = moveZ > 0 ? moveZ : 0;
                    moveZ *= 5F;
                }else{
                    moveZ = moveZ * 0.75;
                }

                if ((this.getOutOfWaterRidingTicks() > 0 && this.getOutOfWaterRidingTicks() < 5) || this.isInWater()){
                    if (this.isInWater()) {
                        moveY = Minecraft.getInstance().options.keyJump.isDown() ? 60F : ModEventClient.descendKey.isDown() ? -30F  : 0F;
                    }else{
                        moveY = Minecraft.getInstance().options.keyJump.isDown() ? 0.05F : 0F;
                    }
                }
                if (moveZ < 1F){
                    moveY = moveY/2;
                }

                if(Minecraft.getInstance().options.keyJump.isDown()){
                    if ((this.getOutOfWaterRidingTicks() > 0 && this.getOutOfWaterRidingTicks() < 5) || this.isInWater()){
                        this.setDeltaMovement(this.getDeltaMovement().add(0, 0.08, 0));
                    }

                }else if (ModEventClient.descendKey.isDown() && this.isInWater()){
                    this.setDeltaMovement(this.getDeltaMovement().add(0, -0.08, 0));

                }

                vec3d = new Vec3(moveX, moveY, moveZ);

                this.setSpeed(speed * 0.2F);
            }
            else if (rider instanceof Player) {
//                calculateEntityAnimation(true);
                setDeltaMovement(Vec3.ZERO);
                return;
            }
        }

        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed() * 0.2F, vec3d);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.4D));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
            }
        }else {
            super.travel(vec3d);
        }
    }

    protected float rotateTowards(float pFrom, float pTo, float pMaxDelta) {
        float f = Mth.degreesDifference(pFrom, pTo);
        float f1 = Mth.clamp(f, -pMaxDelta, pMaxDelta);
        return pFrom + f1;
    }

    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {

        if (!this.isVehicle() && this.isInWater()) {

            ItemStack itemstack = pPlayer.getItemInHand(pHand);


            if (!itemstack.isEmpty()) {
                InteractionResult interactionresult = itemstack.interactLivingEntity(pPlayer, this, pHand);
                if (interactionresult.consumesAction()) {
                    return interactionresult;
                }

                if (itemstack.is(Items.SHEARS) && this.isTamed()){

                    if (this.getHasArmor()){
                        this.spawnAtLocation(this.getItemBySlot(EquipmentSlot.CHEST));
                        this.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                        this.setHasArmor(false);

                        return InteractionResult.SUCCESS;
                    }else if (this.getIsSaddled()){
                        this.playSound(SoundEvents.SHEEP_SHEAR);
                        this.spawnAtLocation(Items.SADDLE);
                        this.setIsSaddled(false);

                        return InteractionResult.SUCCESS;
                    }

                }

                if (itemstack.is(Items.TROPICAL_FISH) && !this.isTamed() && this.getEatingTime()==0){

                    if (!pPlayer.getAbilities().instabuild) {
                        itemstack.shrink(1);
                    }

                    this.setEatingTime(20);
                    this.tame(pPlayer);

                    this.playSound(SoundEvents.DOLPHIN_EAT);

                    for(int j = 0; j < 5; ++j) {
                        this.level().addParticle(ParticleTypes.HEART, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 0.0, 0.0, 0.0);
                    }

                    return InteractionResult.SUCCESS;
                }

                if (itemstack.is(Items.SADDLE) && isSaddleable() && !this.getIsSaddled()){
                    if (!pPlayer.getAbilities().instabuild) {
                        itemstack.shrink(1);
                    }
                    this.setIsSaddled(true);
                    this.playSound(SoundEvents.HORSE_SADDLE);
                    return InteractionResult.SUCCESS;
                }

                if (this.canWearArmor() && this.isArmor(itemstack) && !this.isWearingArmor()) {
                    this.equipArmor(pPlayer, itemstack);
                    if (!pPlayer.getAbilities().instabuild) {
                        itemstack.shrink(1);
                    }
                    return InteractionResult.SUCCESS;
                }

                if (this.isTamed()){
                    if (this.isFood(itemstack) && this.getHealth() < this.getMaxHealth()){
                        if (!pPlayer.getAbilities().instabuild) {
                            itemstack.shrink(1);
                        }
                        this.heal((float)itemstack.getFoodProperties(this).getNutrition());
                        if (!pPlayer.getAbilities().instabuild) {
                            itemstack.shrink(1);
                        }
                        for(int j = 0; j < 5; ++j) {
                            this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 0.0, 0.0, 0.0);
                        }
                        this.setEatingTime(20);
                        return InteractionResult.SUCCESS;
                    }

                    if (itemstack.is(Items.IRON_HORSE_ARMOR) && this.getHasArmor()){
                        if (!pPlayer.getAbilities().instabuild) {
                            itemstack.shrink(1);
                        }
                        ItemStack armor = this.getItemBySlot(EquipmentSlot.CHEST);
                        int prevDamage = armor.getDamageValue();
                        int durabilityLeft = armor.getMaxDamage() - armor.getDamageValue();
                        if (durabilityLeft < armor.getMaxDamage()){
                            if (armor.getMaxDamage() > durabilityLeft+50) {
                                this.repairItem(this.getItemBySlot(EquipmentSlot.CHEST), 50);
                            }else {
                                this.repairItem(this.getItemBySlot(EquipmentSlot.CHEST), prevDamage);
                            }
                            for(int j = 0; j < 5; ++j) {
                                this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 0.0, 0.0, 0.0);
                            }
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            }

            if (this.isTamed() && this.getIsSaddled()){
                this.doPlayerRide(pPlayer);
                return InteractionResult.SUCCESS;
            }else{
                return super.mobInteract(pPlayer, pHand);
            }

        }

        return super.mobInteract(pPlayer, pHand);
    }

    private void repairItem(ItemStack stack, int repair) {
        if (stack != null) {
            this.getItemBySlot(EquipmentSlot.CHEST).hurt(-repair, this.getRandom(), null);
            this.playSound(SoundEvents.ANVIL_PLACE, 1.0F, (this.random.nextFloat() - (this.random.nextFloat()) * 0.2F) + 0.8F);
        }
    }

    private void damageItem(ItemStack stack) {
        stack.hurt(1, this.getRandom(), null);
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            this.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            this.setHasArmor(false);
        }
        else if (stack.getDamageValue() == (stack.getMaxDamage()*0.5) || stack.getDamageValue() == (stack.getMaxDamage()*0.75) || stack.getDamageValue() == (stack.getMaxDamage()*0.25)){
            this.playSound(SoundEvents.ANVIL_PLACE, 1.0F, (this.random.nextFloat() - (this.random.nextFloat()) * 0.2F) + 1.0F);
        }else {
            this.playSound(SoundEvents.ANVIL_PLACE, 1.0F, (this.random.nextFloat() - (this.random.nextFloat()) * 0.2F) + 1.0F);
        }
    }

    public void aiStep() {
        if (!this.isInWater() && this.onGround() && this.verticalCollision) {
            this.setDeltaMovement(this.getDeltaMovement().add((this.random.nextFloat() * 2.0F - 1.0F) * 0.05F, 0.4F, (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F));
            this.setOnGround(false);
            this.hasImpulse = true;
        }

        super.aiStep();

        prevTilt = tilt;
        if (this.isInWater()) {
            final float v = Mth.degreesDifference(this.getYRot(), yRotO);
            if (Math.abs(v) > 1) {
                if (Math.abs(tilt) < 25) {
                    tilt -= Math.signum(v);
                }
            } else {
                if (Math.abs(tilt) > 0) {
                    final float tiltSign = Math.signum(tilt);
                    tilt -= tiltSign * 0.85F;
                    if (tilt * tiltSign < 0) {
                        tilt = 0;
                    }
                }
            }
        } else {
            tilt = 0;
        }

        if (!this.level().isClientSide) {
            this.updatePersistentAnger((ServerLevel)this.level(), true);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController[]{new AnimationController(this, "Normal", 5, this::Controller)});
        controllerRegistrar.add(new AnimationController[]{new AnimationController(this, "Head", 0, this::HeadController)});
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    protected <E extends GeoAnimatable> PlayState Controller(AnimationState<E> event) {

        if (!this.isFromBook()) {
            if (this.getAnimationState() == 21) {
                if (rand > 50) {
                    event.setAndContinue(ATTACK);
                } else {
                    event.setAndContinue(ATTACK2);
                }
            } else if (this.isVehicle()) {

                if (this.movement!=null){

                    if (this.movement.horizontalDistanceSqr() > 0.1 || (this.movement.y >= 2 || this.movement.y <= -2)) {
                        event.setAndContinue(SWIM_FAST);

                    } else if (this.movement.horizontalDistanceSqr() > 0.001 || (this.movement.y < 2 || this.movement.y > -2)) {
                        event.setAndContinue(SWIM_SLOW);
                    }else {
                        event.setAndContinue(IDLE);
                    }

                } else {
                    event.setAndContinue(IDLE);
                }

            }else if(this.getDeltaMovement().horizontalDistanceSqr() > 0.000001 && this.isInWater() && !this.isVehicle()) {
                if (this.getEatingTime() > 0){
                    event.setAndContinue(SWIM_HEADLESS);
                }
                else {
                    event.setAndContinue(SWIM);
                }
                event.getController().setAnimationSpeed(1.25f);
            } else if (!this.isInWater()&&!this.isVehicle()){
                event.setAndContinue(FLOP);
            } else {
                if (this.getEatingTime() > 0){
                    event.setAndContinue(IDLE_HEADLESS);
                }
                else {
                    event.setAndContinue(IDLE);
                }
            }
        }
        return PlayState.CONTINUE;
    }

    protected <E extends GeoAnimatable> PlayState HeadController(AnimationState<E> event) {
        if (this.isFromBook()){
            event.setAndContinue(BOOK);
            return PlayState.CONTINUE;
        }else if (this.getEatingTime()>0){
            event.setAndContinue(EAT);
            return PlayState.CONTINUE;
        }else {
            event.getController().forceAnimationReset();
            return PlayState.STOP;
        }
    }

    public int getAnimationState() {
        return this.entityData.get(ANIMATION_STATE);
    }

    public void setAnimationState(int anim) {
        this.entityData.set(ANIMATION_STATE, anim);
    }

    @Override
    public boolean canBeLeashed(Player pPlayer) {
        return !this.isVehicle() && !this.isAngryAt(pPlayer);
    }


    public boolean isFromBook() {return this.entityData.get(FROM_BOOK);}

    static class OphthalmoMeleeAttackGoal extends Goal {
        protected final ArchelonEntity mob;
        private final double speedModifier;
        private final boolean followingTargetEvenIfNotSeen;
        private Path path;
        private double pathedTargetX;
        private double pathedTargetY;
        private double pathedTargetZ;
        private int ticksUntilNextPathRecalculation;
        private int ticksUntilNextAttack;
        private long lastCanUseCheck;
        private int failedPathFindingPenalty = 0;
        private boolean canPenalize = false;
        private int animTime = 0;

        public OphthalmoMeleeAttackGoal(ArchelonEntity anomalocaris, double speed, boolean canFollowIfUnseen) {
            this.mob = anomalocaris;
            this.speedModifier = speed;
            this.followingTargetEvenIfNotSeen = canFollowIfUnseen;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        public boolean canUse() {
            long i = this.mob.level().getGameTime();
            this.lastCanUseCheck = i;
            LivingEntity livingentity = this.mob.getTarget();
            if (livingentity == null) {
                return false;
            } else if (!livingentity.isAlive()) {
                return false;
            } else if (this.canPenalize) {
                if (--this.ticksUntilNextPathRecalculation <= 0) {
                    this.path = this.mob.getNavigation().createPath(livingentity, 0);
                    this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);
                    return this.path != null;
                } else {
                    return true;
                }
            } else {
                this.path = this.mob.getNavigation().createPath(livingentity, 0);
                if (this.path != null) {
                    return true;
                } else {
                    return this.getAttackReachSqr(livingentity) >= this.mob.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
                }
            }
        }

        public boolean canContinueToUse() {
            LivingEntity livingentity = this.mob.getTarget();
            if (livingentity == null) {
                return false;
            } else if (!livingentity.isAlive()) {
                return false;
            } else if (!this.followingTargetEvenIfNotSeen) {
                return !this.mob.getNavigation().isDone();
            } else if (!this.mob.isWithinRestriction(livingentity.blockPosition())) {
                return false;
            } else {
                return !(livingentity instanceof Player) || !livingentity.isSpectator() && !((Player)livingentity).isCreative();
            }
        }

        public void start() {
            this.mob.getNavigation().moveTo(this.path, this.speedModifier);
            this.ticksUntilNextPathRecalculation = 0;
            this.ticksUntilNextAttack = 0;
            this.animTime = 0;
            this.mob.setAnimationState(0);
        }

        public void stop() {
            LivingEntity livingentity = this.mob.getTarget();
            if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(livingentity)) {
                this.mob.setTarget(null);
            }

            this.mob.setAnimationState(0);
        }

        public void tick() {
            LivingEntity target = this.mob.getTarget();
            double distance = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
            double reach = this.getAttackReachSqr(target);
            int animState = this.mob.getAnimationState();
            Vec3 aim = this.mob.getLookAngle();
            new Vec2((float)(aim.x / (1.0 - Math.abs(aim.y))), (float)(aim.z / (1.0 - Math.abs(aim.y))));

            if (animState == 21) {
                this.tickBiteAttack();
            } else {
                this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
                this.ticksUntilNextAttack = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
                this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                this.doMovement(target, distance);
                this.checkForCloseRangeAttack(distance, reach);
            }

        }

        protected void doMovement(LivingEntity livingentity, Double d0) {
            this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
            if ((this.followingTargetEvenIfNotSeen || this.mob.getSensing().hasLineOfSight(livingentity)) && this.ticksUntilNextPathRecalculation <= 0 && (this.pathedTargetX == 0.0 && this.pathedTargetY == 0.0 && this.pathedTargetZ == 0.0 || livingentity.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0 || this.mob.getRandom().nextFloat() < 0.05F)) {
                this.pathedTargetX = livingentity.getX();
                this.pathedTargetY = livingentity.getY();
                this.pathedTargetZ = livingentity.getZ();
                this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);
                if (this.canPenalize) {
                    this.ticksUntilNextPathRecalculation += this.failedPathFindingPenalty;
                    if (this.mob.getNavigation().getPath() != null) {
                        Node finalPathPoint = this.mob.getNavigation().getPath().getEndNode();
                        if (finalPathPoint != null && livingentity.distanceToSqr((double)finalPathPoint.x, (double)finalPathPoint.y, (double)finalPathPoint.z) < 1.0) {
                            this.failedPathFindingPenalty = 0;
                        } else {
                            this.failedPathFindingPenalty += 10;
                        }
                    } else {
                        this.failedPathFindingPenalty += 10;
                    }
                }

                if (d0 > 1024.0) {
                    this.ticksUntilNextPathRecalculation += 10;
                } else if (d0 > 256.0) {
                    this.ticksUntilNextPathRecalculation += 5;
                }

                if (!this.mob.getNavigation().moveTo(livingentity, this.speedModifier)) {
                    this.ticksUntilNextPathRecalculation += 15;
                }
            }

        }

        protected void checkForCloseRangeAttack(double distance, double reach) {
            if (distance <= reach && this.ticksUntilNextAttack <= 0) {
                int r = this.mob.getRandom().nextInt(2048);
                if (r <= 600) {
                    this.mob.setAnimationState(21);
                    this.mob.rand = this.mob.random.nextInt(0, 100);
                }
            }

        }

        protected boolean getRangeCheck() {
            return this.mob.distanceToSqr(this.mob.getTarget().getX(), this.mob.getTarget().getY(), this.mob.getTarget().getZ()) <= 1.7999999523162842 * this.getAttackReachSqr(this.mob.getTarget());
        }

        protected void tickBiteAttack() {
            ++this.animTime;
            if (this.animTime == 4) {
                this.preformBiteAttack();
            }

            if (this.animTime >= 8) {
                this.animTime = 0;
                if (this.getRangeCheck()) {
                    this.mob.setAnimationState(22);
                } else {
                    this.mob.setAnimationState(0);
                    this.resetAttackCooldown();
                    this.ticksUntilNextPathRecalculation = 0;
                }
            }
        }

        protected void preformBiteAttack() {
            Vec3 pos = this.mob.position();
            HitboxHelper.LargeAttackWithTargetCheck(this.mob.damageSources().mobAttack(this.mob), 10.0F, 0.2F, this.mob, pos, 5.0, -1.5707963267948966, 1.5707963267948966, -1.0, 3.0);
        }

        protected void resetAttackCooldown() {
            this.ticksUntilNextAttack = 0;
        }

        protected boolean isTimeToAttack() {
            return this.ticksUntilNextAttack <= 0;
        }

        protected int getTicksUntilNextAttack() {
            return this.ticksUntilNextAttack;
        }

        protected int getAttackInterval() {
            return 5;
        }

        protected double getAttackReachSqr(LivingEntity p_179512_1_) {
            return (double)(this.mob.getBbWidth() * 2.5F * this.mob.getBbWidth() * 1.8F + p_179512_1_.getBbWidth());
        }
    }


    private SoundEvent getAttackSound() {
        return SoundEvents.PHANTOM_BITE;
    }

    protected void playStepSound(BlockPos p_28301_, BlockState p_28302_) {}


    @Nullable
    public LivingEntity getControllingPassenger() {
        Iterator var1 = this.getPassengers().iterator();

        Entity passenger;
        do {
            if (!var1.hasNext()) {
                return null;
            }

            passenger = (Entity)var1.next();
        } while(!(passenger instanceof Player));

        return this.getIsSaddled() ? (Player)passenger : null;
    }

    public class OphthalmoJumpGoal extends JumpGoal {
        private static final int[] STEPS_TO_CHECK = new int[]{0, 1, 4, 5, 6, 7};
        private final ArchelonEntity dolphin;
        private final int interval;
        private boolean breached;

        public OphthalmoJumpGoal(ArchelonEntity pDolphin, int pInterval) {
            this.dolphin = pDolphin;
            this.interval = reducedTickDelay(pInterval);
        }

        public boolean canUse() {
            if (this.dolphin.getRandom().nextInt(this.interval) != 0) {
                return false;
            } else {
                Direction direction = this.dolphin.getMotionDirection();
                int i = direction.getStepX();
                int j = direction.getStepZ();
                BlockPos blockpos = this.dolphin.blockPosition();

                for(int k : STEPS_TO_CHECK) {
                    if (!this.waterIsClear(blockpos, i, j, k) || !this.surfaceIsClear(blockpos, i, j, k)) {
                        return false;
                    }
                }

                return true;
            }
        }

        private boolean waterIsClear(BlockPos pPos, int pDx, int pDz, int pScale) {
            BlockPos blockpos = pPos.offset(pDx * pScale, 0, pDz * pScale);
            return this.dolphin.level().getFluidState(blockpos).is(FluidTags.WATER) && !this.dolphin.level().getBlockState(blockpos).blocksMotion();
        }

        private boolean surfaceIsClear(BlockPos pPos, int pDx, int pDz, int pScale) {
            return this.dolphin.level().getBlockState(pPos.offset(pDx * pScale, 1, pDz * pScale)).isAir() && this.dolphin.level().getBlockState(pPos.offset(pDx * pScale, 2, pDz * pScale)).isAir();
        }

        public boolean canContinueToUse() {
            double d0 = this.dolphin.getDeltaMovement().y;
            return (!(d0 * d0 < (double)0.03F) || this.dolphin.getXRot() == 0.0F || !(Math.abs(this.dolphin.getXRot()) < 10.0F) || !this.dolphin.isInWater()) && !this.dolphin.onGround();
        }

        public boolean isInterruptable() {
            return false;
        }

        public void start() {
            Direction direction = this.dolphin.getMotionDirection();
            this.dolphin.setDeltaMovement(this.dolphin.getDeltaMovement().add((double)direction.getStepX() * 0.6D, 0.7D, (double)direction.getStepZ() * 0.6D));
            this.dolphin.getNavigation().stop();
        }

        public void stop() {
            this.dolphin.setXRot(0.0F);
        }

        public void tick() {
            boolean flag = this.breached;
            if (!flag) {
                FluidState fluidstate = this.dolphin.level().getFluidState(this.dolphin.blockPosition());
                this.breached = fluidstate.is(FluidTags.WATER);
            }

            if (this.breached && !flag) {
                this.dolphin.playSound(SoundEvents.DOLPHIN_JUMP, 1.0F, 1.0F);
            }

            Vec3 vec3 = this.dolphin.getDeltaMovement();
            if (vec3.y * vec3.y < (double)0.03F && this.dolphin.getXRot() != 0.0F) {
                this.dolphin.setXRot(Mth.rotLerp(0.2F, this.dolphin.getXRot(), 0.0F));
            } else if (vec3.length() > (double)1.0E-5F) {
                double d0 = vec3.horizontalDistance();
                double d1 = Math.atan2(-vec3.y, d0) * (double)(180F / (float)Math.PI);
                this.dolphin.setXRot((float)d1);
            }

        }
    }

    public class OphthalmoOwnerHurtByTargetGoal extends TargetGoal {
        private final ArchelonEntity tameAnimal;
        private LivingEntity ownerLastHurtBy;
        private int timestamp;

        public OphthalmoOwnerHurtByTargetGoal(ArchelonEntity pTameAnimal) {
            super(pTameAnimal, false);
            this.tameAnimal = pTameAnimal;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        public boolean canUse() {
            if (this.tameAnimal.isTamed() && !this.tameAnimal.isVehicle()) {
                LivingEntity livingentity = this.tameAnimal.getOwner();
                if (livingentity == null) {
                    return false;
                } else {
                    this.ownerLastHurtBy = livingentity.getLastHurtByMob();
                    int i = livingentity.getLastHurtByMobTimestamp();
                    return i != this.timestamp && this.canAttack(this.ownerLastHurtBy, TargetingConditions.DEFAULT) && this.tameAnimal.wantsToAttack(this.ownerLastHurtBy, livingentity);
                }
            } else {
                return false;
            }
        }

        public void start() {
            this.mob.setTarget(this.ownerLastHurtBy);
            LivingEntity livingentity = this.tameAnimal.getOwner();
            if (livingentity != null) {
                this.timestamp = livingentity.getLastHurtByMobTimestamp();
            }

            super.start();
        }
    }

    static class OphthalmoSwimWithPlayerGoal extends Goal {
        private final ArchelonEntity dolphin;
        private final double speedModifier;
        @javax.annotation.Nullable
        private Player player;

        OphthalmoSwimWithPlayerGoal(ArchelonEntity pDolphin, double pSpeedModifier) {
            this.dolphin = pDolphin;
            this.speedModifier = pSpeedModifier;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        public boolean canUse() {
            this.player = this.dolphin.level().getNearestPlayer(ArchelonEntity.SWIM_WITH_PLAYER_TARGETING, this.dolphin);
            if (this.player == null || !this.dolphin.isTamed()) {
                return false;
            } else if (this.player == this.dolphin.getOwner()){
                return this.player.isSwimming();
            }else {
                return false;
            }
        }

        public boolean canContinueToUse() {
            return this.player != null && this.player.isSwimming() && this.dolphin.distanceToSqr(this.player) < 256.0D;
        }

        public void start() {
            //this.player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100), this.dolphin);
        }

        public void stop() {
            this.player = null;
            this.dolphin.getNavigation().stop();
        }

        public void tick() {
            this.dolphin.getLookControl().setLookAt(this.player, (float)(this.dolphin.getMaxHeadYRot() + 20), (float)this.dolphin.getMaxHeadXRot());
            if (this.dolphin.distanceToSqr(this.player) < 6.25D) {
                this.dolphin.getNavigation().stop();
            } else {
                this.dolphin.getNavigation().moveTo(this.player, this.speedModifier);
            }

        }
    }

    protected boolean shouldPassengersInheritMalus() {
        return true;
    }


    //persistance stuff
    public boolean removeWhenFarAway(double p_213397_1_) {
        return !this.hasCustomName() && !this.isTamed() && !this.isFromEgg();
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {

        this.setBaseColor(this.random.nextInt(0, 3));
        this.setPattern(this.random.nextInt(0, 4));

        spawnDataIn = super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
        return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    public String getColorName(int color){
        return switch (color){
            case 1 -> "_black";
            case 2-> "_green";
            default -> "_blue";
        };
    }

    public String getPatternName(int color){
        return switch (color){
            case 2-> "_spots";
            case 3-> "_streak";
            default -> "_stripes";
        };
    }

    public boolean canBreed() {
        return this.isTamed() && !this.isAngry();
    }

}
