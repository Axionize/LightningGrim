package ac.grim.grimac.predictionengine;

import ac.grim.grimac.manager.LastInstanceManager;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntitySelf;
import ac.grim.grimac.utils.latency.CompensatedEntities;
import ac.grim.grimac.utils.latency.CompensatedFireworks;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VehicleCollisionTest {
    private static PacketEventsAPI<?> previousApi;

    @BeforeAll
    static void initializeEntityMappings() {
        previousApi = PacketEvents.getAPI();
        PacketEventsAPI<?> api = mock(PacketEventsAPI.class);
        when(api.getSettings()).thenReturn(new PacketEventsSettings());
        PacketEvents.setAPI(api);
    }

    @AfterAll
    static void restorePacketEvents() {
        PacketEvents.setAPI(previousApi);
    }

    @Test
    void indirectPassengersDoNotRefreshBoatCollisionLenience() throws Exception {
        Fixture fixture = new Fixture();
        PacketEntity pig = fixture.addEntity(2, EntityTypes.PIG);
        PacketEntity passenger = fixture.addEntity(3, EntityTypes.PLAYER);
        pig.mount(fixture.boat);
        passenger.mount(pig);

        assertSame(fixture.boat, passenger.getRootVehicle());
        fixture.handler.checkForHardCollision();

        assertFalse(fixture.handler.lastHardCollidingLerpingEntity.hasOccurredSince(0));

        passenger.eject();
        fixture.handler.checkForHardCollision();

        assertTrue(fixture.handler.lastHardCollidingLerpingEntity.hasOccurredSince(0));
    }

    @Test
    void nearbyEntitiesOutsideTheVehicleStackStillGiveCollisionLenience() throws Exception {
        Fixture fixture = new Fixture();
        fixture.addEntity(2, EntityTypes.PLAYER);

        fixture.handler.checkForHardCollision();

        assertTrue(fixture.handler.lastHardCollidingLerpingEntity.hasOccurredSince(0));
    }

    private static final class Fixture {
        private final GrimPlayer player = mock(GrimPlayer.class);
        private final CompensatedEntities entities = mock(CompensatedEntities.class);
        private final Int2ObjectOpenHashMap<PacketEntity> entityMap = new Int2ObjectOpenHashMap<>();
        private final PacketEntity boat;
        private final UncertaintyHandler handler;

        private Fixture() throws Exception {
            setField(player, "compensatedEntities", entities);
            setField(player, "lastInstanceManager", mock(LastInstanceManager.class));
            setField(player, "fireworks", mock(CompensatedFireworks.class));
            setField(entities, "entityMap", entityMap);
            player.boundingBox = new SimpleCollisionBox(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
            entities.self = new PacketEntitySelf(player) {
                @Override
                protected void initAttributes(GrimPlayer ignored) {}
            };
            boat = addEntity(1, EntityTypes.BOAT);
            entities.self.mount(boat);
            handler = new UncertaintyHandler(player);
            setField(player, "uncertaintyHandler", handler);
        }

        private PacketEntity addEntity(int id, EntityType type) {
            PacketEntity entity = new PacketEntity(player, type) {
                @Override
                protected void initAttributes(GrimPlayer ignored) {}

                @Override
                public SimpleCollisionBox getPossibleCollisionBoxes() {
                    return new SimpleCollisionBox(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
                }
            };
            entityMap.put(id, entity);
            return entity;
        }
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
