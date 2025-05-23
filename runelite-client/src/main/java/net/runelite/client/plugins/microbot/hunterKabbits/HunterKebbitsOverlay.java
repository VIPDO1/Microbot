package net.runelite.client.plugins.microbot.hunterKabbits;


import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class HunterKebbitsOverlay extends OverlayPanel {

    private final Client client;
    private final HunterKebbitsConfig config;
    private final HunterKebbitsPlugin plugin;
    private final HunterKabbitsScript script;
    private int startingLevel = 0;

    @Inject
    public HunterKebbitsOverlay(Client client, HunterKebbitsConfig config, HunterKebbitsPlugin plugin, HunterKabbitsScript script) {
        super(plugin);
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        this.script = script;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showOverlay()) {
            return null;
        }

        if (startingLevel == 0) {
            startingLevel = client.getRealSkillLevel(Skill.HUNTER);
        }

        panelComponent.getChildren().clear();
        panelComponent.setPreferredSize(new Dimension(200, 300));

        // Title with version
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Kebbit Hunter by VIP")
                .color(Color.GREEN)
                .build());

        panelComponent.getChildren().add(LineComponent.builder().build());

        // Basic information
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Running: ")
                .right(plugin.getTimeRunning())
                .leftColor(Color.WHITE)
                .rightColor(Color.WHITE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Hunter Level:")
                .right(startingLevel + "/" + client.getRealSkillLevel(Skill.HUNTER))
                .leftColor(Color.WHITE)
                .rightColor(Color.ORANGE)
                .build());

        // Salamander type
        if (config.kebbitHunting() != null) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Hunting:")
                    .right(config.kebbitHunting().getName())
                    .leftColor(Color.WHITE)
                    .rightColor(Color.YELLOW)
                    .build());
        }

        // Statistics
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Kebbits Caught:")
                .right(String.valueOf(HunterKabbitsScript.KebbitCaught))
                .leftColor(Color.WHITE)
                .rightColor(Color.GREEN)
                .build());

        return super.render(graphics);
    }
}
