package ac.grim.grimac.manager;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.api.CheckType;
import ac.grim.grimac.api.dynamic.DefaultUnloadedBehavior;
import ac.grim.grimac.api.dynamic.UnloadedBehavior;
import ac.grim.grimac.checks.impl.aim.AimDuplicateLook;
import ac.grim.grimac.checks.impl.aim.AimModulo360;
import ac.grim.grimac.checks.impl.aim.processor.AimProcessor;
import ac.grim.grimac.checks.impl.badpackets.*;
import ac.grim.grimac.checks.impl.combat.*;
import ac.grim.grimac.checks.impl.crash.*;
import ac.grim.grimac.checks.impl.exploit.ExploitA;
import ac.grim.grimac.checks.impl.exploit.ExploitB;
import ac.grim.grimac.checks.impl.exploit.ExploitC;
import ac.grim.grimac.checks.impl.groundspoof.NoFallA;
import ac.grim.grimac.checks.impl.inventory.*;
import ac.grim.grimac.checks.impl.misc.ClientBrand;
import ac.grim.grimac.checks.impl.misc.FastBreak;
import ac.grim.grimac.checks.impl.misc.GhostBlockMitigation;
import ac.grim.grimac.checks.impl.movement.*;
import ac.grim.grimac.checks.impl.packetorder.*;
import ac.grim.grimac.checks.impl.multiactions.*;
import ac.grim.grimac.checks.impl.post.Post;
import ac.grim.grimac.checks.impl.prediction.DebugHandler;
import ac.grim.grimac.checks.impl.prediction.NoFallB;
import ac.grim.grimac.checks.impl.prediction.OffsetHandler;
import ac.grim.grimac.checks.impl.prediction.Phase;
import ac.grim.grimac.checks.impl.scaffolding.*;
import ac.grim.grimac.checks.impl.velocity.ExplosionHandler;
import ac.grim.grimac.checks.impl.velocity.KnockbackHandler;
import ac.grim.grimac.checks.type.*;
import ac.grim.grimac.checks.type.interfaces.*;
import ac.grim.grimac.events.packets.PacketChangeGameState;
import ac.grim.grimac.events.packets.PacketEntityReplication;
import ac.grim.grimac.events.packets.PacketPlayerAbilities;
import ac.grim.grimac.events.packets.PacketWorldBorder;
import ac.grim.grimac.manager.init.start.SuperDebug;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.GhostBlockDetector;
import ac.grim.grimac.predictionengine.SneakingEstimator;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.update.*;
import ac.grim.grimac.utils.latency.CompensatedCooldown;
import ac.grim.grimac.utils.latency.CompensatedFireworks;
import ac.grim.grimac.utils.latency.CompensatedInventory;
import ac.grim.grimac.utils.team.TeamHandler;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


public class CheckManager {
    private static boolean inited;
    private static final AtomicBoolean initedAtomic = new AtomicBoolean(false);

    private final GrimPlayer player;

    // Fast arrays for iteration
    private PacketCheckI[] packetChecks;
    private PositionCheckI[] positionChecks;
    private RotationCheckI[] rotationChecks;
    private VehicleCheckI[] vehicleChecks;
    private PacketCheckI[] prePredictionChecks;
    private BlockBreakCheckI[] blockBreakChecks;
    private BlockPlaceCheck[] blockPlaceChecks;
    private PostPredictionCheckI[] postPredictionChecks;

    // Lookup map for getting specific checks
    public Map<Class<? extends AbstractCheck>, AbstractCheck> loadedChecks = new HashMap<>();
    public Map<Class<? extends AbstractCheck>, AbstractCheck> unloadedProxies = new HashMap<>();

    public CheckManager(GrimPlayer player) {
        this.player = player;
        // Packet Checks
        Map<Class<? extends PacketCheckI>, PacketCheckI> packetCheckMap = new HashMap<>();
        addCheck(PacketOrderProcessor.class, player.packetOrderProcessor, packetCheckMap);
        addCheck(Reach.class, new Reach(player), packetCheckMap);
        addCheck(HitboxMiss.class, new HitboxMiss(player), packetCheckMap);
        addCheck(HitboxBlock.class, new HitboxBlock(player), packetCheckMap);
        addCheck(HitboxEntity.class, new HitboxEntity(player), packetCheckMap);
        addCheck(PacketEntityReplication.class, new PacketEntityReplication(player), packetCheckMap);
        addCheck(PacketChangeGameState.class, new PacketChangeGameState(player), packetCheckMap);
        addCheck(CompensatedInventory.class, new CompensatedInventory(player), packetCheckMap);
        addCheck(PacketPlayerAbilities.class, new PacketPlayerAbilities(player), packetCheckMap);
        addCheck(PacketWorldBorder.class, new PacketWorldBorder(player), packetCheckMap);
        addCheck(ActionManager.class, player.actionManager, packetCheckMap);
        addCheck(TeamHandler.class, new TeamHandler(player), packetCheckMap);
        addCheck(ClientBrand.class, new ClientBrand(player), packetCheckMap);
        addCheck(NoFallA.class, new NoFallA(player), packetCheckMap);
        addCheck(BadPacketsO.class, new BadPacketsO(player), packetCheckMap);
        addCheck(BadPacketsA.class, new BadPacketsA(player), packetCheckMap);
        addCheck(BadPacketsB.class, new BadPacketsB(player), packetCheckMap);
        addCheck(BadPacketsC.class, new BadPacketsC(player), packetCheckMap);
        addCheck(BadPacketsD.class, new BadPacketsD(player), packetCheckMap);
        addCheck(BadPacketsE.class, new BadPacketsE(player), packetCheckMap);
        addCheck(BadPacketsF.class, new BadPacketsF(player), packetCheckMap);
        addCheck(BadPacketsG.class, new BadPacketsG(player), packetCheckMap);
        addCheck(BadPacketsI.class, new BadPacketsI(player), packetCheckMap);
        addCheck(BadPacketsJ.class, new BadPacketsJ(player), packetCheckMap);
        addCheck(BadPacketsK.class, new BadPacketsK(player), packetCheckMap);
        addCheck(BadPacketsL.class, new BadPacketsL(player), packetCheckMap);
        addCheck(BadPacketsN.class, new BadPacketsN(player), packetCheckMap);
        addCheck(BadPacketsP.class, new BadPacketsP(player), packetCheckMap);
        addCheck(BadPacketsQ.class, new BadPacketsQ(player), packetCheckMap);
        addCheck(BadPacketsR.class, new BadPacketsR(player), packetCheckMap);
        addCheck(BadPacketsS.class, new BadPacketsS(player), packetCheckMap);
        addCheck(BadPacketsT.class, new BadPacketsT(player), packetCheckMap);
        addCheck(BadPacketsU.class, new BadPacketsU(player), packetCheckMap);
        addCheck(BadPacketsW.class, new BadPacketsW(player), packetCheckMap);
        addCheck(BadPacketsY.class, new BadPacketsY(player), packetCheckMap);
        addCheck(InventoryA.class, new InventoryA(player), packetCheckMap);
        addCheck(InventoryB.class, new InventoryB(player), packetCheckMap);
        addCheck(InventoryE.class, new InventoryE(player), packetCheckMap);
        addCheck(InventoryF.class, new InventoryF(player), packetCheckMap);
        addCheck(InventoryG.class, new InventoryG(player), packetCheckMap);
        addCheck(MultiActionsA.class, new MultiActionsA(player), packetCheckMap);
        addCheck(MultiActionsB.class, new MultiActionsB(player), packetCheckMap);
        addCheck(MultiActionsC.class, new MultiActionsC(player), packetCheckMap);
        addCheck(MultiActionsD.class, new MultiActionsD(player), packetCheckMap);
        addCheck(MultiActionsE.class, new MultiActionsE(player), packetCheckMap);
        addCheck(PacketOrderB.class, new PacketOrderB(player), packetCheckMap);
        addCheck(PacketOrderC.class, new PacketOrderC(player), packetCheckMap);
        addCheck(PacketOrderD.class, new PacketOrderD(player), packetCheckMap);
        addCheck(PacketOrderO.class, new PacketOrderO(player), packetCheckMap);
        addCheck(NoSlowB.class, new NoSlowB(player), packetCheckMap);
        addCheck(SetbackBlocker.class, new SetbackBlocker(player), packetCheckMap);
        packetChecks = packetCheckMap.values().toArray(new PacketCheckI[0]);
        loadedChecks.putAll(packetCheckMap);

        // Position Checks
        Map<Class<? extends PositionCheckI>, PositionCheckI> positionCheckMap = new HashMap<>();
        addCheck(PredictionRunner.class, new PredictionRunner(player), positionCheckMap);
        addCheck(CompensatedCooldown.class, new CompensatedCooldown(player), positionCheckMap);
        positionChecks = positionCheckMap.values().toArray(new PositionCheckI[0]);
        loadedChecks.putAll(positionCheckMap);

        // Rotation Checks
        Map<Class<? extends RotationCheckI>, RotationCheckI> rotationCheckMap = new HashMap<>();
        addCheck(AimProcessor.class, new AimProcessor(player), rotationCheckMap);
        addCheck(AimModulo360.class, new AimModulo360(player), rotationCheckMap);
        addCheck(AimDuplicateLook.class, new AimDuplicateLook(player), rotationCheckMap);
        rotationChecks = rotationCheckMap.values().toArray(new RotationCheckI[0]);
        loadedChecks.putAll(rotationCheckMap);

        // Vehicle Checks
        Map<Class<? extends VehicleCheckI>, VehicleCheckI> vehicleCheckMap = new HashMap<>();
        addCheck(VehiclePredictionRunner.class, new VehiclePredictionRunner(player), vehicleCheckMap);
        vehicleChecks = vehicleCheckMap.values().toArray(new VehicleCheckI[0]);
        loadedChecks.putAll(vehicleCheckMap);

        // Pre Prediction Checks
        Map<Class<? extends PacketCheckI>, PacketCheckI> prePredictionCheckMap = new HashMap<>();
        addCheck(TimerCheck.class, new TimerCheck(player), prePredictionCheckMap);
        addCheck(TickTimer.class, new TickTimer(player), prePredictionCheckMap);
        addCheck(CrashA.class, new CrashA(player), prePredictionCheckMap);
        addCheck(CrashB.class, new CrashB(player), prePredictionCheckMap);
        addCheck(CrashC.class, new CrashC(player), prePredictionCheckMap);
        addCheck(CrashD.class, new CrashD(player), prePredictionCheckMap);
        addCheck(CrashE.class, new CrashE(player), prePredictionCheckMap);
        addCheck(CrashF.class, new CrashF(player), prePredictionCheckMap);
        addCheck(CrashG.class, new CrashG(player), prePredictionCheckMap);
        addCheck(CrashH.class, new CrashH(player), prePredictionCheckMap);
        addCheck(ExploitA.class, new ExploitA(player), prePredictionCheckMap);
        addCheck(ExploitB.class, new ExploitB(player), prePredictionCheckMap);
        addCheck(ExploitC.class, new ExploitC(player), prePredictionCheckMap);
        addCheck(VehicleTimer.class, new VehicleTimer(player), prePredictionCheckMap);
        prePredictionChecks = prePredictionCheckMap.values().toArray(new PacketCheckI[0]);
        loadedChecks.putAll(prePredictionCheckMap);

        // Block Break Checks
        Map<Class<? extends BlockBreakCheckI>, BlockBreakCheckI> blockBreakCheckMap = new HashMap<>();
        addCheck(BadPacketsX.class, new BadPacketsX(player), blockBreakCheckMap);
        addCheck(BadPacketsZ.class, new BadPacketsZ(player), blockBreakCheckMap);
        addCheck(FastBreak.class, new FastBreak(player), blockBreakCheckMap);
        blockBreakChecks = blockBreakCheckMap.values().toArray(new BlockBreakCheckI[0]);
        loadedChecks.putAll(blockBreakCheckMap);

        // Block Place Checks
        Map<Class<? extends BlockPlaceCheck>, BlockPlaceCheck> blockPlaceCheckMap = new HashMap<>();
        addCheck(InventoryC.class, new InventoryC(player), blockPlaceCheckMap);
        addCheck(InvalidPlaceA.class, new InvalidPlaceA(player), blockPlaceCheckMap);
        addCheck(InvalidPlaceB.class, new InvalidPlaceB(player), blockPlaceCheckMap);
        addCheck(AirLiquidPlace.class, new AirLiquidPlace(player), blockPlaceCheckMap);
        addCheck(MultiPlace.class, new MultiPlace(player), blockPlaceCheckMap);
        addCheck(MultiActionsF.class, new MultiActionsF(player), blockPlaceCheckMap);
        addCheck(FarPlace.class, new FarPlace(player), blockPlaceCheckMap);
        addCheck(FabricatedPlace.class, new FabricatedPlace(player), blockPlaceCheckMap);
        addCheck(PositionPlace.class, new PositionPlace(player), blockPlaceCheckMap);
        addCheck(PacketOrderN.class, new PacketOrderN(player), blockPlaceCheckMap);
        addCheck(DuplicateRotPlace.class, new DuplicateRotPlace(player), blockPlaceCheckMap);
        addCheck(LineOfSightPlace.class, new LineOfSightPlace(player), blockPlaceCheckMap);
        addCheck(GhostBlockMitigation.class, new GhostBlockMitigation(player), blockPlaceCheckMap);
        blockPlaceChecks = blockPlaceCheckMap.values().toArray(new BlockPlaceCheck[0]);
        loadedChecks.putAll(blockPlaceCheckMap);

        // Post Prediction Checks
        Map<Class<? extends PostPredictionCheckI>, PostPredictionCheckI> postPredictionCheckMap = new HashMap<>();
        addCheck(NegativeTimerCheck.class, new NegativeTimerCheck(player), postPredictionCheckMap);
        addCheck(ExplosionHandler.class, new ExplosionHandler(player), postPredictionCheckMap);
        addCheck(KnockbackHandler.class, new KnockbackHandler(player), postPredictionCheckMap);
        addCheck(GhostBlockDetector.class, new GhostBlockDetector(player), postPredictionCheckMap);
        addCheck(InventoryD.class, new InventoryD(player), postPredictionCheckMap);
        addCheck(Phase.class, new Phase(player), postPredictionCheckMap);
        addCheck(Post.class, new Post(player), postPredictionCheckMap);
        addCheck(PacketOrderA.class, new PacketOrderA(player), postPredictionCheckMap);
        addCheck(PacketOrderE.class, new PacketOrderE(player), postPredictionCheckMap);
        addCheck(PacketOrderF.class, new PacketOrderF(player), postPredictionCheckMap);
        addCheck(PacketOrderG.class, new PacketOrderG(player), postPredictionCheckMap);
        addCheck(PacketOrderH.class, new PacketOrderH(player), postPredictionCheckMap);
        addCheck(PacketOrderI.class, new PacketOrderI(player), postPredictionCheckMap);
        addCheck(PacketOrderJ.class, new PacketOrderJ(player), postPredictionCheckMap);
        addCheck(PacketOrderK.class, new PacketOrderK(player), postPredictionCheckMap);
        addCheck(PacketOrderL.class, new PacketOrderL(player), postPredictionCheckMap);
        addCheck(PacketOrderM.class, new PacketOrderM(player), postPredictionCheckMap);
        addCheck(NoFallB.class, new NoFallB(player), postPredictionCheckMap);
        addCheck(OffsetHandler.class, new OffsetHandler(player), postPredictionCheckMap);
        addCheck(SuperDebug.class, new SuperDebug(player), postPredictionCheckMap);
        addCheck(DebugHandler.class, new DebugHandler(player), postPredictionCheckMap);
        addCheck(EntityControl.class, new EntityControl(player), postPredictionCheckMap);
        addCheck(NoSlowA.class, new NoSlowA(player), postPredictionCheckMap);
        addCheck(NoSlowC.class, new NoSlowC(player), postPredictionCheckMap);
        addCheck(NoSlowD.class, new NoSlowD(player), postPredictionCheckMap);
        addCheck(NoSlowE.class, new NoSlowE(player), postPredictionCheckMap);
        addCheck(MultiInteractA.class, new MultiInteractA(player), postPredictionCheckMap);
        addCheck(MultiInteractB.class, new MultiInteractB(player), postPredictionCheckMap);
        addCheck(SetbackTeleportUtil.class, new SetbackTeleportUtil(player), postPredictionCheckMap);
        addCheck(CompensatedFireworks.class, player.compensatedFireworks, postPredictionCheckMap);
        addCheck(SneakingEstimator.class, new SneakingEstimator(player), postPredictionCheckMap);
        addCheck(LastInstanceManager.class, player.lastInstanceManager, postPredictionCheckMap);
        postPredictionChecks = postPredictionCheckMap.values().toArray(new PostPredictionCheckI[0]);
        loadedChecks.putAll(postPredictionCheckMap);

        init();
    }

    /**
     * Adds a check to the appropriate map if the player is not exempt.
     */
    private <T extends AbstractCheck> void addCheck(
            Class<? extends T> checkClass,  // The class type of the check
            T checkInstance,                // The instance of the check
            Map<Class<? extends T>, T> checkMap // The map to store the check
    ) {
        if (checkMap.get(checkClass) != null) {
            LogUtil.warn("Attempted to add " + checkClass + " twice to the same checktype map for player + " + player.getName() + ", ignoring!");
//        } else if (checkInstance.getCheckName() == null || checkInstance.getCheckName() == "") {
//            LogUtil.warn("Attempted to add a check with no or null name for player " + player.getName() + ", ignoring!");
        } else {
            // existing behaviour has essentially been to always treat checks with null names and as core/non-unloadable and with no exempt permissions
            // should we change how to handle this?
            if (checkInstance.getCheckName() != null) {
                // register permissions here to prevent NPE when checking if player is exempt
                String permissionName = "grim.exempt." + checkInstance.getCheckName().toLowerCase();
                Permission permission = Bukkit.getPluginManager().getPermission(permissionName);
                if (permission == null) {
                    Bukkit.getPluginManager().addPermission(new Permission(permissionName, PermissionDefault.FALSE));
                } else {
                    permission.setDefault(PermissionDefault.FALSE);
                }

                // returns true if the check is exempt for the player
                // Currently only checks for permission, in the future we will not add to map if:
                // 1. Permission exempts player
                // 2. Client version exempts player
                if (!isExempt(checkClass)) {
                    checkMap.put(checkClass, checkInstance);
                }
            } else {
                checkMap.put(checkClass, checkInstance);
            }
        }
    }

    /**
     * Performs a topological sort of checks based on their dependencies
     */
    private List<AbstractCheck> topologicalSort(Collection<AbstractCheck> checks) {
        Map<Class<? extends AbstractCheck>, Set<Class<? extends AbstractCheck>>> graph = new HashMap<>();
        Map<Class<? extends AbstractCheck>, Integer> inDegree = new HashMap<>();

        // Build dependency graph
        for (AbstractCheck check : checks) {
            Class<? extends AbstractCheck> checkClass = check.getClass();
            graph.putIfAbsent(checkClass, new HashSet<>());
            inDegree.putIfAbsent(checkClass, 0);

            // Add loadAfter dependencies
            for (Class<? extends AbstractCheck> dep : check.getLoadAfter()) {
                graph.computeIfAbsent(dep, k -> new HashSet<>()).add(checkClass);
                inDegree.merge(checkClass, 1, Integer::sum);
            }

            // Add loadBefore reverse dependencies
            for (Class<? extends AbstractCheck> dep : check.getLoadBefore()) {
                graph.computeIfAbsent(checkClass, k -> new HashSet<>()).add(dep);
                inDegree.merge(dep, 1, Integer::sum);
            }

            // Add direct dependencies
            for (Class<? extends AbstractCheck> dep : check.getDependencies()) {
                graph.computeIfAbsent(dep, k -> new HashSet<>()).add(checkClass);
                inDegree.merge(checkClass, 1, Integer::sum);
            }
        }

        // Perform topological sort using Kahn's algorithm
        Queue<Class<? extends AbstractCheck>> queue = new LinkedList<>();
        Map<Class<? extends AbstractCheck>, AbstractCheck> checkMap = checks.stream()
                .collect(Collectors.toMap(AbstractCheck::getClass, c -> c));

        inDegree.forEach((check, degree) -> {
            if (degree == 0) queue.add(check);
        });

        List<AbstractCheck> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            Class<? extends AbstractCheck> current = queue.poll();
            AbstractCheck check = checkMap.get(current);
            if (check != null) {
                sorted.add(check);
            }

            for (Class<? extends AbstractCheck> dependent : graph.getOrDefault(current, Collections.emptySet())) {
                inDegree.merge(dependent, -1, Integer::sum);
                if (inDegree.get(dependent) == 0) {
                    queue.add(dependent);
                }
            }
        }

        if (sorted.size() != checks.size()) {
            throw new IllegalStateException("Circular dependency detected in checks");
        }

        return sorted;
    }

    /**
     * Rebuilds all check arrays maintaining dependency order
     */
    private void rebuildCheckArrays() {
        List<AbstractCheck> sorted = topologicalSort(loadedChecks.values());

        // Use lists first since we don't know final size
        List<PacketCheckI> newPacketChecks = new ArrayList<>();
        List<PositionCheckI> newPositionChecks = new ArrayList<>();
        List<RotationCheckI> newRotationChecks = new ArrayList<>();
        List<VehicleCheckI> newVehicleChecks = new ArrayList<>();
        List<PacketCheckI> newPrePredictionChecks = new ArrayList<>();
        List<BlockBreakCheckI> newBlockBreakChecks = new ArrayList<>();
        List<BlockPlaceCheck> newBlockPlaceChecks = new ArrayList<>();
        List<PostPredictionCheckI> newPostPredictionChecks = new ArrayList<>();

        // Single pass, add to all applicable lists
        for (AbstractCheck check : sorted) {
            // A check can be multiple types, so no else-if
            if (check.isCheckType(CheckType.PACKET)) newPacketChecks.add((PacketCheckI) check);
            if (check.isCheckType(CheckType.POSITION)) newPositionChecks.add((PositionCheckI) check);
            if (check.isCheckType(CheckType.ROTATION)) newRotationChecks.add((RotationCheckI) check);
            if (check.isCheckType(CheckType.VEHICLE)) newVehicleChecks.add((VehicleCheckI) check);
            if (check.isCheckType(CheckType.PRE_PREDICTION)) newPrePredictionChecks.add((PacketCheckI) check);
            if (check.isCheckType(CheckType.BLOCK_BREAK)) newBlockBreakChecks.add((BlockBreakCheckI) check);
            if (check.isCheckType(CheckType.BLOCK_PLACE)) newBlockPlaceChecks.add((BlockPlaceCheck) check);
            if (check.isCheckType(CheckType.POST_PREDICTION)) newPostPredictionChecks.add((PostPredictionCheckI) check);

        }

        // Convert lists to arrays atomically
        this.packetChecks = newPacketChecks.toArray(new PacketCheckI[0]);
        this.positionChecks = newPositionChecks.toArray(new PositionCheckI[0]);
        this.rotationChecks = newRotationChecks.toArray(new RotationCheckI[0]);
        this.vehicleChecks = newVehicleChecks.toArray(new VehicleCheckI[0]);
        this.prePredictionChecks = newPrePredictionChecks.toArray(new PacketCheckI[0]);
        this.blockBreakChecks = newBlockBreakChecks.toArray(new BlockBreakCheckI[0]);
        this.blockPlaceChecks = newBlockPlaceChecks.toArray(new BlockPlaceCheck[0]);
        this.postPredictionChecks = newPostPredictionChecks.toArray(new PostPredictionCheckI[0]);
    }

    /**
     * Registers and loads a check, resolving dependencies
     * @throws IllegalStateException if dependencies cannot be satisfied
     */
    public void registerCheck(AbstractCheck check) {
        Class<? extends AbstractCheck> checkClass = check.getClass();

        // Verify dependencies are loaded
        for (Class<? extends AbstractCheck> dep : check.getDependencies()) {
            if (!isCheckLoaded(dep)) {
                throw new IllegalStateException("Missing required dependency: " + dep.getSimpleName());
            }
        }

        // Load the check
        if (!check.onLoad()) {
            throw new IllegalStateException("Failed to load " + checkClass.getSimpleName());
        }

        loadedChecks.put(checkClass, check);
        rebuildCheckArrays();
    }

    private boolean isCheckLoaded(Class<? extends AbstractCheck> dep) {
        return loadedChecks.containsKey(dep);
    }

    /**
     * Unloads a check and its dependents
     */
    public void unregisterCheck(Class<? extends AbstractCheck> checkClass) {
        AbstractCheck check = loadedChecks.remove(checkClass);
        if (check != null) {
            // Unload dependents first
            for (AbstractCheck other : loadedChecks.values()) {
                if (other.getDependencies().contains(checkClass)) {
                    unregisterCheck(other.getClass());
                }
            }
            check.onUnload();
            rebuildCheckArrays();
        }
    }

    /**
     * Reloads a check and its dependents
     */
    public void reloadCheck(Class<? extends AbstractCheck> checkClass) {
        AbstractCheck check = loadedChecks.get(checkClass);
        if (check != null) {
            unregisterCheck(checkClass);
            registerCheck(check);
        }
    }

    /**
     * Check if a player is exempt from a specific check type.
     */
    private boolean isExempt(Class<? extends AbstractCheck> checkClass) {
        // Example logic for exemptions
        String permission = "grim.exempt." + checkClass.getSimpleName().toLowerCase();
        return
//                this.player.bukkitPlayer.hasPermission(permission) ||
                !checkClassAppliesToPlayerVersion(checkClass);
    }

    private boolean checkClassAppliesToPlayerVersion(Class<? extends AbstractCheck> checkClass) {
        // Example: Logic to determine if a check is compatible with the player's version
        return true; // Replace with actual version check logic
    }

    @SuppressWarnings("unchecked")
    public <T extends PositionCheckI> T getPositionCheck(Class<T> check) {
//        return (T) positionCheck.get(check);
        return (T) loadedChecks.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends RotationCheckI> T getRotationCheck(Class<T> check) {
//        return (T) rotationCheck.get(check);
        return (T) loadedChecks.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends VehicleCheckI> T getVehicleCheck(Class<T> check) {
//        return (T) vehicleCheck.get(check);
        return (T) loadedChecks.get(check);
    }

    public void onPrePredictionReceivePacket(final PacketReceiveEvent packet) {
        for (PacketCheckI check : prePredictionChecks) {
            check.onPacketReceive(packet);
        }
    }

    public void onPacketReceive(final PacketReceiveEvent packet) {
        for (PacketCheckI check : packetChecks) {
            check.onPacketReceive(packet);
        }
        for (PostPredictionCheckI check : postPredictionChecks) {
            check.onPacketReceive(packet);
        }
        for (BlockPlaceCheck check : blockPlaceChecks) {
            check.onPacketReceive(packet);
        }
        for (BlockBreakCheckI check : blockBreakChecks) {
            check.onPacketReceive(packet);
        }
    }

    public void onPacketSend(final PacketSendEvent packet) {
        for (PacketCheckI check : prePredictionChecks) {
            check.onPacketSend(packet);
        }
        for (PacketCheckI check : packetChecks) {
            check.onPacketSend(packet);
        }
        for (PostPredictionCheckI check : postPredictionChecks) {
            check.onPacketSend(packet);
        }
        for (BlockPlaceCheck check : blockPlaceChecks) {
            check.onPacketSend(packet);
        }
        for (BlockBreakCheckI check : blockBreakChecks) {
            check.onPacketSend(packet);
        }
    }

    public void onPositionUpdate(final PositionUpdate position) {
        for (PositionCheckI check : positionChecks) {
            check.onPositionUpdate(position);
        }
    }

    public void onRotationUpdate(final RotationUpdate rotation) {
        for (RotationCheckI check : rotationChecks) {
            check.process(rotation);
        }
        for (BlockPlaceCheck check : blockPlaceChecks) {
            check.process(rotation);
        }
    }

    public void onVehiclePositionUpdate(final VehiclePositionUpdate update) {
        for (VehicleCheckI check : vehicleChecks) {
            check.process(update);
        }
    }

    public void onPredictionFinish(final PredictionComplete complete) {
        for (PostPredictionCheckI check : postPredictionChecks) {
            check.onPredictionComplete(complete);
        }
        for (BlockPlaceCheck check : blockPlaceChecks) {
            check.onPredictionComplete(complete);
        }
        for (BlockBreakCheckI check : blockBreakChecks) {
            check.onPredictionComplete(complete);
        }
    }

    public void onBlockPlace(final BlockPlace place) {
        for (BlockPlaceCheck check : blockPlaceChecks) {
            check.onBlockPlace(place);
        }
    }

    public void onPostFlyingBlockPlace(final BlockPlace place) {
        for (BlockPlaceCheck check : blockPlaceChecks) {
            check.onPostFlyingBlockPlace(place);
        }
    }

    public void onBlockBreak(final BlockBreak blockBreak) {
        for (BlockBreakCheckI check : blockBreakChecks) {
            check.onBlockBreak(blockBreak);
        }
    }

    public ExplosionHandler getExplosionHandler() {
        return getPostPredictionCheck(ExplosionHandler.class);
    }

    @SuppressWarnings("unchecked")
    public <T extends PacketCheckI> T getPacketCheck(Class<T> check) {
        return getCheck(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends BlockPlaceCheck> T getBlockPlaceCheck(Class<T> check) {
        return getCheck(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends PacketCheckI> T getPrePredictionCheck(Class<T> check) {
        return getCheck(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends PacketCheckI> T getPostPredictionCheck(Class<T> check) {
        return getCheck(check);
    }

    private PacketEntityReplication packetEntityReplication = null;

    public PacketEntityReplication getEntityReplication() {
        if (packetEntityReplication == null) packetEntityReplication = getPacketCheck(PacketEntityReplication.class);
        return packetEntityReplication;
    }

    public NoFallA getNoFall() {
        return getPacketCheck(NoFallA.class);
    }

    private CompensatedInventory inventory = null;

    public CompensatedInventory getInventory() {
        if (inventory == null) inventory = getPacketCheck(CompensatedInventory.class);
        return inventory;
    }

    public KnockbackHandler getKnockbackHandler() {
        return getPostPredictionCheck(KnockbackHandler.class);
    }

    public CompensatedCooldown getCompensatedCooldown() {
        return getPositionCheck(CompensatedCooldown.class);
    }

    public NoSlowA getNoSlow() {
        return getPostPredictionCheck(NoSlowA.class);
    }

    public SetbackTeleportUtil getSetbackUtil() {
        return getPostPredictionCheck(SetbackTeleportUtil.class);
    }

    public DebugHandler getDebugHandler() {
        return getPostPredictionCheck(DebugHandler.class);
    }

    public OffsetHandler getOffsetHandler() {
        return getPostPredictionCheck(OffsetHandler.class);
    }

    /**
     * Gets a check instance, creating an unloaded proxy if the check isn't loaded.
     * This ensures calls to unloaded checks fail gracefully rather than throwing exceptions.
     *
     * @param checkClass The class of check to retrieve
     * @return The check instance or an unloaded proxy
     */
    // Type-safe check access with unloaded handling
    @SuppressWarnings("unchecked")
    public <T extends AbstractCheck> T getCheck(Class<T> checkClass) {
        T check = (T) loadedChecks.get(checkClass);
        if (check == null) {
            return (T) unloadedProxies.computeIfAbsent(checkClass,
                    this::createUnloadedProxy);
        }
        return check;
    }

    /**
     * Creates a proxy that handles calls to an unloaded check.
     * The proxy uses the check's provider to determine unloaded behavior.
     *
     * @param checkClass The check class to create a proxy for
     * @return A proxy implementing the check's interface
     */
    private <T extends AbstractCheck> T createUnloadedProxy(Class<T> checkClass) {
        // Get the unloaded behavior from a cached instance or use default
        UnloadedBehavior behavior = DefaultUnloadedBehavior.INSTANCE;

        return (T) Proxy.newProxyInstance(
                checkClass.getClassLoader(),
                new Class<?>[] { checkClass },
                (proxy, method, args) -> behavior.handleUnloadedCall(method, args)
        );
    }

    private void init() {
        // Fast non-thread safe check
        if (inited) return;
        // Slow thread safe check
        if (!initedAtomic.compareAndSet(false, true)) return;
        inited = true;
    }
}
