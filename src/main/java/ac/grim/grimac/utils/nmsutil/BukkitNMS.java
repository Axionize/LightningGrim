package ac.grim.grimac.utils.nmsutil;

import com.github.retrooper.packetevents.PacketEvents;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Predicate;

import static com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_12_2;

@UtilityClass
public class BukkitNMS {
    // resets item usage, then returns whether the player was using an item
    private final @NotNull Predicate<@NotNull Player> resetActiveBukkitItem;

    public void resetBukkitItemUsage(@Nullable Player player) {
        if (player != null && resetActiveBukkitItem.test(player)) {
            // only update if they were using an item to prevent certain issues
            player.updateInventory();
        }
    }

    static {
        try {
            Class<?> EntityLiving;
            Method getHandle;
            Method clearActiveItem;
            Method getItemInUse;
            Method isUsingItem;
            Method isEmpty;
            switch (PacketEvents.getAPI().getServerManager().getVersion()) {
                case V_1_8_8:
                    Class<?> EntityHuman = Class.forName("net.minecraft.server.v1_8_R3.EntityHuman");
                    getHandle = Class.forName("org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer").getMethod("getHandle");
                    clearActiveItem = EntityHuman.getMethod("bV");
                    isUsingItem = EntityHuman.getMethod("bS");

                    resetActiveBukkitItem = player -> {
                        try {
                            Object handle = getHandle.invoke(player);
                            clearActiveItem.invoke(handle);
                            return (boolean) isUsingItem.invoke(handle);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    };
                    break;
                case V_1_12_2:
                    EntityLiving = Class.forName("net.minecraft.server.v1_12_R1.EntityLiving");
                    getHandle = Class.forName("org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer").getMethod("getHandle");
                    clearActiveItem = EntityLiving.getMethod("cN");
                    getItemInUse = EntityLiving.getMethod("cJ");
                    isEmpty = Class.forName("net.minecraft.server.v1_12_R1.ItemStack").getMethod("isEmpty");

                    resetActiveBukkitItem = player -> {
                        try {
                            Object handle = getHandle.invoke(player);
                            clearActiveItem.invoke(handle);
                            Object item = getItemInUse.invoke(handle);
                            return item != null && !((boolean) isEmpty.invoke(item));
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    };
                    break;
                case V_1_16_5:
                    EntityLiving = Class.forName("net.minecraft.server.v1_16_R3.EntityLiving");
                    getHandle = Class.forName("org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer").getMethod("getHandle");
                    clearActiveItem = EntityLiving.getMethod("clearActiveItem");
                    getItemInUse = EntityLiving.getMethod("getActiveItem");
                    isEmpty = Class.forName("net.minecraft.server.v1_16_R3.ItemStack").getMethod("isEmpty");

                    resetActiveBukkitItem = player -> {
                        try {
                            Object handle = getHandle.invoke(player);
                            clearActiveItem.invoke(handle);
                            Object item = getItemInUse.invoke(handle);
                            return item != null && !((boolean) isEmpty.invoke(item));
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    };
                    break;
                default:
                    // cause an exception if these methods don't exist
                    clearActiveItem = Player.class.getMethod("clearActiveItem");
                    getItemInUse = Player.class.getMethod("getItemInUse");
                    resetActiveBukkitItem = player -> {
                        try {
                            clearActiveItem.invoke(player);
                            return getItemInUse.invoke(player) != null;
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    };
                    break;
            }
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException("you are likely using an unsupported server software and or version!", e);
        }
    }
}
