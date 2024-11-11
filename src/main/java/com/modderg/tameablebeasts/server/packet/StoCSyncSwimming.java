package com.modderg.tameablebeasts.server.packet;

import com.modderg.tameablebeasts.server.entity.WaterTBAnimal;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import software.bernie.geckolib.util.ClientUtils;

import java.util.function.Supplier;

public class StoCSyncSwimming {

    private final int entityId;
    private final boolean isSwimming;

    // Constructor para crear el paquete
    public StoCSyncSwimming(int entityId, boolean isSwimming) {
        this.entityId = entityId;
        this.isSwimming = isSwimming;
    }

    // Constructor para decodificar el paquete recibido
    public StoCSyncSwimming(FriendlyByteBuf buffer) {
        this.entityId = buffer.readInt();
        this.isSwimming = buffer.readBoolean();
    }

    // Método para codificar el paquete antes de enviarlo//
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeBoolean(isSwimming);
    }

    // Manejo del paquete recibido en el cliente
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Verifica que el nivel y la entidad existen antes de intentar actualizar el estado
            if (ClientUtils.getLevel() != null && ClientUtils.getLevel().getEntity(entityId) instanceof WaterTBAnimal waterAnimal) {
                waterAnimal.setIsSwimming(isSwimming);
            }
        });
        context.get().setPacketHandled(true); // Marcar el paquete como manejado
    }
}



