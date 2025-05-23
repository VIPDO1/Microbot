package net.runelite.client.plugins.microbot.hunterKabbits;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.enums.Kebbits;
import net.runelite.client.plugins.microbot.util.walker.enums.Salamanders;

@Getter
@RequiredArgsConstructor
public enum KebbitHunting {

    SPOTTED("Spotted Kebbit", Kebbits.SPOTTED_KEBBIT.getWorldPoint()),
    DASHING("DASHING Kebbit", Kebbits.DASHING_KEBBIT.getWorldPoint()),
    DARK("Dark Kebbit", Kebbits.DARK_KEBBIT.getWorldPoint()),
    BARBTAILED("Barb Tailes Kebbit", Kebbits.BARB_TAILED_KEBBIT.getWorldPoint());

    private final String name;
    private final WorldPoint huntingPoint;

    @Override
    public String toString() {
        return name;
    }
}