package com.modderg.tameablebeasts.client.entity.model;

import com.modderg.tameablebeasts.TameableBeast;
import com.modderg.tameablebeasts.client.entity.TBGeoModel;
import com.modderg.tameablebeasts.server.entity.custom.ArchelonEntity;
import com.modderg.tameablebeasts.server.entity.custom.ArgentavisEntity;
import com.modderg.tameablebeasts.server.entity.custom.ChikoteEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;

public class ArchelonModel extends TBGeoModel<ArchelonEntity> {

    @Override
    public ResourceLocation getModelResource(ArchelonEntity entity) {
        if(!entity.isBaby())
            return new ResourceLocation(TameableBeast.MOD_ID, "geo/archelon.geo.json");
        return new ResourceLocation(TameableBeast.MOD_ID, "geo/baby_archelon.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ArchelonEntity entity) {
        return new ResourceLocation(TameableBeast.MOD_ID, "textures/entity/archelon"+ entity.getTextureID() +".png");
    }

    @Override
    public ResourceLocation getAnimationResource(ArchelonEntity entity) {
        if(entity.isBaby())
            return new ResourceLocation(TameableBeast.MOD_ID, "animations/baby_archelon.anims.json");
        return new ResourceLocation(TameableBeast.MOD_ID, "animations/archelon.anims.json");
    }

    @Override
    public void setCustomAnimations(ArchelonEntity animatable, long instanceId, AnimationState<ArchelonEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
    }
}