package net.runelite.client.plugins.microbot.hunterKabbits;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.hunter.HunterTrap;

import net.runelite.client.plugins.microbot.util.misc.TimeUtils;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;

@Slf4j
@PluginDescriptor(
        name = PluginDescriptor.TaFCat + "Kebbits",
        description = "Automates Kebbits hunting",
        tags = {"hunter", "kebbits", "skilling"},
        enabledByDefault = false
)
public class HunterKebbitsPlugin extends Plugin {

    @Inject
    private Client client;
    @Inject
    private HunterKebbitsConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private HunterKebbitsOverlay kebbitsOverlay;
    private HunterKabbitsScript script;
    private WorldPoint lastTickLocalPlayerLocation;
    private Instant scriptStartTime;

    @Provides
    HunterKebbitsConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(HunterKebbitsConfig.class);
    }


    @Override
    protected void startUp() throws Exception {
        log.info("Kebbit Plugin started!");
        scriptStartTime = Instant.now();
        overlayManager.add(kebbitsOverlay);
        script = new HunterKabbitsScript();
        script.run(config, this);
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Kebbit Plugin stopped!");
        scriptStartTime = null;
        overlayManager.remove(kebbitsOverlay);
        if (script != null) {
            script.shutdown();
        }
    }


    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() == ChatMessageType.GAMEMESSAGE && event.getMessage().equalsIgnoreCase("oh dear, you are dead!")) {
            script.hasDied = true;
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {

    }


    public String getTimeRunning() {
        return scriptStartTime != null ? TimeUtils.getFormattedDurationBetween(scriptStartTime, Instant.now()) : "";
    }

    @Subscribe
    public void onGameTick(GameTick event) {
    }


}
