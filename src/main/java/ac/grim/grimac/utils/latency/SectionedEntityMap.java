package ac.grim.grimac.utils.latency;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.util.Vector3d;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;

public class SectionedEntityMap {

    private final Long2ObjectOpenHashMap<EntitySection> sections = new Long2ObjectOpenHashMap<>();
    private final LongSortedSet trackedSections = new LongAVLTreeSet();

    public void addEntity(PacketEntity entity) {
        Vector3d entityLocation = entity.trackedServerPosition.getPos();
        long sectionPos = GrimMath.asLong(
                GrimMath.getSectionCoord(entityLocation.getX()),
                GrimMath.getSectionCoord(entityLocation.getY()),
                GrimMath.getSectionCoord(entityLocation.getZ())
        );

        EntitySection section = sections.computeIfAbsent(sectionPos, this::createSection);
        section.addEntity(entity);
        trackedSections.add(sectionPos);
    }

    public void removeEntity(PacketEntity entity) {
        Vector3d entityLocation = entity.trackedServerPosition.getPos();
        long sectionPos = GrimMath.asLong(
                GrimMath.getSectionCoord(entityLocation.getX()),
                GrimMath.getSectionCoord(entityLocation.getY()),
                GrimMath.getSectionCoord(entityLocation.getZ())
        );

        EntitySection section = sections.get(sectionPos);
        if (section != null) {
            section.removeEntity(entity);
            if (section.isEmpty()) {
                sections.remove(sectionPos);
                trackedSections.remove(sectionPos);
            }
        }
    }

    public void forEachInBox(SimpleCollisionBox box, Consumer<PacketEntity> action) {
        int minX = GrimMath.getSectionCoord(box.minX - 2.0);
        int minY = GrimMath.getSectionCoord(box.minY - 4.0);
        int minZ = GrimMath.getSectionCoord(box.minZ - 2.0);
        int maxX = GrimMath.getSectionCoord(box.maxX + 2.0);
        int maxY = GrimMath.getSectionCoord(box.maxY);
        int maxZ = GrimMath.getSectionCoord(box.maxZ + 2.0);

        for (int x = minX; x <= maxX; x++) {
            long minPacked = GrimMath.asLong(x, 0, 0);
            long maxPacked = GrimMath.asLong(x, -1, -1);
            LongSortedSet relevantSections = trackedSections.subSet(minPacked, maxPacked + 1);

            for (long sectionPos : relevantSections) {
                int y = GrimMath.unpackY(sectionPos);
                int z = GrimMath.unpackZ(sectionPos);

                if (y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
                    EntitySection section = sections.get(sectionPos);
                    if (section != null) {
                        section.forEachEntity(action);
                    }
                }
            }
        }
    }

    private EntitySection createSection(long pos) {
        return new EntitySection();
    }

    private static class EntitySection {
        private final List<PacketEntity> entities = new ArrayList<>();

        public void addEntity(PacketEntity entity) {
            entities.add(entity);
        }

        public void removeEntity(PacketEntity entity) {
            entities.remove(entity);
        }

        public boolean isEmpty() {
            return entities.isEmpty();
        }

        public void forEachEntity(Consumer<PacketEntity> action) {
            for (PacketEntity entity : entities) { // Iterate in insertion order (oldest first)
                action.accept(entity);
            }
        }

        public List<PacketEntity> getEntities() {
            return entities;
        }
    }
}