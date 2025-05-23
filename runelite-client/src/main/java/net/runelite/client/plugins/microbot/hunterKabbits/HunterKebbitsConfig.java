package net.runelite.client.plugins.microbot.hunterKabbits;

import net.runelite.client.config.*;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigInformation;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.plugins.microbot.TaF.salamanders.SalamanderHunting;
import net.runelite.client.ui.overlay.Overlay;

@ConfigGroup("Hunter Kebbits")
@ConfigInformation("<html>"
        + "<p>This plugin automates Kebbit hunting.</p>\n"
        + "<p>Configure sleep timings and Kebbit type in the settings for optimal performance.</p>\n"
        + "</html>")
public interface HunterKebbitsConfig extends Config {

    @ConfigItem(
            position = 0,
            keyName = "kebbitHunting",
            name = "Kabbit to hunt",
            description = "Select which Kabbit to hunt"
    )
    default KebbitHunting kebbitHunting() {
        return KebbitHunting.SPOTTED;
    }

    @ConfigItem(
            position = 1,
            keyName = "progressiveHunting",
            name = "Automatically select best Kebbit to hunt.",
            description = "This will override the selected Kebbit. Furthermore, it will move you to the next location when you meet the requirements."
    )
    default boolean progressiveHunting() {
        return false;
    }

    @ConfigItem(
            position = 2,
            keyName = "showOverlay",
            name = "Show Overlay",
            description = "Displays the overlay"
    )
    default boolean showOverlay() {
        return true;
    }

    @ConfigItem(
            position = 4,
            keyName = "MinSleepAfterCatch",
            name = "Min. Sleep After Catch - Recommended minimum 7500ms",
            description = "Min sleep after catch"
    )
    default int minSleepAfterCatch() {
        return 7500;
    }

    @ConfigItem(
            position = 5,
            keyName = "MaxSleepAfterCatch",
            name = "Max. Sleep After Catch",
            description = "Max sleep after catch"
    )
    default int maxSleepAfterCatch() {
        return 8400;
    }

    @ConfigItem(
            position = 6,
            keyName = "MinSleepAfterHuntingKebbit",
            name = "Min. Sleep After sending Kyr - Recommended minimum 4000ms",
            description = "Min sleep before Send Kyr to fly"
    )
    default int MinSleepAfterHuntingKebbit() {
        return 4000;
    }

    @ConfigItem(
            position = 7,
            keyName = "MaxSleepAfterHuntingKebbit",
            name = "Max. Sleep After sending Kyr",
            description = "Max sleep before Send Kyr to fl"
    )
    default int MaxSleepAfterHuntingKebbit() {
        return 5400;
    }

}
