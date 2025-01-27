package ac.grim.grimac.utils.collisions;

/*

 */
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.*;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// Expansion to the CollisionData class, which is different than regular ray tracing hitboxes
public enum RaycastData {

    HOPPER((player, item, version, data, isTarget, x, y, z) -> {
        HexCollisionBox insideShape = new HexCollisionBox(2.0, 11.0, 2.0, 14.0, 16.0, 14.0);
        switch (data.getFacing()) {
            case NORTH:
                new ComplexCollisionBox(insideShape, new HexCollisionBox(6.0, 8.0, 0.0, 10.0, 10.0, 4.0));
            case SOUTH:
                new ComplexCollisionBox(insideShape, new HexCollisionBox(6.0, 8.0, 12.0, 10.0, 10.0, 16.0));
            case WEST:
                new ComplexCollisionBox(insideShape, new HexCollisionBox(0.0, 8.0, 6.0, 4.0, 10.0, 10.0));
            case EAST:
                new ComplexCollisionBox(insideShape, new HexCollisionBox(12.0, 8.0, 6.0, 16.0, 10.0, 10.0));
            default:
                return insideShape;
        }
    }, StateTypes.HOPPER),

    CAULDRON((player, item, version, data, isTarget, x, y, z) -> {
        return new HexCollisionBox(2.0, 4.0, 2.0, 14.0, 16.0, 14.0);
    }, BlockTags.CAULDRONS.getStates().toArray(new StateType[0])),

    FULL_BLOCK((player, item, version, data, isTarget, x, y, z) -> {
        return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);
    }, StateTypes.COMPOSTER, StateTypes.SCAFFOLDING);


    private static final Map<StateType, RaycastData> lookup = new HashMap<>();

    static {
        for (RaycastData data : RaycastData.values()) {
            for (StateType type : data.materials) {
                lookup.put(type, data);
            }
        }
    }

    private final StateType[] materials;
    private CollisionBox box;
    private HitBoxFactory dynamic;

    RaycastData(CollisionBox box, StateType... materials) {
        this.box = box;
        Set<StateType> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new StateType[0]);
    }

    RaycastData(HitBoxFactory dynamic, StateType... materials) {
        this.dynamic = dynamic;
        Set<StateType> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new StateType[0]);
    }

    public static RaycastData getData(StateType material) {
        return lookup.get(material);
    }

    @Nullable
    public static CollisionBox getBlockHitbox(GrimPlayer player, StateType heldItem, ClientVersion version, WrappedBlockState block, int x, int y, int z) {
        RaycastData data = getData(block.getType());

        if (data == null) {
            // We explicitly do not want to fallback on HitBoxData or CollisionBox
            // Because we only ever use this as a 2nd ray trace
            return NoCollisionBox.INSTANCE;
        }

        // Simple collision box to override
        if (data.box != null)
            return data.box.copy().offset(x, y, z);

        // Allow this class to override collision boxes when they aren't the same as regular boxes
        HitBoxFactory hitBoxFactory = data.dynamic;
        CollisionBox collisionBox = hitBoxFactory.fetch(player, heldItem, version, block, false, x, y, z);
        collisionBox.offset(x, y, z);
        return collisionBox;
    }
}
