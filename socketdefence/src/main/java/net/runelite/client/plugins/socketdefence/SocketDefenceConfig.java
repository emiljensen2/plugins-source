package net.runelite.client.plugins.socket.plugins.socketdefence;

import net.runelite.client.config.*;

@ConfigGroup("socketdefence")
public interface SocketDefenceConfig extends Config{
    @ConfigSection(
            name = "<html><font color=#00aeef>Corp",
            description = "Corp settings",
            position = 0,
            closedByDefault = true
    )
    public static final String corpSection = "corp";

    @Range(max = 50, min = 2)
    @ConfigItem(
            name = "Low Defence Threshold",
            keyName = "lowDef",
            description = "Sets when you want the defence to appear as yellow (low defence).",
            position = 0
    )
    default int lowDef() {
        return 10;
    }

    @ConfigItem(
            keyName = "cm",
            name = "Challenge Mode",
            description = "Toggle this to set the defence to Challenge Mode when doing Cox",
            position = 1
    )
    default boolean cm() {
        return true;
    }

    @ConfigItem(
            keyName = "vulnerability",
            name = "Show Vulnerability",
            description = "Displays an infobox when you successfully land vulnerability",
            position = 1
    )
    default boolean vulnerability() {
        return true;
    }

    //Corp Section
    @ConfigItem(
            keyName = "corpChally",
            name = "Corp Chally Highlight",
            description = "Highlight corp when you should chally spec",
            position = 0,
            section = corpSection
    )
    default CorpTileMode corpChally() {
        return CorpTileMode.OFF;
    }

    @Range(min = 0, max = 255)
    @ConfigItem(
            keyName = "corpChallyOpacity",
            name = "Corp Chally Opactiy",
            description = "Toggles opacity of Corp Chally Highlight",
            position = 1,
            section = corpSection
    )
    default int corpChallyOpacity() {
        return 20;
    }

    @Range(min = 1, max = 5)
    @ConfigItem(
            keyName = "corpChallyThicc",
            name = "Corp Chally Width",
            description = "Toggles girth of Corp Chally Highlight",
            position = 2,
            section = corpSection
    )
    default int corpChallyThicc() {
        return 2;
    }

    @Range(min = 0, max = 4)
    @ConfigItem(
            keyName = "corpGlow",
            name = "Corp Chally Glow",
            description = "Adjusts glow of Corp Chally Outline Highlight",
            position = 3,
            section = corpSection
    )
    default int corpGlow() {
        return 4;
    }

    public enum CorpTileMode {
        OFF, AREA, HULL, TILE, TRUE_LOCATION, OUTLINE;
    }
}
