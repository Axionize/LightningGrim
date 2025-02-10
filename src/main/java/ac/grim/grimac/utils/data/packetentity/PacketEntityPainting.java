package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.Direction;
import com.github.retrooper.packetevents.protocol.world.states.enums.Axis;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import lombok.Getter;

import java.util.UUID;

@Getter
public class PacketEntityPainting extends PacketEntity {

    private final Direction side;
    public SimpleCollisionBox paintingHitBox;

    private int width, height;

    public PacketEntityPainting(GrimPlayer player, UUID uuid, double x, double y, double z, Direction direction) {
        super(player, uuid, EntityTypes.PAINTING, x, y, z);
        this.side = direction;
    }

//    public CollisionBox getMinimumPossibleCollisionBoxes() {
//        return paintingHitBox;
//    }
//
//    public SimpleCollisionBox getPossibleLocationBoxes() {
//        return paintingHitBox;
//    }

    public SimpleCollisionBox calculateBoundingBoxDimensions(int width, int height) {
        if (this.width == width && this.height == height) return paintingHitBox;

        this.width = width;
        this.height = height;

        float f = 0.46875F;

        Vector3d trackedServerPositionVector = this.trackedServerPosition.getPos();
        Vector3i attachedBlockPos = new Vector3i(GrimMath.floor(trackedServerPositionVector.getX()),
                GrimMath.floor(trackedServerPositionVector.getY()),
                GrimMath.floor(trackedServerPositionVector.getZ()));

        Vector3d vec3d = attachedBlockPos.toVector3d().add(0.5, 0.5, 0.5); // get center of block

        this.offset(vec3d, side, -f);
        double d = this.getOffset(width);
        double e = this.getOffset(height);
        Direction direction = rotateYCounterclockwise(side);
        this.offset(vec3d, direction, d);
        Vector3d vec3d2 = this.offset(vec3d, Direction.UP, e);

        Axis axis = this.getAxis(side);
        double g = axis == Axis.X ? 0.0625 : (double) width;
        double h = (double) height;
        double i = axis == Axis.Z ? 0.0625 : (double) width;

        return new SimpleCollisionBox(
                vec3d2.getX() - g/2, vec3d2.getY() - h/2, vec3d2.getZ() - i/2,
                vec3d2.getX() + g/2, vec3d2.getY() + h/2, vec3d2.getZ() + i/2
        );
    }

    private double getOffset(int length) {
        return length % 2 == 0 ? 0.5 : 0.0;
    }

    private Direction rotateYCounterclockwise(Direction side) {
        switch (side) {
            case NORTH:
                return Direction.WEST;
            case SOUTH:
                return Direction.EAST;
            case WEST:
                return Direction.SOUTH;
            case EAST:
                return Direction.NORTH;
            default:
                throw new IllegalStateException("Unable to get CCW facing of " + this);
        }
    }

    private Vector3d offset(Vector3d vector3d, Direction side, double value) {
        Vector3i vec3i = getDirectionVector(side);
        vector3d.add(value * vec3i.getX(), value * vec3i.getY(), value * vec3i.getZ());
        return vector3d;
    }

    private Vector3i getDirectionVector(Direction side) {
        switch (side) {
            case DOWN:
                return new Vector3i(0, -1, 0);
            case UP:
                return new Vector3i(0, 1, 0);
            case NORTH:
                return new Vector3i(0, 0, -1);
            case SOUTH:
                return new Vector3i(0, 0, 1);
            case WEST:
                return new Vector3i(-1, 0, 0);
            case EAST:
                return new Vector3i(1, 0, 0);
            default:
                throw new IllegalStateException("Impossible direction: " + side);
        }
    }

    private Axis getAxis(Direction direction) {
        switch (side) {
            case DOWN:
            case UP:
                return Axis.Y;
            case NORTH:
            case SOUTH:
                return Axis.Z;
            case WEST:
            case EAST:
                return Axis.X;
            default:
                throw new IllegalStateException("Impossible direction: " + side);
        }
    }
}
