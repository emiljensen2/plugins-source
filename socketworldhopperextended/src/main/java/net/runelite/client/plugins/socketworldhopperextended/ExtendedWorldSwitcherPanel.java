package net.runelite.client.plugins.socketworldhopperextended;

import com.google.common.collect.Ordering;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldType;

class ExtendedWorldSwitcherPanel extends PluginPanel {
    private static final Color ODD_ROW = new Color(44, 44, 44);

    private static final int WORLD_COLUMN_WIDTH = 60;

    private static final int PLAYERS_COLUMN_WIDTH = 40;

    private static final int PING_COLUMN_WIDTH = 47;

    private final JPanel listContainer = new JPanel();

    private ExtendedWorldTableHeader worldHeader;

    private ExtendedWorldTableHeader playersHeader;

    private ExtendedWorldTableHeader activityHeader;

    private ExtendedWorldTableHeader pingHeader;

    private WorldOrder orderIndex = WorldOrder.WORLD;

    private boolean ascendingOrder = true;

    private final ArrayList<ExtendedWorldTableRow> rows = new ArrayList<>();

    private final WorldHopperExtendedPlugin plugin;

    private SubscriptionFilterMode filterMode;

    void setFilterMode(SubscriptionFilterMode filterMode) {
        this.filterMode = filterMode;
    }

    private RegionFilterMode regionFilterMode;
    void setRegionFilterMode(RegionFilterMode regionFilterMode) {this.regionFilterMode = regionFilterMode;}

    ExtendedWorldSwitcherPanel(WorldHopperExtendedPlugin plugin) {
        this.plugin = plugin;
        setBorder(null);
        setLayout((LayoutManager)new DynamicGridLayout(0, 1));
        JPanel headerContainer = buildHeader();
        this.listContainer.setLayout(new GridLayout(0, 1));
        add(headerContainer);
        add(this.listContainer);
    }

    void switchCurrentHighlight(int newWorld, int lastWorld) {
        for (ExtendedWorldTableRow row : this.rows) {
            if (row.getWorld().getId() == newWorld) {
                row.recolour(true);
                continue;
            }
            if (row.getWorld().getId() == lastWorld)
                row.recolour(false);
        }
    }

    void updateListData(Map<Integer, Integer> worldData) {
        for (ExtendedWorldTableRow worldTableRow : this.rows) {
            World world = worldTableRow.getWorld();
            Integer playerCount = worldData.get(world.getId());
            if (playerCount != null)
                worldTableRow.updatePlayerCount(playerCount);
        }
        if (this.orderIndex == WorldOrder.PLAYERS)
            updateList();
    }

    void updatePing(int world, int ping) {
        for (ExtendedWorldTableRow worldTableRow : this.rows) {
            if (worldTableRow.getWorld().getId() == world) {
                worldTableRow.setPing(ping);
                if (this.orderIndex == WorldOrder.PING)
                    updateList();
                break;
            }
        }
    }

    void hidePing() {
        for (ExtendedWorldTableRow worldTableRow : this.rows)
            worldTableRow.hidePing();
    }

    void showPing() {
        for (ExtendedWorldTableRow worldTableRow : this.rows)
            worldTableRow.showPing();
    }

    void updateList()
    {
        rows.sort((r1, r2) ->
        {
            switch (orderIndex)
            {
                case PING:
                    // Leave worlds with unknown ping at the bottom
                    return getCompareValue(r1, r2, row ->
                    {
                        int ping = row.getPing();
                        return ping > 0 ? ping : null;
                    });
                case WORLD:
                    return getCompareValue(r1, r2, row -> row.getWorld().getId());
                case PLAYERS:
                    return getCompareValue(r1, r2, ExtendedWorldTableRow::getUpdatedPlayerCount);
                case ACTIVITY:
                    // Leave empty activity worlds on the bottom of the list
                    return getCompareValue(r1, r2, row ->
                    {
                        String activity = row.getWorld().getActivity();
                        return !activity.equals("-") ? activity : null;
                    });
                default:
                    return 0;
            }
        });

        this.rows.sort((r1, r2) -> {
            boolean b1 = this.plugin.isFavorite(r1.getWorld());
            boolean b2 = this.plugin.isFavorite(r2.getWorld());
            return Boolean.compare(b2, b1);
        });
        this.listContainer.removeAll();
        for (int i = 0; i < this.rows.size(); i++) {
            ExtendedWorldTableRow row = this.rows.get(i);
            row.setBackground((i % 2 == 0) ? ODD_ROW : ColorScheme.DARK_GRAY_COLOR);
            this.listContainer.add(row);
        }
        this.listContainer.revalidate();
        this.listContainer.repaint();
    }

    private int getCompareValue(ExtendedWorldTableRow row1, ExtendedWorldTableRow row2, Function<ExtendedWorldTableRow, Comparable> compareByFn) {
        Ordering<Comparable> ordering = Ordering.natural();
        if (!this.ascendingOrder)
            ordering = ordering.reverse();
        ordering = ordering.nullsLast();
        return ordering.compare(compareByFn.apply(row1), compareByFn.apply(row2));
    }

    void updateFavoriteMenu(int world, boolean favorite) {
        for (ExtendedWorldTableRow row : this.rows) {
            if (row.getWorld().getId() == world)
                row.setFavoriteMenu(favorite);
        }
    }

    void populate(List<World> worlds) {
        this.rows.clear();
        for (int i = 0; i < worlds.size(); i++) {
            World world = worlds.get(i);
            switch (this.filterMode) {
                case FREE:
                    if (world.getTypes().contains(WorldType.MEMBERS))
                        break;
                case MEMBERS:
                    if (!world.getTypes().contains(WorldType.MEMBERS))
                        break;
                default:
                    this.rows.add(buildRow(world, (i % 2 == 0), (world.getId() == this.plugin.getCurrentWorld() && this.plugin.getLastWorld() != 0), this.plugin.isFavorite(world)));
                    break;
            }

            if (regionFilterMode.getRegion() != null && !regionFilterMode.getRegion().equals(world.getRegion()))
            {
                continue;
            }

        }
        updateList();
    }

    private void orderBy(WorldOrder order) {
        this.pingHeader.highlight(false, this.ascendingOrder);
        this.worldHeader.highlight(false, this.ascendingOrder);
        this.playersHeader.highlight(false, this.ascendingOrder);
        this.activityHeader.highlight(false, this.ascendingOrder);
        switch (order) {
            case PING:
                this.pingHeader.highlight(true, this.ascendingOrder);
                break;
            case WORLD:
                this.worldHeader.highlight(true, this.ascendingOrder);
                break;
            case PLAYERS:
                this.playersHeader.highlight(true, this.ascendingOrder);
                break;
            case ACTIVITY:
                this.activityHeader.highlight(true, this.ascendingOrder);
                break;
        }
        this.orderIndex = order;
        updateList();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        JPanel leftSide = new JPanel(new BorderLayout());
        JPanel rightSide = new JPanel(new BorderLayout());
        Objects.requireNonNull(this.plugin);
        this.pingHeader = new ExtendedWorldTableHeader("Ping", (this.orderIndex == WorldOrder.PING), this.ascendingOrder, this.plugin::refresh);
        this.pingHeader.setPreferredSize(new Dimension(47, 0));
        this.pingHeader.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                if (SwingUtilities.isRightMouseButton(mouseEvent))
                    return;
                ExtendedWorldSwitcherPanel.this.ascendingOrder = (ExtendedWorldSwitcherPanel.this.orderIndex != ExtendedWorldSwitcherPanel.WorldOrder.PING || !ExtendedWorldSwitcherPanel.this.ascendingOrder);
                ExtendedWorldSwitcherPanel.this.orderBy(ExtendedWorldSwitcherPanel.WorldOrder.PING);
            }
        });
        Objects.requireNonNull(this.plugin);
        this.worldHeader = new ExtendedWorldTableHeader("World", (this.orderIndex == WorldOrder.WORLD), this.ascendingOrder, this.plugin::refresh);
        this.worldHeader.setPreferredSize(new Dimension(60, 0));
        this.worldHeader.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                if (SwingUtilities.isRightMouseButton(mouseEvent))
                    return;
                ExtendedWorldSwitcherPanel.this.ascendingOrder = (ExtendedWorldSwitcherPanel.this.orderIndex != ExtendedWorldSwitcherPanel.WorldOrder.WORLD || !ExtendedWorldSwitcherPanel.this.ascendingOrder);
                ExtendedWorldSwitcherPanel.this.orderBy(ExtendedWorldSwitcherPanel.WorldOrder.WORLD);
            }
        });
        Objects.requireNonNull(this.plugin);
        this.playersHeader = new ExtendedWorldTableHeader("#", (this.orderIndex == WorldOrder.PLAYERS), this.ascendingOrder, this.plugin::refresh);
        this.playersHeader.setPreferredSize(new Dimension(40, 0));
        this.playersHeader.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                if (SwingUtilities.isRightMouseButton(mouseEvent))
                    return;
                ExtendedWorldSwitcherPanel.this.ascendingOrder = (ExtendedWorldSwitcherPanel.this.orderIndex != ExtendedWorldSwitcherPanel.WorldOrder.PLAYERS || !ExtendedWorldSwitcherPanel.this.ascendingOrder);
                ExtendedWorldSwitcherPanel.this.orderBy(ExtendedWorldSwitcherPanel.WorldOrder.PLAYERS);
            }
        });
        Objects.requireNonNull(this.plugin);
        this.activityHeader = new ExtendedWorldTableHeader("Activity", (this.orderIndex == WorldOrder.ACTIVITY), this.ascendingOrder, this.plugin::refresh);
        this.activityHeader.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                if (SwingUtilities.isRightMouseButton(mouseEvent))
                    return;
                ExtendedWorldSwitcherPanel.this.ascendingOrder = (ExtendedWorldSwitcherPanel.this.orderIndex != ExtendedWorldSwitcherPanel.WorldOrder.ACTIVITY || !ExtendedWorldSwitcherPanel.this.ascendingOrder);
                ExtendedWorldSwitcherPanel.this.orderBy(ExtendedWorldSwitcherPanel.WorldOrder.ACTIVITY);
            }
        });
        leftSide.add(this.worldHeader, "West");
        leftSide.add(this.playersHeader, "Center");
        rightSide.add(this.activityHeader, "Center");
        rightSide.add(this.pingHeader, "East");
        header.add(leftSide, "West");
        header.add(rightSide, "Center");
        return header;
    }

    private ExtendedWorldTableRow buildRow(World world, boolean stripe, boolean current, boolean favorite) {
        ExtendedWorldTableRow row = new ExtendedWorldTableRow(world, current, favorite, this.plugin.getStoredPing(world), world1 -> this.plugin.hopTo(world1), (world12, add) -> {
            if (add) {
                this.plugin.addToFavorites(world12);
            } else {
                this.plugin.removeFromFavorites(world12);
            }
            updateList();
        });
        row.setBackground(stripe ? ODD_ROW : ColorScheme.DARK_GRAY_COLOR);
        return row;
    }

    private enum WorldOrder {
        WORLD, PLAYERS, ACTIVITY, PING;
    }
}
