package com.modderg.tameablebeasts.server.entity.custom;

import com.modderg.tameablebeasts.client.events.ModEventClient;
import com.modderg.tameablebeasts.server.entity.EntityInit;
import com.modderg.tameablebeasts.server.entity.RideableTBAnimal;
import com.modderg.tameablebeasts.server.entity.goals.TBFollowOwnerGoal;
import com.modderg.tameablebeasts.server.entity.goals.WaterMountLookControl;
import com.modderg.tameablebeasts.server.item.ItemInit;
import com.modderg.tameablebeasts.server.item.block.EggBlockItem;
import com.modderg.tameablebeasts.server.tags.TBTags;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;



public class ArchelonEntity extends RideableTBAnimal {

    @Override
    public Item itemSaddle() {
        return ItemInit.CHIKOTE_SADDLE.get();
    }

    private static final EntityDataAccessor<Integer> OUT_OF_WATER_RIDING_TICKS = SynchedEntityData.defineId(ArchelonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BlockPos> TRAVEL_POS = SynchedEntityData.defineId(ArchelonEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Boolean> TRAVELLING = SynchedEntityData.defineId(ArchelonEntity.class, EntityDataSerializers.BOOLEAN);


    public ArchelonEntity(EntityType<? extends TamableAnimal> p_21803_, Level p_21804_) {
        super(p_21803_, p_21804_);
        this.textureIdSize = 3;
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.01F, true);
        this.lookControl = new WaterMountLookControl(this,10);
        this.moveControl = new ArchelonEntity.TurtleMoveControl(this);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);

    }

    static class TurtleMoveControl extends MoveControl {
        private final ArchelonEntity turtle;

        TurtleMoveControl(ArchelonEntity p_30286_) {
            super(p_30286_);
            this.turtle = p_30286_;
        }

        private void updateSpeed() {
            if (this.turtle.isInWater()) {
                this.turtle.setDeltaMovement(this.turtle.getDeltaMovement().add(0.0D, 0.005D, 0.0D));
                if (this.turtle.isBaby()) {
                    this.turtle.setSpeed(Math.max(this.turtle.getSpeed() / 3.0F, 0.06F));
                }
            } else if (this.turtle.onGround()) {
                this.turtle.setSpeed(Math.max(this.turtle.getSpeed() / 2.0F, 0.06F));
            }

        }

        public void tick() {
            this.updateSpeed();
            if (this.operation == MoveControl.Operation.MOVE_TO && !this.turtle.getNavigation().isDone()) {
                double d0 = this.wantedX - this.turtle.getX();
                double d1 = this.wantedY - this.turtle.getY();
                double d2 = this.wantedZ - this.turtle.getZ();
                double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                if (d3 < (double)1.0E-5F) {
                    this.mob.setSpeed(0.0F);
                } else {
                    d1 /= d3;
                    float f = (float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
                    this.turtle.setYRot(this.rotlerp(this.turtle.getYRot(), f, 90.0F));
                    this.turtle.yBodyRot = this.turtle.getYRot();
                    float f1 = (float)(this.speedModifier * this.turtle.getAttributeValue(Attributes.MOVEMENT_SPEED));
                    this.turtle.setSpeed(Mth.lerp(0.125F, this.turtle.getSpeed(), f1));
                    this.turtle.setDeltaMovement(this.turtle.getDeltaMovement().add(0.0D, (double)this.turtle.getSpeed() * d1 * 0.1D, 0.0D));
                }
            } else {
                this.turtle.setSpeed(0.0F);
            }
        }
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return true;
    }

    public static AttributeSupplier.Builder setCustomAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 24);

    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new TryFindWaterGoal(this));
        this.goalSelector.addGoal(1, new ArchelonEntity.TurtleBreedGoal(this, 1.D));
        this.goalSelector.addGoal(1, new ArchelonEntity.TurtleRandomStrollGoal(this, 0.5D, 70));
        this.goalSelector.addGoal(2, new ArchelonEntity.TurtleGoToWaterGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new ArchelonEntity.TurtleTravelGoal(this, 0.5D));
        this.goalSelector.addGoal(4,new TBFollowOwnerGoal(this, 1.0D, 10.0F, 6.0F));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, (double)1F, true));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    void setTravelPos(BlockPos p_30224_) {
        this.entityData.set(TRAVEL_POS, p_30224_);
    }

    BlockPos getTravelPos() {
        return this.entityData.get(TRAVEL_POS);
    }

    static class TurtleGoToWaterGoal extends MoveToBlockGoal {
        private static final int GIVE_UP_TICKS = 1200;
        private final ArchelonEntity turtle;

        TurtleGoToWaterGoal(ArchelonEntity p_30262_, double p_30263_) {
            super(p_30262_, p_30262_.isBaby() ? 2.0D : p_30263_, 24);
            this.turtle = p_30262_;
            this.verticalSearchStart = -1;
        }

        public boolean canContinueToUse() {
            return !this.turtle.isInWater() && this.tryTicks <= 1200 && this.isValidTarget(this.turtle.level(), this.blockPos);
        }

        public boolean canUse() {
            if (this.turtle.isBaby() && !this.turtle.isInWater()) {
                return super.canUse();
            } else {
                return !this.turtle.isInWater() && super.canUse();
            }
        }

        public boolean shouldRecalculatePath() {
            return this.tryTicks % 160 == 0;
        }

        protected boolean isValidTarget(LevelReader p_30270_, BlockPos p_30271_) {
            return p_30270_.getBlockState(p_30271_).is(Blocks.WATER);
        }
    }

    static class TurtleRandomStrollGoal extends RandomStrollGoal {
        private final ArchelonEntity turtle;

        TurtleRandomStrollGoal(ArchelonEntity p_30303_, double p_30304_, int p_30305_) {
            super(p_30303_, p_30304_, p_30305_);
            this.turtle = p_30303_;
        }

        public boolean canUse() {
            return !this.mob.isInWater() && super.canUse();
        }
    }

    static class TurtleTravelGoal extends Goal {
        private final ArchelonEntity turtle;
        private final double speedModifier;
        private boolean stuck;


        TurtleTravelGoal(ArchelonEntity p_30333_, double p_30334_) {
            this.turtle = p_30333_;
            this.speedModifier = p_30334_;
        }

        public boolean canUse() {
            return this.turtle.isInWater();
        }

        public void start() {
            int i = 512;
            int j = 4;
            RandomSource randomsource = this.turtle.random;
            int k = randomsource.nextInt(1025) - 512;
            int l = randomsource.nextInt(9) - 4;
            int i1 = randomsource.nextInt(1025) - 512;
            if ((double) l + this.turtle.getY() > (double) (this.turtle.level().getSeaLevel() - 1)) {
                l = 0;
            }

            BlockPos blockpos = BlockPos.containing((double) k + this.turtle.getX(), (double) l + this.turtle.getY(), (double) i1 + this.turtle.getZ());
            this.turtle.setTravelPos(blockpos);
            this.turtle.setTravelling(true);
            this.stuck = false;
        }

        public void tick() {
            if (this.turtle.getNavigation().isDone()) {
                Vec3 vec3 = Vec3.atBottomCenterOf(this.turtle.getTravelPos());
                Vec3 vec31 = DefaultRandomPos.getPosTowards(this.turtle, 16, 3, vec3, (double) ((float) Math.PI / 10F));
                if (vec31 == null) {
                    vec31 = DefaultRandomPos.getPosTowards(this.turtle, 8, 7, vec3, (double) ((float) Math.PI / 2F));
                }

                if (vec31 != null) {
                    int i = Mth.floor(vec31.x);
                    int j = Mth.floor(vec31.z);
                    int k = 34;
                    if (!this.turtle.level().hasChunksAt(i - 34, j - 34, i + 34, j + 34)) {
                        vec31 = null;
                    }
                }

                if (vec31 == null) {
                    this.stuck = true;
                    return;
                }

                this.turtle.getNavigation().moveTo(vec31.x, vec31.y, vec31.z, this.speedModifier);
            }

        }
    }

    static class TurtleBreedGoal extends BreedGoal {
        private final ArchelonEntity turtle;

        TurtleBreedGoal(ArchelonEntity p_30244_, double p_30245_) {
            super(p_30244_, p_30245_);
            this.turtle = p_30244_;
        }

        protected void breed() {
            ServerPlayer serverplayer = this.animal.getLoveCause();
            if (serverplayer == null && this.partner.getLoveCause() != null) {
                serverplayer = this.partner.getLoveCause();
            }

            if (serverplayer != null) {
                serverplayer.awardStat(Stats.ANIMALS_BRED);
                CriteriaTriggers.BRED_ANIMALS.trigger(serverplayer, this.animal, this.partner, (AgeableMob)null);
            }


            this.animal.setAge(6000);
            this.partner.setAge(6000);
            this.animal.resetLove();
            this.partner.resetLove();
            RandomSource randomsource = this.animal.getRandom();
            if (this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
                this.level.addFreshEntity(new ExperienceOrb(this.level, this.animal.getX(), this.animal.getY(), this.animal.getZ(), randomsource.nextInt(7) + 1));
            }

        }
    }


    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (isTameFood(itemstack) && !this.isTame()) {
            tameGAnimal(player, itemstack, 10);
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    //actual speed 0.36
    @Override
    public float getRidingSpeedMultiplier() {
        return 1.2f;
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(Items.BEETROOT);
    }

    @Override
    public boolean isTameFood(ItemStack itemStack) {return itemStack.is(TBTags.Items.CHIKOTE_TAME_FOOD);}

    @Override
    public EggBlockItem getEgg() {
        return (EggBlockItem) ItemInit.CHIKOTE_EGG_ITEM.get();
    }

    //Bebes
    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob mob) {
        return EntityInit.ARCHELON.get().create(serverLevel);
    }

    @Override
    public void updateAttributes(){
        if (this.isBaby())
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2D);
        else
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
    }

    @Override
    public @NotNull PathNavigation createNavigation(@NotNull Level pLevel) {
        return new ArchelonEntity.TurtlePathNavigation(this, pLevel);
    }
    static class TurtlePathNavigation extends AmphibiousPathNavigation {
        TurtlePathNavigation(ArchelonEntity p_30294_, Level p_30295_) {
            super(p_30294_, p_30295_);
        }

        public boolean isStableDestination(BlockPos p_30300_) {
            Mob mob = this.mob;
            if (mob instanceof ArchelonEntity turtle) {
                if (turtle.isTravelling()) {
                    return this.level.getBlockState(p_30300_).is(Blocks.WATER);
                }
            }

            return !this.level.getBlockState(p_30300_.below()).isAir();
        }
    }

    boolean isTravelling() {
        return this.entityData.get(TRAVELLING);
    }

    public float getWalkTargetValue(BlockPos p_30159_, LevelReader p_30160_) {
        if (p_30160_.getFluidState(p_30159_).is(FluidTags.WATER)) {
            return 10.0F;
        } else {
            return TurtleEggBlock.onSand(p_30160_, p_30159_) ? 10.0F : p_30160_.getPathfindingCostFromLightLevels(p_30159_);
        }
    }

    public int getOutOfWaterRidingTicks() {
        return this.entityData.get(OUT_OF_WATER_RIDING_TICKS);
    }
    public void setOutOfWaterRidingTicks(int ticks) {
        this.entityData.set(OUT_OF_WATER_RIDING_TICKS, ticks);
    }
    public Vec3 movement;

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

    @Override
    protected float getWaterSlowDown() {
        return 1F;
    }

    protected void positionRider(Entity pPassenger, MoveFunction pCallback) {
        double d0 = this.getY() + this.getPassengersRidingOffset() + pPassenger.getMyRidingOffset();
        float ySin = Mth.sin(this.yBodyRot * 0.017453292F);
        float yCos = Mth.cos(this.yBodyRot * 0.017453292F);
        pCallback.accept(pPassenger, this.getX() - (double)(0.3F * ySin), d0+0.005, this.getZ() + (double)(0.3F * yCos));
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (!this.level().isClientSide && !this.isInWater() && this.onGround() && this.isVehicle()){
            this.ejectPassengers();
        }
    }

    public void travel(Vec3 vec3d) {

        float speed = this.getSpeed();

        if (isControlledByLocalInstance() && getControllingPassenger() != null && getControllingPassenger() instanceof Player rider) {
            speed = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);

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
                    moveZ *= 15F;
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

                this.setSpeed(speed);
            }
            else if (rider instanceof Player) {
//                calculateEntityAnimation(true);
                setDeltaMovement(Vec3.ZERO);
                return;
            }
        }

        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), vec3d);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
            }
        }else {
            super.travel(vec3d);
        }
    }

    @Override
    public boolean isSwimming() {
        return super.isSwimming();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController[] { new AnimationController((GeoAnimatable)this, "normal", 5, this::movementPredicate) });
        controllers.add(new AnimationController[] { new AnimationController((GeoAnimatable)this, "attackcontroller", 5, this::attackPredicate) });
    }

    private PlayState attackPredicate(AnimationState event) {
        if(this.swinging && event.getController().getAnimationState().equals(AnimationController.State.STOPPED)){
            event.getController().forceAnimationReset();
                event.getController().setAnimation(RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE));

            this.swinging = false;
        }
        return PlayState.CONTINUE;
    }

    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    protected PlayState movementPredicate(AnimationState event) {
        if (this.getDeltaMovement().horizontalDistance() > 1.0E-6D && this.isInWater()) {
            if (isSprinting()) {
                System.out.println("Ejecutando animación: SPRINT");
                event.setAndContinue(SWIM);
                event.getController().setAnimationSpeed(2.0D);
                return PlayState.CONTINUE;
            }
            if (event.isMoving()) {
                System.out.println("Ejecutando animación: WALK");
                event.setAndContinue(SWIM);
                event.getController().setAnimationSpeed(1.0D);
                return PlayState.CONTINUE;
            }
        }
        if (isSwimming()) {
            System.out.println("Ejecutando animación: SWIM");
            event.setAndContinue(SWIM);
            event.getController().setAnimationSpeed(1.0D);
            return PlayState.CONTINUE;
        }

        if (isInWater()) {
            System.out.println("Ejecutando animación: IDLE");
            event.setAndContinue(IDLE);
            event.getController().setAnimationSpeed(1.0D);
        }
        return PlayState.CONTINUE;
    }

    protected void dropEquipment() {
        super.dropEquipment();
        if (this.getIsSaddled()) {
            this.spawnAtLocation(ItemInit.CHIKOTE_SADDLE.get());
        }
    }

    private static final EntityDataAccessor<Boolean> IS_SADDLED = SynchedEntityData.defineId(ArchelonEntity.class, EntityDataSerializers.BOOLEAN);

    public void setIsSaddled(boolean pSaddled) {
        this.entityData.set(IS_SADDLED, pSaddled);
    }

    public boolean getIsSaddled() {
        return this.entityData.get(IS_SADDLED);
    }

    void setTravelling(boolean p_30241_) {
        this.entityData.set(TRAVELLING, p_30241_);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OUT_OF_WATER_RIDING_TICKS, 0);
        this.entityData.define(TRAVEL_POS, BlockPos.ZERO);
        this.entityData.define(TRAVELLING, false);
        this.entityData.define(IS_SADDLED, false);
    }

    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("IsSaddled", this.getIsSaddled());
    }

    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setIsSaddled(pCompound.getBoolean("IsSaddled"));
    }
}