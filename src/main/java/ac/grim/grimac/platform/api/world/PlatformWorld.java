package ac.grim.grimac.platform.api.world;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface PlatformWorld {
    boolean isChunkLoaded(int i, int i1);

    WrappedBlockState getBlockAt(int i, int j, int k);

    String getName();

    @Nullable UUID getUID();

    PlatformChunk getChunkAt(int currChunkX, int currChunkZ);

    boolean isLoaded();
}
