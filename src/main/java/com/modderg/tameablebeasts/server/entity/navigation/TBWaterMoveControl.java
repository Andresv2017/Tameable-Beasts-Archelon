package com.modderg.tameablebeasts.server.entity.navigation;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

public class TBWaterMoveControl extends MoveControl {
    private final int maxTurn;
    private final boolean hoversInPlace;

    public TBWaterMoveControl(Mob mob, int maxTurn, boolean hoversInPlace) {
        super(mob);
        this.maxTurn = maxTurn;
        this.hoversInPlace = hoversInPlace;
    }

    @Override
    public void tick() {
        if (this.operation == MoveControl.Operation.MOVE_TO) {
            this.operation = MoveControl.Operation.WAIT;
            double deltaX = this.wantedX - this.mob.getX();
            double deltaY = this.wantedY - this.mob.getY();
            double deltaZ = this.wantedZ - this.mob.getZ();
            double distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;

            if (distanceSquared < 2.5000003E-7F) {
                this.mob.setYya(0.0F);
                this.mob.setZza(0.0F);
                return;
            }

            float targetYaw = (float) (Mth.atan2(deltaZ, deltaX) * (180F / (float) Math.PI)) - 90.0F;
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), targetYaw, 90.0F));

            // Ajuste de velocidad según si está en agua o en tierra
            float speed = this.mob.isInWater()
                    ? (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED))
                    : 0.02F; // Movimiento lento en tierra

            this.mob.setSpeed(speed);

            // Solo permite movimiento vertical si está en agua
            if (this.mob.isInWater()) {
                double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                float pitch = (float) (-(Mth.atan2(deltaY, horizontalDistance) * (180F / (float) Math.PI)));
                this.mob.setXRot(this.rotlerp(this.mob.getXRot(), pitch, (float) this.maxTurn));
                this.mob.setYya(deltaY > 0.0D ? speed : -speed); // Subida o bajada en agua
                this.mob.setNoGravity(true);
            } else {
                this.mob.setNoGravity(false);
            }
        } else {
            if (!this.hoversInPlace) {
                this.mob.setNoGravity(false);
            }
            this.mob.setYya(0.0F);
            this.mob.setZza(0.0F);
        }
    }
}
