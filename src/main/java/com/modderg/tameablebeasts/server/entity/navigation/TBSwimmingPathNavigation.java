package com.modderg.tameablebeasts.server.entity.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TBSwimmingPathNavigation extends WaterBoundPathNavigation {

    private float distanceModifier = 1.5F;

    public TBSwimmingPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    public boolean isStableDestination(BlockPos pos) {
        // Solo permite destinos en agua
        return this.level.getBlockState(pos).getFluidState().isSource();
    }

    @Override
    protected void followThePath() {
        Vec3 currentPos = this.getTempMobPos();
        this.maxDistanceToWaypoint = this.mob.getBbWidth() * distanceModifier;
        Vec3i nextNodePos = this.path.getNextNodePos();

        double deltaX = Math.abs(this.mob.getX() - (nextNodePos.getX() + 0.5D));
        double deltaY = Math.abs(this.mob.getY() - nextNodePos.getY());
        double deltaZ = Math.abs(this.mob.getZ() - (nextNodePos.getZ() + 0.5D));

        // Define cuando el nodo está suficientemente cerca
        boolean closeEnough = deltaX < this.maxDistanceToWaypoint && deltaZ < this.maxDistanceToWaypoint && deltaY < 1.0D;

        // Avanza al siguiente nodo si está lo suficientemente cerca o si puede cortar la esquina
        if (closeEnough || (this.canCutCorner(this.path.getNextNode().type) && shouldTargetNextNodeInDirection(currentPos))) {
            this.path.advance();
        }

        // Detección de atascos
        this.doStuckDetection(currentPos);
    }

    private boolean shouldTargetNextNodeInDirection(Vec3 currentPosition) {
        // Comprueba si la entidad está cerca del nodo siguiente y avanza en la misma dirección
        if (this.path.getNextNodeIndex() + 1 >= this.path.getNodeCount()) {
            return false;
        } else {
            Vec3 nextNodeVec = Vec3.atBottomCenterOf(this.path.getNextNodePos());
            if (!currentPosition.closerThan(nextNodeVec, 2.0D)) {
                return false;
            } else {
                Vec3 followingNodeVec = Vec3.atBottomCenterOf(this.path.getNodePos(this.path.getNextNodeIndex() + 1));
                Vec3 directionToFollowing = followingNodeVec.subtract(nextNodeVec);
                Vec3 currentDirection = currentPosition.subtract(nextNodeVec);
                return directionToFollowing.dot(currentDirection) > 0.0D;
            }
        }
    }

    // Método para configurar si la entidad puede flotar
    public TBSwimmingPathNavigation canFloat(boolean canFloat) {
        this.setCanFloat(canFloat);
        return this;
    }
}
