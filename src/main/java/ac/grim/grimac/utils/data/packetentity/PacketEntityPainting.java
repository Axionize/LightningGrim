package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
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

    // Make default hitbox basically a point that can never be hit; temporary hack until we can model paintings in every version
    public SimpleCollisionBox paintingHitBox = new SimpleCollisionBox(trackedServerPosition.getPos().x, trackedServerPosition.getPos().y, trackedServerPosition.getPos().z,
            trackedServerPosition.getPos().x, trackedServerPosition.getPos().y, trackedServerPosition.getPos().z);

    private int width, height;

    public PacketEntityPainting(GrimPlayer player, UUID uuid, double x, double y, double z, Direction direction) {
        super(player, uuid, EntityTypes.PAINTING, x, y, z);
        this.side = direction;
    }

    public SimpleCollisionBox calculateBoundingBoxDimensions(int width, int height) {
        if (this.width == width && this.height == height) return paintingHitBox;

        this.width = width;
        this.height = height;

        float f = 0.46875F;
        System.out.println("f: " + f);

        Vector3d trackedServerPositionVector = this.trackedServerPosition.getPos();
        Vector3i attachedBlockPos = new Vector3i(GrimMath.floor(trackedServerPositionVector.getX()),
                GrimMath.floor(trackedServerPositionVector.getY()),
                GrimMath.floor(trackedServerPositionVector.getZ()));
        Vector3d vec3d = attachedBlockPos.toVector3d().add(0.5, 0.5, 0.5);

        System.out.println("Initial vec3d (center of block): " + vec3d);
        System.out.println("Side: " + side);

        vec3d = offset(vec3d, side, -f);
        System.out.println("vec3d after offset to back: " + vec3d);

        double d = this.getOffset(width);
        double e = this.getOffset(height);
        System.out.println("d (horizontal offset): " + d + ", e (vertical offset): " + e);

        Direction direction = rotateYCounterclockwise(side);
        System.out.println("Perpendicular direction: " + direction);

        Vector3d vec3d2 = offset(vec3d, direction, d);
        System.out.println("vec3d2 after horizontal offset: " + vec3d2);
        vec3d2 = offset(vec3d2, Direction.UP, e);
        System.out.println("vec3d2 after vertical offset: " + vec3d2);

        Axis axis = this.getAxis(side);
        System.out.println("Axis: " + axis);

        double g = axis == Axis.X ? 0.0625 : (double) width;
        double h = (double) height;
        double i = axis == Axis.Z ? 0.0625 : (double) width;
        System.out.println("g: " + g + ", h: " + h + ", i: " + i);

        SimpleCollisionBox box = new SimpleCollisionBox(
                vec3d2.getX() - g/2, vec3d2.getY() - h/2, vec3d2.getZ() - i/2,
                vec3d2.getX() + g/2, vec3d2.getY() + h/2, vec3d2.getZ() + i/2
        );
        System.out.println("Final box: " + box);
        System.out.println("--- End calculation ---");
        return box;
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
        //return vector3d.add(value * vec3i.getX(), value * vec3i.getY(), value * vec3i.getZ()); //Incorrect
        return new Vector3d(vector3d.getX() + value * vec3i.getX(), vector3d.getY() + value * vec3i.getY(), vector3d.getZ() + value * vec3i.getZ()); //Correct
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
