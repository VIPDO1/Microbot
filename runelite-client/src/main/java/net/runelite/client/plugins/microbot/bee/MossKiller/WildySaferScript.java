package net.runelite.client.plugins.microbot.bee.MossKiller;

import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.magic.Rs2CombatSpells;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.models.RS2Item;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.runelite.api.EquipmentInventorySlot.AMMO;
import static net.runelite.api.EquipmentInventorySlot.WEAPON;
import static net.runelite.api.ItemID.*;
import static net.runelite.api.NpcID.MOSS_GIANT_2093;
import static net.runelite.api.Skill.DEFENCE;
import static net.runelite.api.Skill.WOODCUTTING;
import static net.runelite.client.plugins.microbot.bee.MossKiller.Enums.AttackStyle.MAGIC;
import static net.runelite.client.plugins.microbot.bee.MossKiller.Enums.AttackStyle.RANGE;
import static net.runelite.client.plugins.microbot.util.npc.Rs2Npc.getNpcs;
import static net.runelite.client.plugins.microbot.util.walker.Rs2Walker.*;
import static net.runelite.client.plugins.skillcalculator.skills.MagicAction.HIGH_LEVEL_ALCHEMY;

public class WildySaferScript extends Script {
    
    @Inject
    MossKillerPlugin mossKillerPlugin;

    @Inject
    WildyKillerScript wildyKillerScript;

    @Inject
    MossKillerScript mossKillerScript;

    @Inject
    Client client;

    @Inject
    private MossKillerConfig mossKillerConfig;

    private static MossKillerConfig config;

    @Inject
    public WildySaferScript(MossKillerConfig config) {
        WildySaferScript.config = config;
    }

    private static int[] LOOT_LIST = new int[]{MOSSY_KEY, LAW_RUNE, AIR_RUNE, COSMIC_RUNE, CHAOS_RUNE, DEATH_RUNE, NATURE_RUNE, UNCUT_RUBY, UNCUT_DIAMOND, MITHRIL_ARROW};
    private static int[] ALCHABLES = new int[]{STEEL_KITESHIELD, MITHRIL_SWORD, BLACK_SQ_SHIELD};

    private static final WorldArea SAFE_ZONE_AREA = new WorldArea(3130, 3822, 30, 20, 0);
    public static final WorldPoint SAFESPOT = new WorldPoint(3137, 3833, 0);
    public static final WorldPoint SAFESPOT1 = new WorldPoint(3137, 3831, 0);

    public boolean fired = false;
    public boolean move = false;
    public boolean safeSpot1Attack = false;
    public boolean iveMoved = false;

    public int playerCounter = 0;

    public static boolean test = false;
    public boolean run(MossKillerConfig config) {
        if (mainScheduledFuture != null && !mainScheduledFuture.isCancelled() && !mainScheduledFuture.isDone()) {
            Microbot.log("Scheduled task already running.");
            return false;
        }
        Microbot.enableAutoRunOn = false;
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.simulateMistakes = true;
        Rs2Antiban.setActivityIntensity(ActivityIntensity.MODERATE);
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) {
                    Microbot.log("Not logged in, skipping tick.");
                    return;}
                if (!super.run()) {Microbot.log("super.run() returned false, skipping tick.");
                    if (Microbot.isLoggedIn() && !Rs2Player.isInCombat() && BreakHandlerScript.breakIn <= 1) {Rs2Player.logout();}
                    return;}
                if (Microbot.getClient() == null || Microbot.getClient().getLocalPlayer() == null) {
                    Microbot.log("Client or local player not ready. Skipping tick.");
                    return;
                }
                long startTime = System.currentTimeMillis();

                if (mossKillerPlugin.startedFromScheduler) {prepareSchedulerStart();
                    mossKillerPlugin.startedFromScheduler = false;}

                if (mossKillerPlugin.preparingForShutdown) {
                    MossKillerScript.prepareSoftStop();}

                if (mossKillerPlugin.windStrikeflag) {
                    Rs2Combat.setAutoCastSpell(Rs2CombatSpells.FIRE_STRIKE, config.forceDefensive());
                    mossKillerPlugin.windStrikeflag = false;
                }

                if (Rs2Inventory.contains(MOSSY_KEY)) {
                    doBankingLogic();
                }

                if (fired) {
                    Rs2Bank.walkToBank();
                    Rs2Player.logout();
                    fired = false;
                }

                //if you're at moss giants and your inventory is not prepared, prepare inventory
                if (isInMossGiantArea() && !isInventoryPreparedMage()) {
                    if (config.attackStyle() == MAGIC) {doBankingLogic();}
                    if (config.attackStyle() == RANGE) {if (!isInventoryPreparedArcher()) {doBankingLogic();}}
                    if (config.attackStyle() == RANGE) {if (!equipmentIsPrepared()) {doBankingLogic();}}
                }

                //if you're not at moss giants and your inventory is not prepared, prepare inventory
                if (!isInMossGiantArea() && !isInventoryPreparedMage()) {
                    if (config.attackStyle() == MAGIC) {doBankingLogic();}
                    if (config.attackStyle() == RANGE) {if (!isInventoryPreparedArcher()) {doBankingLogic();}}
                }
                // If you're not at moss giants but have prepared inventory, go to moss giants
                if (!isInMossGiantArea() && equipmentIsPrepared()) {
                    System.out.println("not in moss giant area but we are prepared");
                    if (config.attackStyle() == MAGIC && isEquippedWithRequiredItems() && isInventoryPreparedMage()) {walkTo(SAFESPOT);} else if (config.attackStyle() == MAGIC){doBankingLogic();}
                    if (config.attackStyle() == RANGE && isEquippedWithRequiredItemsRange() && isInventoryPreparedArcher()) {walkTo(SAFESPOT);}
                    // if you're not at moss giants but don't have prepared inventory, prepare inventory
                }

                if (!isInMossGiantArea() && isInventoryPreparedMage() && !equipmentIsPrepared()) {
                    doBankingLogic();
                    return;
                }

                if (!isInMossGiantArea() && isInventoryPreparedArcher() && !equipmentIsPrepared()) {
                    doBankingLogic();
                    return;
                }

                if (!isInMossGiantArea() && !isInventoryPreparedArcher() && !equipmentIsPrepared()) {
                    doBankingLogic();
                    return;
                }

                // If at safe area of moss giants and there is items to loot, loot them
                if (isInMossGiantArea() && itemsToLoot() && !isAnyMossGiantInteractingWithMe()) {
                    lootItems();
                    if (config.attackStyle() == RANGE && Rs2Inventory.contains(MITHRIL_ARROW)) {
                        System.out.println("getting here?");
                        Rs2Inventory.interact(MITHRIL_ARROW, "wield");
                    }
                }
                // If not at the safe spot but in the safe zone area, go to the safe spot
                if (!isAtSafeSpot() && !iveMoved && isInMossGiantArea()) {
                    System.out.println("not at safe spot but in moss giant area");
                    walkFastCanvas(SAFESPOT);
                    sleep(1200,2000);
                    return;
                }

                //if using magic make sure autocast is on
                if (config.attackStyle() == MAGIC && Rs2Equipment.isWearing(STAFF_OF_FIRE)
                        && !mossKillerPlugin.getAttackStyle()) {
                    if (!config.forceDefensive()){
                        Rs2Combat.setAutoCastSpell(Rs2CombatSpells.FIRE_STRIKE, false);}
                    else Rs2Combat.setAutoCastSpell(Rs2CombatSpells.FIRE_STRIKE, true);
                }

                //if using magic make sure staff is equipped
                if (config.attackStyle() == MAGIC && !Rs2Equipment.isWearing(STAFF_OF_FIRE) && Rs2Inventory.contains(STAFF_OF_FIRE)) {
                    Rs2Inventory.equip(STAFF_OF_FIRE);
                }
                //if using magic make sure staff you have a staff in your possesion
                if (config.attackStyle() == MAGIC && !Rs2Equipment.isWearing(STAFF_OF_FIRE) && !Rs2Inventory.contains(STAFF_OF_FIRE)) {
                    doBankingLogic();
                }

                Rs2Player.eatAt(70);

                if (Rs2Inventory.contains(NATURE_RUNE) &&
                        !Rs2Inventory.contains(STAFF_OF_FIRE) &&
                        Rs2Inventory.contains(ALCHABLES) &&
                        config.alchLoot()) {

                    if (config.attackStyle() == RANGE && !Rs2Inventory.contains(FIRE_RUNE, 5)) return;

                    if (Rs2Player.getRealSkillLevel(Skill.MAGIC) > 54 && Rs2Magic.canCast(HIGH_LEVEL_ALCHEMY)) {

                        if (Rs2Inventory.contains(STEEL_KITESHIELD)) {
                            Rs2Magic.alch("Steel kiteshield");
                        } else if (Rs2Inventory.contains(BLACK_SQ_SHIELD)) {
                            Rs2Magic.alch("Black sq shield");
                        } else if (Rs2Inventory.contains(MITHRIL_SWORD)) {
                            Rs2Magic.alch("Mithril sword");
                        }

                        Rs2Player.waitForXpDrop(Skill.MAGIC, 10000, false);
                    }
                }
                // if at the safe spot attack the moss giant and run to the safespot
                if (isAtSafeSpot() && !Rs2Player.isInteracting() && desired2093Exists()) {
                    attackMossGiant();
                }

                if (isAtSafeSpot() && move) {
                    walkFastCanvas(SAFESPOT1);
                    sleep(900,1400);
                    Rs2Npc.interact(MOSS_GIANT_2093,"Attack");
                    sleepUntil(() -> Rs2Player.getInteracting() != null);
                    if (Rs2Player.getInteracting() == null) {
                        Microbot.log("We are not interacting with anything, trying to attack again");
                        Rs2Npc.interact(MOSS_GIANT_2093,"Attack");
                    }
                    move = false;
                    iveMoved = true;
                }

                if (adjacentToSafeSpot1()) {
                    walkFastCanvas(SAFESPOT);
                }


                if (!Rs2Player.isInteracting() && iveMoved && isAtSafeSpot1() && !isAnyMossGiantInteractingWithMe()) {
                    System.out.println("ive moved is false");
                    iveMoved = false;
                }

                if (Rs2Player.isInteracting() && isAtSafeSpot1() && isAnyMossGiantInteractingWithMe() && safeSpot1Attack) {
                    walkFastCanvas(SAFESPOT);
                    iveMoved = false;
                }

                if (config.buryBones() && !Rs2Player.isInteracting()) {
                    if (Rs2Inventory.contains(BIG_BONES)) {
                        sleep(100, 1750);
                        Rs2Inventory.interact(BIG_BONES, "Bury");
                        Rs2Player.waitForAnimation();
                    }
                }

                // Check if any players are near and hop if there are
                if (SAFE_ZONE_AREA.contains(Rs2Player.getWorldLocation())) playersCheck();

                if (mossKillerPlugin.isSuperJammed()) {if (Rs2Inventory.items() == null) {Microbot.log("Inventory has returned null, doing banking logic");
                    doBankingLogic();}}

                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;
                System.out.println("Total time for loop " + totalTime);

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    private boolean equipmentIsPrepared() {
        if (config.attackStyle() == MAGIC && Rs2Equipment.isEquipped(STAFF_OF_FIRE, WEAPON)) {
        return true;
        }

        return config.attackStyle() == RANGE && Rs2Equipment.isEquipped(MAPLE_SHORTBOW, WEAPON) && Rs2Equipment.isEquipped(MITHRIL_ARROW, AMMO);
    }

    /// /// reserved for anti-pk logic /// ///

    //int interactingTicks = 0;

    /*public void dealWithPker() {
        if (Rs2Npc.getNpcsForPlayer() == null
                && Rs2Player.getPlayersInCombatLevelRange() != null) {
            Player localPlayer = Rs2Player.getLocalPlayer();
            for (Rs2PlayerModel p : Rs2Player.getPlayersInCombatLevelRange()) {
                if (p != null && p != localPlayer && p.getInteracting() == localPlayer) {
                    interactingTicks++;
                    break;
                }
            }

            if (interactingTicks > 3) {
                fired = true;
            }
        } else {
            interactingTicks = 0; // reset if not in combat
        }
    }*/


    private boolean isInMossGiantArea() {
        return SAFE_ZONE_AREA.contains(Rs2Player.getWorldLocation());
    }

    private boolean attackMossGiant() {
        Rs2NpcModel mossGiant = Rs2Npc.getNpc(MOSS_GIANT_2093);

        if (mossGiant != null && Rs2Player.getWorldLocation() != null) {
            double distance = mossGiant.getWorldLocation().distanceTo(Rs2Player.getWorldLocation());
            System.out.println("Distance to moss giant: " + distance);
        }

        if (!mossGiant.isDead() && isInMossGiantArea() && !Rs2Player.isAnimating()) {
            Rs2Camera.turnTo(mossGiant);
            Rs2Npc.interact(mossGiant, "attack");
            sleep(100,300);
            sleepUntil(Rs2Player::isAnimating);

            if (Rs2Player.isInteracting()) {
                if (!isAtSafeSpot() && !iveMoved && !move && !safeSpot1Attack)
                    sleep(600,900);
                if (!isAtSafeSpot()) walkFastCanvas(SAFESPOT);
                sleepUntil(this::isAtSafeSpot);
                sleepUntil(() -> !Rs2Npc.isMoving(mossGiant));
                if (!mossGiant.isDead() && !Rs2Player.isInteracting()) {
                    Rs2Npc.attack(MOSS_GIANT_2093);
                }
            }

            return true;
        }

        return false;
    }

    private void playersCheck() {
        if(!mossKillerScript.getNearbyPlayers(14).isEmpty()){
            if (ShortestPathPlugin.isStartPointSet()) {setTarget(null);}
            if(playerCounter > 15) {
                Rs2Player.logout();
                sleepUntil(() -> !Microbot.isLoggedIn());
                sleepUntil(Microbot::isLoggedIn);
                return;
            }
            playerCounter++;
        } else {
            playerCounter = 0;
        }
    }

    public boolean isAnyMossGiantInteractingWithMe() {
        Stream<Rs2NpcModel> mossGiantStream = Rs2Npc.getNpcs("Moss giant");

        if (mossGiantStream == null) {
            System.out.println("No Moss Giants found (Stream is null).");
            return false;
        }

        var player = Rs2Player.getLocalPlayer();
        if (player == null) {
            System.out.println("Local player not found!");
            return false;
        }

        String playerName = player.getName();
        System.out.println("Local Player Name: " + playerName);

        List<Rs2NpcModel> mossGiants = mossGiantStream.collect(Collectors.toList());

        for (Rs2NpcModel mossGiant : mossGiants) {
            if (mossGiant != null) {
                var interacting = mossGiant.getInteracting();
                String interactingName = interacting != null ? interacting.getName() : "None";

                System.out.println("Moss Giant interacting with: " + interactingName);

                if (interacting != null && interactingName.equals(playerName)) {
                    System.out.println("A Moss Giant is interacting with YOU!");
                    return true;
                }
            }
        }

        System.out.println("No Moss Giant is interacting with you.");
        return false;
    }

    private boolean itemsToLoot() {
        RS2Item[] items = Rs2GroundItem.getAllFromWorldPoint(5, SAFESPOT);
        System.out.println("is there anything to loot?");
        if (items.length == 0) return false;
        System.out.println("getting past return false");

        for (int lootItem : LOOT_LIST) {
            for (RS2Item item : items) {
                if (item.getItem().getId() == lootItem) {
                    return true; // Lootable item found
                }
            }
        }

        if (config.buryBones()) {
            for (RS2Item item : items) {
                if (item.getItem().getId() == BIG_BONES) {
                    return true;
                }
            }
        }

        if (config.alchLoot()) {
            for (int lootItem : ALCHABLES) {
                for (RS2Item item : items) {
                    if (item.getItem().getId() == lootItem) {
                        return true; // Lootable item found
                    }
                }
            }

        }

        return false; // No lootable items found
    }

    public boolean isAtSafeSpot() {
        WorldPoint playerPos = Rs2Player.getWorldLocation();
        return playerPos.equals(SAFESPOT);
    }

    public boolean isAtSafeSpot1() {
        WorldPoint playerPos = Rs2Player.getWorldLocation();
        return playerPos.equals(SAFESPOT1);
    }

    public boolean adjacentToSafeSpot1() {
        WorldPoint playerPos = Rs2Player.getWorldLocation();
        WorldPoint westOfSafeSpot = new WorldPoint(
                SAFESPOT1.getX() - 1,
                SAFESPOT1.getY(),
                SAFESPOT1.getPlane()
        );
        return playerPos.equals(westOfSafeSpot);
    }

    private void lootItems() {
        if (Rs2Player.getInteracting() == null && !Rs2Player.isInCombat()) {
            System.out.println("entering loot items");
            RS2Item[] items = Rs2GroundItem.getAllFromWorldPoint(5, SAFESPOT);

            if (items == null || items.length == 0) {
                System.out.println("No items found to loot");
                return;
            }

            // Loot items from the predefined list
            for (RS2Item item : items) {
                if (Rs2Inventory.isFull()) {
                    System.out.println("Inventory full, stopping looting");
                    break;
                }

                int itemId = item.getItem().getId();
                boolean itemLooted = false;

                // Check regular loot items
                for (int lootItem : LOOT_LIST) {
                    if (itemId == lootItem) {
                        System.out.println("Looting regular item: " + itemId);
                        Rs2GroundItem.loot(lootItem);
                        sleep(1000, 3000); // Simulate human-like delay
                        itemLooted = true;
                        break;
                    }
                }

                // If we already looted this item, continue to next item
                if (itemLooted) continue;

                // Handle alchables if enabled
                if (config.alchLoot() && !Rs2Inventory.isFull()) {
                    System.out.println("Checking for alchables, config.alchLoot() = " + config.alchLoot());
                    for (int lootItem : ALCHABLES) {
                        if (itemId == lootItem) {
                            System.out.println("Looting alchable: " + itemId);
                            Rs2GroundItem.loot(lootItem);
                            sleep(1000, 3000);
                            itemLooted = true;
                            break;
                        }
                    }
                }

                // If we already looted this item, continue to next item
                if (itemLooted) continue;

                // Handle bones separately if enabled
                if (config.buryBones() && !Rs2Inventory.isFull()) {
                    if (itemId == BIG_BONES) {
                        System.out.println("Looting bones: " + itemId);
                        Rs2GroundItem.loot(BIG_BONES);
                        sleep(1000, 3000);
                    }
                }
            }
        }
        sleep(400,900);
    }

    private boolean desired2093Exists() {
        Stream<Rs2NpcModel> mossGiantsStream = getNpcs(MOSS_GIANT_2093);
        List<Rs2NpcModel> mossGiants = mossGiantsStream.collect(Collectors.toList());

        for (Rs2NpcModel mossGiant : mossGiants) {
            if (SAFE_ZONE_AREA.contains(mossGiant.getWorldLocation())) {
                return true;
            }
        }

        return false;
    }

    private void doBankingLogic() {
        int amuletId = config.rangedAmulet().getItemId();
        int torsoId = config.rangedTorso().getItemId();
        int chapsId = config.rangedChaps().getItemId();
        int capeId = config.cape().getItemId();

        if (config.attackStyle() == RANGE) {
            if (Rs2Bank.isOpen()) {
                Rs2Bank.depositAll();
            }

            if (!Rs2Bank.isOpen()) {
                Rs2Bank.walkToBank();
                Rs2Bank.walkToBankAndUseBank();
                if (!Rs2Bank.isOpen()) {
                Rs2Equipment.unEquip(AMMO);
                Rs2Bank.walkToBankAndUseBank();
                sleep(2000,4000);
            }
            }
        }

        if (config.attackStyle() == RANGE && !Rs2Bank.isOpen()) {
            Rs2Bank.walkToBank();
            Rs2Bank.walkToBankAndUseBank();
            sleep(800,1900);
            if (Rs2Bank.openBank()){
                if (Rs2Bank.isOpen()) {
                sleep(2200,3200); if (Rs2Bank.count(APPLE_PIE) < 16 ||
                    Rs2Bank.count(MITHRIL_ARROW) < config.mithrilArrowAmount() ||
                    !Rs2Bank.hasItem(MAPLE_SHORTBOW)) {

                Microbot.log("Missing required items in the bank. Shutting down script.");
                shutdown(); // Stop script
                return;
            }
        }
            }
        }
        if (config.attackStyle() == MAGIC) {
            if (!Rs2Bank.isOpen()) {
                Rs2Bank.walkToBank();
                Rs2Bank.walkToBankAndUseBank();
                sleepUntil(Rs2Bank::isOpen, 15000);
                if (!Rs2Bank.isOpen()) {
                    Rs2Bank.openBank();
                    Microbot.log("called to open bank twice");
                    sleepUntil(Rs2Bank::isOpen);
                }// Check if required consumables exist in the bank with the correct amounts
                if (Rs2Bank.isOpen()) {
                    if (Rs2Bank.count(APPLE_PIE) < 16 ||
                            Rs2Bank.count(MIND_RUNE) < 750 ||
                            Rs2Bank.count(AIR_RUNE) < 1550 ||
                            !Rs2Bank.hasItem(STAFF_OF_FIRE)) {

                        Microbot.log("Missing required consumables in the bank. Shutting down script.");
                        shutdown(); // Stop script
                        return;
                    }
                }
            }
        }
        sleepUntil(Rs2Bank::isOpen);
        Rs2Bank.depositAll();
        sleep(600);

        // Withdraw required consumables
        if (Rs2Player.getRealSkillLevel(DEFENCE) < 30) {Rs2Bank.withdrawX(APPLE_PIE, 16);} else {Rs2Bank.withdrawX(APPLE_PIE, 8);}
        sleep(300);
        if (config.attackStyle() == MAGIC) {
            Rs2Bank.withdrawX(MIND_RUNE, 750);
            sleep(300);
            Rs2Bank.withdrawX(AIR_RUNE, 1550);
            sleep(300);
            Rs2Bank.withdrawOne(LAW_RUNE);
            sleep(300);

            // Check if equipped with necessary items
            if (!isEquippedWithRequiredItems()) {
                // Ensure all required equipment is in the bank before proceeding
                if (!Rs2Bank.hasItem(AMULET_OF_MAGIC) ||
                        !Rs2Bank.hasItem(STAFF_OF_FIRE) ||
                        !Rs2Bank.hasItem(capeId)) {

                    Microbot.log("Missing required equipment in the bank. Shutting down script.");
                    shutdown();
                    return;
                }

                if (!Rs2Equipment.isNaked()) {
                    Rs2Bank.depositEquipment();
                    sleep(400,900);
                }

                OutfitHelper.equipOutfit(OutfitHelper.OutfitType.NAKED_MAGE);
                Rs2Bank.withdrawAndEquip(STAFF_OF_FIRE);
                Rs2Bank.withdrawAndEquip(capeId);
                sleep(600,900);
            }
        }

        if (config.attackStyle() == RANGE) {

            if (!isEquippedWithRequiredItemsRange()) {
                if (!Rs2Equipment.isNaked()) {
                    Rs2Bank.depositEquipment();
                    Rs2Bank.withdrawX(MITHRIL_ARROW, config.mithrilArrowAmount());
                    sleep(400,800);
                    Rs2Inventory.equip(MITHRIL_ARROW);
                    sleep(300);
                    sleep(400,900);
                }

                int[] equipItems = {
                        amuletId,
                        torsoId,
                        chapsId,
                        capeId,
                        MAPLE_SHORTBOW,
                        LEATHER_VAMBRACES,
                        LEATHER_BOOTS
                };

                for (int itemId : equipItems) {
                    Rs2Bank.withdrawAndEquip(itemId);
                    sleepUntil(() -> Rs2Equipment.isWearing(itemId), 5000);
                }
            }

            Rs2Bank.withdrawX(MITHRIL_ARROW, config.mithrilArrowAmount());
            sleep(400,800);
            Rs2Inventory.equip(MITHRIL_ARROW);
            sleep(300);
            if (Rs2Player.getRealSkillLevel(Skill.MAGIC) > 24) {
                Rs2Bank.withdrawOne(LAW_RUNE);
                sleep(400,800);
                Rs2Bank.withdrawX(AIR_RUNE,3);
                sleep(400,800);
                Rs2Bank.withdrawOne(FIRE_RUNE);
                sleep(400,800);
            }

        }

        if (config.alchLoot() && Rs2Player.getRealSkillLevel(Skill.MAGIC) > 54) {Rs2Bank.withdrawX(NATURE_RUNE, 10);
            if (config.attackStyle() == RANGE && Rs2Player.getRealSkillLevel(Skill.MAGIC) > 54) Rs2Bank.withdrawX(FIRE_RUNE,50);}

        if (Microbot.getClient().getRealSkillLevel(WOODCUTTING) > 56 && Rs2Player.getWorldLocation().getY() < 3520) {
            Rs2Bank.withdrawOne("axe", false);
        }

        Rs2Bank.closeBank();
    }


    private boolean isEquippedWithRequiredItems() {
        int capeId = config.cape().getItemId();
        // Check if player is wearing the required items
        return Rs2Equipment.hasEquipped(AMULET_OF_MAGIC)
                && Rs2Equipment.hasEquipped(STAFF_OF_FIRE)
                && Rs2Equipment.hasEquipped(capeId)
                && Rs2Equipment.hasEquipped(LEATHER_BOOTS)
                && Rs2Equipment.hasEquipped(LEATHER_VAMBRACES);
    }


    private boolean isEquippedWithRequiredItemsRange() {
        int amuletId = config.rangedAmulet().getItemId();
        int torsoId = config.rangedTorso().getItemId();
        int chapsId = config.rangedChaps().getItemId();
        int capeId = config.cape().getItemId();
        return Rs2Equipment.hasEquipped(amuletId)
                && Rs2Equipment.hasEquipped(chapsId)
                && Rs2Equipment.hasEquipped(capeId)
                && Rs2Equipment.hasEquipped(torsoId)
                && Rs2Equipment.hasEquipped(LEATHER_BOOTS)
                && Rs2Equipment.hasEquipped(LEATHER_VAMBRACES);
    }

    private void prepareSchedulerStart() {
            Rs2Bank.walkToBank();
            Rs2Bank.openBank();
            sleepUntil(Rs2Bank::isOpen);
            Rs2Bank.depositAll();
            Rs2Bank.depositEquipment();
            Rs2Bank.closeBank();
            Rs2Bank.walkToBank(BankLocation.FEROX_ENCLAVE);
    }

    private boolean isInventoryPreparedMage() {
        return Rs2Inventory.hasItemAmount(MIND_RUNE, 15) &&
                Rs2Inventory.hasItemAmount(AIR_RUNE, 30) &&
                (Rs2Inventory.hasItemAmount(APPLE_PIE, 1) || Rs2Inventory.hasItemAmount(HALF_AN_APPLE_PIE, 1));
    }

    private boolean isInventoryPreparedArcher() {
        return Rs2Inventory.hasItemAmount(APPLE_PIE, 1) || Rs2Inventory.hasItemAmount(HALF_AN_APPLE_PIE, 1);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        fired = false;
        move = false;
        iveMoved = false;
    }
}

