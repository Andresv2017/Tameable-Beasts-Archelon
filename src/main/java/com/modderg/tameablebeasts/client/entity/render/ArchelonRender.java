package com.modderg.tameablebeasts.client.entity.render;

import com.modderg.tameablebeasts.client.entity.model.ArchelonModel;
import com.modderg.tameablebeasts.client.entity.model.ArgentavisModel;
import com.modderg.tameablebeasts.client.entity.model.ChikoteModel;
import com.modderg.tameablebeasts.server.entity.custom.ArchelonEntity;
import com.modderg.tameablebeasts.server.entity.custom.ArgentavisEntity;
import com.modderg.tameablebeasts.server.entity.custom.ChikoteEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ArchelonRender extends GeoEntityRenderer<ArchelonEntity> {
    public ArchelonRender(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ArchelonModel());
        this.shadowRadius = 0.7f;
    }

    @Override
    public void render(ArchelonEntity entity, float entityYaw, float partialTicks, PoseStack stack, MultiBufferSource bufferIn, int packedLightIn) {
        if(entity.isBaby()){
            stack.scale(1F,1F,1F);
        }else {
            stack.scale(2F,2F,2F);
        }
        super.render(entity, entityYaw, partialTicks, stack, bufferIn, packedLightIn);
    }
}
