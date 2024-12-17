package ac.grim.grimac.utils.latency;

import java.util.*;
import java.util.function.Consumer;

import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.util.Vector3d;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;

public class SectionedEntityMap {

    // exists to make porting over the legacy entity handling easier, will remove later
    private final Int2ObjectOpenHashMap<PacketEntity> idToEntity = new Int2ObjectOpenHashMap<>();

    private final Long2ObjectOpenHashMap<EntitySection> sections = new Long2ObjectOpenHashMap<>();
    private final LongSortedSet trackedSections = new LongAVLTreeSet();

    private final EntityCollection entityCollection = new EntityCollection();

    public PacketEntity get(int entityId) {
        return idToEntity.get(entityId);
    }

    public void addEntity(int entityID, PacketEntity entity) {
        idToEntity.put(entityID, entity);
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

    public void removeEntity(int entityId) {
        PacketEntity entity = idToEntity.remove(entityId);
        if (entity != null) {
            removeEntity(entity);
        }
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

    public Collection<PacketEntity> values() {
        return entityCollection;
    }

    public boolean containsKey(int entityId) {
        return idToEntity.containsKey(entityId);
    }

    public void forEachEntity(Consumer<PacketEntity> action) {
        long minPacked = Long.MIN_VALUE;
        long maxPacked = Long.MAX_VALUE;

        for (long sectionPos : trackedSections.subSet(minPacked, maxPacked)) {
            EntitySection section = sections.get(sectionPos);
            if (section != null) {
                section.forEachEntity(action);
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

    public void updateEntityPosition(PacketEntity entity, Vector3d newPosition) {
        if (entity == null) return; // is null on startup at first
        // Get old and new section positions
        Vector3d oldPosition = entity.trackedServerPosition.getPos();
        long oldSectionPos = GrimMath.asLong(
                GrimMath.getSectionCoord(oldPosition.getX()),
                GrimMath.getSectionCoord(oldPosition.getY()),
                GrimMath.getSectionCoord(oldPosition.getZ())
        );

        long newSectionPos = GrimMath.asLong(
                GrimMath.getSectionCoord(newPosition.getX()),
                GrimMath.getSectionCoord(newPosition.getY()),
                GrimMath.getSectionCoord(newPosition.getZ())
        );

        // If section changed
        if (oldSectionPos != newSectionPos) {
            // Remove from old section
            EntitySection oldSection = sections.get(oldSectionPos);
            if (oldSection != null) {
                oldSection.removeEntity(entity);
                if (oldSection.isEmpty()) {
                    sections.remove(oldSectionPos);
                    trackedSections.remove(oldSectionPos);
                }
            }

            // Add to new section
            EntitySection newSection = sections.computeIfAbsent(newSectionPos, this::createSection);
            newSection.addEntity(entity);
            trackedSections.add(newSectionPos);
        }
    }

    private EntitySection createSection(long pos) {
        return new EntitySection();
    }

    public Iterable<? extends Map.Entry<Integer, PacketEntity>> int2ObjectEntrySet() {
        return idToEntity.entrySet();
    }

    public void put(int entityID, PacketEntity packetEntity) {
        addEntity(entityID, packetEntity);
    }

    public boolean containsValue(PacketEntity entity) {
        return idToEntity.containsValue(entity);
    }

    public void clear() {
        idToEntity.clear();
        sections.clear();
        trackedSections.clear();
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

    private class EntityCollection extends AbstractCollection<PacketEntity> {
        @Override
        public Iterator<PacketEntity> iterator() {
            return new EntityIterator();
        }

        @Override
        public int size() {
            return idToEntity.size();
        }

        @Override
        public boolean contains(Object o) {
            return o instanceof PacketEntity && containsValue((PacketEntity) o);
        }
    }

    private class EntityIterator implements Iterator<PacketEntity> {
        private final Iterator<Long> sectionIterator = trackedSections.iterator();
        private Iterator<PacketEntity> currentSectionIterator = Collections.emptyIterator();

        @Override
        public boolean hasNext() {
            while (!currentSectionIterator.hasNext() && sectionIterator.hasNext()) {
                long sectionPos = sectionIterator.next();
                EntitySection section = sections.get(sectionPos);
                if (section != null) {
                    currentSectionIterator = section.getEntities().iterator();
                }
            }
            return currentSectionIterator.hasNext();
        }

        @Override
        public PacketEntity next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return currentSectionIterator.next();
        }
    }
}
