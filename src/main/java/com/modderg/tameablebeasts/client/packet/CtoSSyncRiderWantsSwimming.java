package com.modderg.tameablebeasts.client.packet;

import com.modderg.tameablebeasts.server.entity.WaterRideableTBAnimal;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CtoSSyncRiderWantsSwimming {

    private final int id;
    private final boolean swimming;

    public CtoSSyncRiderWantsSwimming(int id, boolean swimming) {
        this.id = id;
        this.swimming = swimming;
    }

    public CtoSSyncRiderWantsSwimming(FriendlyByteBuf buffer) {this(buffer.readInt(), buffer.readBoolean());}

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(id);
        buffer.writeBoolean(swimming);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getSender() != null) {
                Entity entity = context.get().getSender().level().getEntity(id);
                if (entity instanceof WaterRideableTBAnimal waterAnimal) {
                    waterAnimal.setRiderWantsSwimming(swimming);
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
