package ac.grim.grimac.utils.data.packetentity;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

public abstract class TypedPacketEntity {

    private final EntityType type;
    private final boolean isLiving, isMinecart, isHorse, isAgeable, isAnimal, isBoat;

    public TypedPacketEntity(EntityType type) {
        this.type = type;
        this.isLiving = EntityTypes.isTypeInstanceOf(type, EntityTypes.LIVINGENTITY);
        this.isMinecart = EntityTypes.isTypeInstanceOf(type, EntityTypes.MINECART_ABSTRACT);
        this.isHorse = EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_HORSE);
        this.isAgeable = EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_AGEABLE);
        this.isAnimal = EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_ANIMAL);
        this.isBoat = EntityTypes.isTypeInstanceOf(type, EntityTypes.BOAT);
    }

    public boolean isLivingEntity() {
        return isLiving;
    }

    public boolean isMinecart() {
        return isMinecart;
    }

    public boolean isHorse() {
        return isHorse;
    }

    public boolean isAgeable() {
        return isAgeable;
    }

    public boolean isAnimal() {
        return isAnimal;
    }

    public boolean isBoat() {
        return isBoat;
    }

    // Mojang makes this default to true and overrides it for everything where it isn't
    // That's too much work for us to replicate...
    // This is temporary hack and technically wrong
    /* By Default every entity in the game cannot be hit by player crosshair. This is overwritten as follows:
      Most Boats, Minecart's, TNT, Falling Blocks, and LivingEntities can only be hit if they're not removed
      Every single BlockAttachedEntity can be hit (Leashes and other decorations)
      End Crystals and IntersecetionEntities can be hit
      Ender Dragon entity itself cannot be hit but its parts can be
      ArmorStands can only be hit if they're not removed AND they're not markers.
      Of all Projectiles, only redirectable ones (Fireballs - not blaze fireballs, Wind Charge, and Breeze Wind charges) can be hit
      Persistent Projectiles can only be hit if they're not on the ground and redirectable
    */
    // TLDR If we want to get 90% of the way there everything can be hit except for fishing rod bobbers, arrows, and marker armor stands
    public boolean canHit() { return true; }

    public boolean isPushable() {
        // Players can only push living entities
        // Minecarts and boats are the only non-living that can push
        // Bats, parrots, and armor stands cannot
        if (type == EntityTypes.ARMOR_STAND || type == EntityTypes.BAT || type == EntityTypes.PARROT) return false;
        return isLiving || isBoat || isMinecart;
    }

    public EntityType getType() {
        return type;
    }
}
