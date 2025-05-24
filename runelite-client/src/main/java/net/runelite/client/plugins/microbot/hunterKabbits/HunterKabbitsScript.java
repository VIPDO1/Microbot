package net.runelite.client.plugins.microbot.hunterKabbits;

import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.concurrent.TimeUnit;

public class HunterKabbitsScript extends Script {
    public static final int FALCON_GLOVES = 10023;
    public static final int FALCON_ON_GLOVES = 10024;

    public static int KebbitCaught;
    public boolean hasDied = false;

    public void run(HunterKebbitsConfig config, HunterKebbitsPlugin plugin) {
        Rs2Antiban.resetAntibanSettings();
        applyAntiBanSettings();
        Rs2Antiban.setActivity(Activity.GENERAL_HUNTER);

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;
                if (!Microbot.isLoggedIn()) return;
                if (!this.isRunning()) return;

                KebbitHunting kebbitType = getKebbit(config);

                if (!isNearKebbitArea(kebbitType)) {
                    Microbot.log("Walking to " + kebbitType.getName() + " area...");
                    Rs2Walker.walkTo(kebbitType.getHuntingPoint());
                    return;
                }

                int equippedId = Rs2Equipment.get(EquipmentInventorySlot.WEAPON).getId();

// FALL 1: Falcon verfügbar → nach Kebbit suchen und fangen
                if (equippedId == FALCON_GLOVES) {
                    Rs2NpcModel kebbit = Rs2Npc.getNpc(kebbitType.getNpcName());
                    if (kebbit != null && Rs2Player.getAnimation() == -1) {
                        Microbot.log("Catching " + kebbit.getName());
                        Rs2Npc.interact(kebbit, "Catch"); // nutzt virtuelle Maus

                        // warte, bis der Falcon geworfen wurde
                        Global.sleepUntil(() -> Rs2Player.getAnimation() != -1, 2000);

                        // Falcon fliegt → warte einige Sekunden
                        Global.sleep(config.MinSleepAfterHuntingKebbit(), config.MaxSleepAfterHuntingKebbit());
                    }

// FALL 2: Falcon ist draußen → versuche ihn zurückzuholen
                } else if (equippedId == FALCON_ON_GLOVES) {
                    Rs2NpcModel falcon = Rs2Npc.getNpc("Gyr Falcon");
                    if (falcon != null) {
                        Microbot.log("Retrieving falcon...");
                        Rs2Npc.interact(falcon, "Retrieve"); // virtuelle Maus

                        // warte, bis Glove wieder da
                        boolean success = Global.sleepUntil(() ->
                                Rs2Equipment.get(EquipmentInventorySlot.WEAPON).getId() == FALCON_GLOVES, 3000);

                        if (success) {
                            KebbitCaught++;
                            Global.sleep(config.minSleepAfterCatch(), config.maxSleepAfterCatch());
                        } else {
                            Microbot.log("Falcon konnte nicht rechtzeitig zurückgerufen werden");
                        }
                    }
                } else {
                    Microbot.log("Kein Falconer's glove gefunden!");
                }


            } catch (Exception ex) {
                Microbot.log("Kebbit Script Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    private boolean isNearKebbitArea(KebbitHunting kebbitType) {
        WorldPoint currentLocation = Rs2Player.getWorldLocation();
        return currentLocation.distanceTo(kebbitType.getHuntingPoint()) <= 20;
    }

    private KebbitHunting getKebbit(HunterKebbitsConfig config) {
        if (config.progressiveHunting()) {
            return getBestKebbit();
        }
        return config.kebbitHunting();
    }

    private KebbitHunting getBestKebbit() {
        int level = Microbot.getClient().getRealSkillLevel(Skill.HUNTER);
        if (level >= 69) return KebbitHunting.DASHING;
        if (level >= 57) return KebbitHunting.DARK;
        if (level >= 43) return KebbitHunting.SPOTTED;
        if (level >= 33) return KebbitHunting.BARBTAILED;

        Microbot.log("Not high enough hunter level for any kebbit.");
        shutdown();
        return null;
    }

    private void applyAntiBanSettings() {
        Rs2AntibanSettings.antibanEnabled = true;
        Rs2AntibanSettings.usePlayStyle = true;
        Rs2AntibanSettings.simulateFatigue = true;
        Rs2AntibanSettings.simulateAttentionSpan = true;
        Rs2AntibanSettings.behavioralVariability = true;
        Rs2AntibanSettings.nonLinearIntervals = true;
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.simulateMistakes = true;
        Rs2AntibanSettings.moveMouseOffScreen = true;
        Rs2AntibanSettings.contextualVariability = true;
        Rs2AntibanSettings.devDebug = false;
        Rs2AntibanSettings.playSchedule = true;
        Rs2AntibanSettings.actionCooldownChance = 0.1;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        KebbitCaught = 0;
    }
}
