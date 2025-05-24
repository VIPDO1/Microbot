package net.runelite.client.plugins.microbot.hunterKabbits;

import com.google.common.collect.EvictingQueue;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentLinkedQueue;

enum State {
    CATCHING, RETRIEVING, DROPPING
}

@Slf4j
public class HunterKabbitsScript extends Script {
    public static int KebbitCaught = 0;
    public boolean hasDied;
    private State currentState = State.CATCHING;
    private boolean droppingInProgress = false;
    private final Queue<Runnable> dropQueue = new ConcurrentLinkedQueue<>();

    public void run(HunterKebbitsConfig config, HunterKebbitsPlugin plugin) {
        super.run();
        Microbot.log("HunterKabbitsScript started.");

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !this.isRunning()) return;

                if (droppingInProgress) return;

                switch (currentState) {
                    case DROPPING:
                        handleDroppingState(config);
                        break;
                    case RETRIEVING:
                        handleRetrievingState(config);
                        break;
                    case CATCHING:
                    default:
                        if (Rs2Inventory.isFull() || Rs2Inventory.getEmptySlots() <= 1) {
                            Microbot.log("📦 Inventar ist voll – wechsle zu DROPPING.");
                            currentState = State.DROPPING;
                        } else {
                            handleCatchingState(config);
                        }
                        break;
                }

            } catch (Exception ex) {
                Microbot.log("❌ Exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!droppingInProgress || dropQueue.isEmpty()) return;

        Runnable nextDropAction = dropQueue.poll();
        if (nextDropAction != null) {
            nextDropAction.run();
        }

        if (dropQueue.isEmpty()) {
            droppingInProgress = false;
            Microbot.log("✅ Dropping abgeschlossen – zurück zu CATCHING.");
            currentState = State.CATCHING;
        }
    }

    private void handleDroppingState(HunterKebbitsConfig config) {
        KebbitHunting currentKebbit = getKebbit(config);
        Integer furItemId = getSupportedFurItemId(currentKebbit);
        if (furItemId == null) {
            Microbot.log("❌ Dieser Kebbit-Typ wird nicht unterstützt.");
            currentState = State.CATCHING;
            return;
        }

        while (Rs2Inventory.contains(furItemId) || Rs2Inventory.contains(ItemID.BONES)) {
            if (Rs2Inventory.contains(furItemId)) Rs2Inventory.drop(furItemId);
            if (Rs2Inventory.contains(ItemID.BONES)) Rs2Inventory.interact(ItemID.BONES, "Bury");
            sleep(300, 600);
        }

        currentState = State.CATCHING;
    }

    private void handleRetrievingState(HunterKebbitsConfig config) {
        final int GYR_FALCON_NPC_ID = 1342;
        if (Rs2Npc.interact(GYR_FALCON_NPC_ID, "Retrieve")) {
            sleep(config.minSleepAfterCatch(), config.maxSleepAfterCatch());
            KebbitCaught++;
            currentState = State.CATCHING;
        }
    }

    private void handleCatchingState(HunterKebbitsConfig config) {
        String npcName = getKebbit(config).getNpcName();
        Microbot.log("🎯 Suche NPC '" + npcName + "'");
        if (Rs2Npc.interact(npcName, "Catch")) {
            Microbot.log("🕒 Catch ausgelöst.");
            boolean falconActive = false;
            for (int i = 0; i < 10; i++) {
                NPC falcon = Rs2Npc.getNpc(1342);
                if (falcon != null) {
                    falconActive = true;
                    break;
                }
                sleep(200, 300);
            }
            if (falconActive) {
                Microbot.log("🦅 Falcon erkannt – wechsle zu RETRIEVING.");
                currentState = State.RETRIEVING;
            } else {
                Microbot.log("⚠️ Kein Falcon – Catch fehlgeschlagen.");
            }
        }
        sleep(config.MinSleepAfterHuntingKebbit(), config.MaxSleepAfterHuntingKebbit());
    }

    private KebbitHunting getKebbit(HunterKebbitsConfig config) {
        int level = Microbot.getClient().getRealSkillLevel(Skill.HUNTER);
        if (config.progressiveHunting()) {
            if (level >= 57) return KebbitHunting.DARK;
            if (level >= 49) return KebbitHunting.DASHING;
            return KebbitHunting.SPOTTED;
        }
        return config.kebbitType() != null ? config.kebbitType() : KebbitHunting.SPOTTED;
    }

    private Integer getSupportedFurItemId(KebbitHunting kebbit) {
        switch (kebbit) {
            case SPOTTED:
                return ItemID.SPOTTED_KEBBIT_FUR;
            case DASHING:
                return ItemID.DASHING_KEBBIT_FUR;
            case DARK:
                return ItemID.DARK_KEBBIT_FUR;
            default:
                return null;
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        KebbitCaught = 0;
        dropQueue.clear();
        droppingInProgress = false;
        Microbot.status = "Script stopped.";
    }
}
