package net.runelite.client.plugins.microbot.hunterKabbits;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;

import java.util.concurrent.TimeUnit;

public class HunterKabbitsScript extends Script {
    public static final int GYR_FALCON = 1342;
    public static final int GYR_FALCON_1343 = 1343;
    public static final int GYR_FALCON_1344 = 1344;
    public static final int GYR_FALCON_1345 = 1345;
    public static final int KEBBIT = 1494;
    public static final int SPOTTED_KEBBIT = 5531;
    public static final int DARK_KEBBIT = 5532;
    public static final int DASHING_KEBBIT = 5533;
    public static final int PRICKLY_KEBBIT = 1346;
    public static final int SABRETOOTHED_KEBBIT = 1347;
    public static final int BARBTAILED_KEBBIT = 1348;
    public static final int WILD_KEBBIT = 1349;
    public static int KebbitCaught;
    public boolean hasDied = false;

    public void run(HunterKebbitsConfig config, HunterKebbitsPlugin hunterKebbitsPlugin) {
        Rs2Antiban.resetAntibanSettings();
        applyAntiBanSettings();
        Rs2Antiban.setActivity(Activity.GENERAL_HUNTER);
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;
                if (!Microbot.isLoggedIn()) return;
                if (!this.isRunning()) return;
            } catch (Exception ex) {
                System.out.println("Kebbit Script Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
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
