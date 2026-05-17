package dev.rinchan.paperplane;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PlayerEntry(UUID id, String name) {
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerEntry> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        PlayerEntry::id,
        ByteBufCodecs.STRING_UTF8,
        PlayerEntry::name,
        PlayerEntry::new
    );
}
