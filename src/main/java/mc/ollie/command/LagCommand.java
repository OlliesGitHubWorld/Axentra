package mc.ollie.command;

import mc.ollie.App;
import mc.ollie.DatabaseManager;
import mc.ollie.gui.TpsHistoryGUI;
import mc.ollie.health.ChunkScorer;
import mc.ollie.health.HealthScoreService;
import mc.ollie.health.TickProfiler;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.*;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.stream.Collectors;

public class LagCommand implements CommandExecutor, TabCompleter {

    private final App plugin;

    public LagCommand(App plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("Axentra.lag")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            showOverview(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "chunks":
                showChunks(sender);
                break;
            case "entities":
                showEntities(sender);
                break;
            case "history":
                showHistory(sender, args);
                break;
            case "clear":
                handleClear(sender, args);
                break;
            case "tp":
                handleTp(sender, args);
                break;
            case "player":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("lag-player-usage"));
                    return true;
                }
                showPlayerLag(sender, args[1]);
                break;
            default:
                showOverview(sender);
                break;
        }
        return true;
    }

    private void showOverview(CommandSender sender) {
        TickProfiler profiler = plugin.getTickProfiler();
        if (profiler == null) {
            sender.sendMessage(plugin.getMessageManager().getMessage("lag-disabled"));
            return;
        }

        String prefix = plugin.getMessageManager().getMessage("prefix");
        sender.sendMessage("");
        sender.sendMessage(color("&b&l  Server Performance Overview"));
        sender.sendMessage(color("&8  ─────────────────────────────────"));

        // TPS
        sender.sendMessage(color("&7  TPS: " + tpsColor(profiler.getTps1m()) +
                String.format("%.1f", profiler.getTps1m()) + "&7/" +
                tpsColor(profiler.getTps5m()) + String.format("%.1f", profiler.getTps5m()) + "&7/" +
                tpsColor(profiler.getTps15m()) + String.format("%.1f", profiler.getTps15m()) +
                " &8(1m/5m/15m)"));

        // MSPT
        String msptColor = profiler.getMsptAvg() <= 50 ? "&a" : profiler.getMsptAvg() <= 100 ? "&e" : "&c";
        sender.sendMessage(color("&7  MSPT: " + msptColor +
                String.format("%.1f", profiler.getMsptAvg()) + " &7avg, &c" +
                String.format("%.1f", profiler.getMsptMax()) + " &7max"));

        // Memory
        long used = profiler.getMemoryUsed();
        long max = profiler.getMemoryMax();
        int pct = profiler.getMemoryPercent();
        String memColor = pct <= 70 ? "&a" : pct <= 85 ? "&e" : "&c";
        sender.sendMessage(color("&7  Memory: " + memColor + used + "MB &7/ &f" + max + "MB &8(" + memColor + pct + "%&8)"));

        // Uptime
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        sender.sendMessage(color("&7  Uptime: &f" + formatDuration(uptimeMs)));

        // Players
        sender.sendMessage(color("&7  Players: &f" + Bukkit.getOnlinePlayers().size() + "&7/&f" + Bukkit.getMaxPlayers()));

        // World stats
        sender.sendMessage(color("&8  ─────────────────────────────────"));
        for (World world : Bukkit.getWorlds()) {
            int entities = world.getEntities().size();
            int chunks = world.getLoadedChunks().length;
            sender.sendMessage(color("&7  " + world.getName() + "&8: &f" + entities +
                    " &7entities, &f" + chunks + " &7chunks"));
        }

        // Health score
        HealthScoreService hs = plugin.getHealthScoreService();
        if (hs != null) {
            int score = hs.calculate();
            sender.sendMessage(color("&8  ─────────────────────────────────"));
            sender.sendMessage(color("&7  Health: " + hs.getLabelColor(score) + score +
                    "/100 &8(" + hs.getLabelColor(score) + hs.getLabel(score) + "&8)"));
        }

        // Clickable buttons
        if (sender instanceof Player) {
            sender.sendMessage(color("&8  ─────────────────────────────────"));
            TextComponent chunks = makeButton("&b[Chunks]", "/lag chunks", "View heaviest chunks");
            TextComponent entities = makeButton("&e[Entities]", "/lag entities", "Entity breakdown");
            TextComponent history = makeButton("&a[History]", "/lag history", "TPS history graph");

            TextComponent line = new TextComponent("  ");
            line.addExtra(chunks);
            line.addExtra(new TextComponent(" "));
            line.addExtra(entities);
            line.addExtra(new TextComponent(" "));
            line.addExtra(history);
            ((Player) sender).spigot().sendMessage(line);
        }
        sender.sendMessage("");
    }

    private void showChunks(CommandSender sender) {
        sender.sendMessage(color("&b&l  Top 10 Heaviest Chunks"));
        sender.sendMessage(color("&8  ─────────────────────────────────"));

        // Score chunks across all worlds
        List<ChunkScore> scores = new ArrayList<>();
        var health = plugin.getFileManager().getHealth();
        int minScore = health.getInt("lag-chunks.min-score", 10);
        int warningScore = health.getInt("lag-chunks.warning-score", 100);
        int criticalScore = health.getInt("lag-chunks.critical-score", 250);
        ChunkScorer scorer = new ChunkScorer(plugin);
        int scanned = 0;

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                scanned++;
                ChunkScorer.Result r = scorer.score(chunk);
                if (r.score >= minScore && r.score > 0) {
                    scores.add(new ChunkScore(world.getName(), chunk.getX(), chunk.getZ(), r));
                }
            }
        }

        scores.sort((a, b) -> Double.compare(b.result.score, a.result.score));
        int shown = Math.min(10, scores.size());

        if (shown == 0) {
            sender.sendMessage(color("  &a✔ No busy chunks. &7All " + scanned + " loaded chunks score below " + minScore + "."));
            sender.sendMessage("");
            return;
        }

        for (int i = 0; i < shown; i++) {
            ChunkScore cs = scores.get(i);
            ChunkScorer.Result r = cs.result;
            int blockX = cs.chunkX * 16 + 8;
            int blockZ = cs.chunkZ * 16 + 8;
            String scoreColor = r.score >= criticalScore ? "&c" : r.score >= warningScore ? "&e" : "&a";
            String line = String.format("  &e#%d &7%s &f(%d, %d) &7Score: %s%.0f &8[E:%d T:%d]",
                    i + 1, cs.world, blockX, blockZ, scoreColor, r.score, r.entityCount, r.tileCount);

            if (sender instanceof Player) {
                TextComponent text = new TextComponent(color(line));
                text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder(color(breakdown(r, warningScore, criticalScore))).create()));
                if (r.player != null) {
                    text.addExtra(color(" &7by &b" + r.player));
                }
                TextComponent tp = makeButton(" &a[TP]",
                        "/lag tp " + cs.world + " " + blockX + " " + blockZ,
                        "Teleport to this chunk");
                text.addExtra(tp);
                ((Player) sender).spigot().sendMessage(text);
            } else {
                sender.sendMessage(color(line + (r.player != null ? " by " + r.player : "")));
            }
        }
        sender.sendMessage(color("&8  ─────────────────────────────────"));
        sender.sendMessage(color("  &7Scanned &f" + scanned + " &7loaded chunks, &f" + (scanned - scores.size())
                + " &7score below " + minScore + " &8(no problem)"));
        sender.sendMessage(color("  &8E = entities, T = active tile entities (hover for details)"));
        sender.sendMessage("");
    }

    private String breakdown(ChunkScorer.Result r, int warningScore, int criticalScore) {
        StringBuilder sb = new StringBuilder();
        String level = r.score >= criticalScore ? "&cHigh" : r.score >= warningScore ? "&eMedium" : "&aLow";
        sb.append("&7Load: ").append(level);
        sb.append("\n\n&eEntities: &f").append(r.entityCount);
        appendTop(sb, r.entityTypes);
        sb.append("\n&eActive tile entities: &f").append(r.tileCount);
        appendTop(sb, r.tileTypes);
        return sb.toString();
    }

    private void appendTop(StringBuilder sb, Map<String, Integer> types) {
        types.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> sb.append("\n  &7").append(e.getKey()).append(": &f").append(e.getValue()));
    }

    private void showEntities(CommandSender sender) {
        sender.sendMessage(color("&b&l  Entity Breakdown by Type"));
        sender.sendMessage(color("&8  ─────────────────────────────────"));

        Map<EntityType, Integer> counts = new HashMap<>();
        Map<EntityType, String> categories = new HashMap<>();
        Map<String, Integer> perCategory = new LinkedHashMap<>();
        perCategory.put("&cHostile", 0);
        perCategory.put("&aAnimals", 0);
        perCategory.put("&fItems", 0);
        perCategory.put("&7Other", 0);
        int total = 0;
        int loadedChunks = 0;
        int busiestChunk = 0;
        String busiestWhere = null;
        for (World world : Bukkit.getWorlds()) {
            loadedChunks += world.getLoadedChunks().length;
            Map<Long, Integer> perChunk = new HashMap<>();
            for (Entity e : world.getEntities()) {
                if (e instanceof Player) continue;
                counts.merge(e.getType(), 1, Integer::sum);
                String category = categories.computeIfAbsent(e.getType(), t -> category(e));
                perCategory.merge(category, 1, Integer::sum);
                total++;

                int cx = e.getLocation().getBlockX() >> 4;
                int cz = e.getLocation().getBlockZ() >> 4;
                int inChunk = perChunk.merge(((long) cx << 32) | (cz & 0xFFFFFFFFL), 1, Integer::sum);
                if (inChunk > busiestChunk) {
                    busiestChunk = inChunk;
                    busiestWhere = world.getName() + " " + (cx * 16 + 8) + ", " + (cz * 16 + 8);
                }
            }
        }

        if (total == 0) {
            sender.sendMessage(color("  &7No entities loaded."));
            sender.sendMessage("");
            return;
        }

        // Sort by count descending
        List<Map.Entry<EntityType, Integer>> sorted = counts.entrySet().stream()
                .sorted(Map.Entry.<EntityType, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        // Fixed-width bar first so names line up (chat font is not monospaced)
        int barLength = 10;
        int max = sorted.get(0).getValue();
        int shown = Math.min(15, sorted.size());
        for (int i = 0; i < shown; i++) {
            Map.Entry<EntityType, Integer> entry = sorted.get(i);
            int count = entry.getValue();
            String category = categories.get(entry.getKey());
            String catColor = category.substring(0, 2);
            int filled = Math.max(1, (int) Math.round(count / (double) max * barLength));
            int percent = (int) Math.round(count * 100.0 / total);
            sender.sendMessage(color("  " + catColor + "■".repeat(filled) + "&8" + "■".repeat(barLength - filled)
                    + " &f" + prettyName(entry.getKey().name()) + " &7" + count + " &8(" + percent + "%)"));
        }
        if (sorted.size() > shown) {
            sender.sendMessage(color("  &8... and " + (sorted.size() - shown) + " more types"));
        }
        sender.sendMessage(color("&8  ─────────────────────────────────"));
        StringBuilder summary = new StringBuilder("  ");
        for (Map.Entry<String, Integer> cat : perCategory.entrySet()) {
            if (cat.getValue() == 0) continue;
            summary.append(cat.getKey()).append(": &f").append(cat.getValue()).append("  ");
        }
        sender.sendMessage(color(summary.toString().stripTrailing()));
        sender.sendMessage(color("  &7Total: &f" + total + " &7entities &8(players not counted)"));

        // Status based on density, not totals, so it scales with player count
        var health = plugin.getFileManager().getHealth();
        double avgWarning = health.getDouble("lag-chunks.avg-entities-warning", 8);
        double avgCritical = health.getDouble("lag-chunks.avg-entities-critical", 20);
        int chunkWarning = health.getInt("lag-chunks.warning-score", 100);
        int chunkCritical = health.getInt("lag-chunks.critical-score", 250);
        double avg = loadedChunks > 0 ? (double) total / loadedChunks : 0;
        int level = Math.max(
                avg >= avgCritical ? 2 : avg >= avgWarning ? 1 : 0,
                busiestChunk >= chunkCritical ? 2 : busiestChunk >= chunkWarning ? 1 : 0);
        String status = level == 2 ? "&c⚠ High" : level == 1 ? "&e⚠ Elevated" : "&a✔ Normal";
        sender.sendMessage(color("  &7Status: " + status));
        sender.sendMessage(color(String.format("  &7Avg per chunk: &f%.1f &8| &7Busiest chunk: &f%d &8(%s)",
                avg, busiestChunk, busiestWhere != null ? busiestWhere : "-")));
        int players = Bukkit.getOnlinePlayers().size();
        if (players > 0) {
            sender.sendMessage(color(String.format("  &7Per player: &f%.0f &7entities", total / (double) players)));
        }
        sender.sendMessage("");
    }

    private String category(Entity e) {
        if (e instanceof Monster || e instanceof Slime || e instanceof Ghast || e instanceof Phantom
                || e instanceof Shulker || e instanceof EnderDragon) return "&cHostile";
        if (e instanceof Item || e instanceof ExperienceOrb) return "&fItems";
        if (e instanceof Mob) return "&aAnimals";
        return "&7Other";
    }

    private String prettyName(String enumName) {
        StringBuilder sb = new StringBuilder();
        for (String part : enumName.toLowerCase().split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private void showHistory(CommandSender sender, String[] args) {
        int hours = 1;
        if (args.length >= 2) {
            try {
                hours = Integer.parseInt(args[1]);
                hours = Math.max(1, Math.min(72, hours));
            } catch (NumberFormatException ignored) {}
        }

        if (sender instanceof Player player) {
            new TpsHistoryGUI(plugin).open(player, hours);
            return;
        }

        // Console: text graph
        long since = System.currentTimeMillis() - (hours * 3600000L);
        int finalHours = hours;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<DatabaseManager.TpsHistoryEntry> entries =
                    plugin.getDatabaseManager().getTpsHistory(since, 60);

            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(color("&b&l  TPS History &7(last " + finalHours + "h)"));
                sender.sendMessage(color("&8  ─────────────────────────────────"));

                if (entries.isEmpty()) {
                    sender.sendMessage(color("  &7No history data yet. Data is recorded every minute."));
                    return;
                }

                // Simple text graph — show last entries
                Collections.reverse(entries); // oldest first
                int barWidth = 30;
                for (DatabaseManager.TpsHistoryEntry e : entries) {
                    double tps = e.tps1m;
                    int filled = (int) Math.round((tps / 20.0) * barWidth);
                    filled = Math.max(0, Math.min(barWidth, filled));
                    String bar = tpsColor(tps) + "█".repeat(filled) + "&8" + "░".repeat(barWidth - filled);

                    long minutesAgo = (System.currentTimeMillis() - e.time) / 60000;
                    String timeLabel = minutesAgo < 1 ? "now" : minutesAgo + "m";
                    sender.sendMessage(color(String.format("  &7%4s %s &7%.1f", timeLabel, bar, tps)));
                }

                // Lag peaks
                sender.sendMessage(color("&8  ─────────────────────────────────"));
                double minTps = entries.stream().mapToDouble(e -> e.tps1m).min().orElse(20.0);
                sender.sendMessage(color("  &7Lowest TPS: " + tpsColor(minTps) + String.format("%.1f", minTps)));
                sender.sendMessage("");
            });
        });
    }

    private void handleClear(CommandSender sender, String[] args) {
        if (!sender.hasPermission("Axentra.lag.clear")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return;
        }

        String type = args.length >= 2 ? args[1].toLowerCase() : "items";
        int removed = 0;

        switch (type) {
            case "items":
                removed = clearGroundItems();
                sender.sendMessage(plugin.getMessageManager().getMessage("lag-clear-items")
                        .replace("%count%", String.valueOf(removed)));
                break;
            case "mobs":
                removed = clearMobs();
                sender.sendMessage(plugin.getMessageManager().getMessage("lag-clear-mobs")
                        .replace("%count%", String.valueOf(removed)));
                break;
            case "all":
                int items = clearGroundItems();
                int mobs = clearMobs();
                sender.sendMessage(plugin.getMessageManager().getMessage("lag-clear-all")
                        .replace("%items%", String.valueOf(items))
                        .replace("%mobs%", String.valueOf(mobs)));
                break;
            default:
                sender.sendMessage(plugin.getMessageManager().getMessage("lag-clear-usage"));
                break;
        }
    }

    private int clearGroundItems() {
        var health = plugin.getFileManager().getHealth();
        boolean skipNamed = health.getBoolean("cleanup.skip-named-items", true);
        List<String> whitelist = health.getStringList("cleanup.whitelist");
        Set<String> whitelistSet = new HashSet<>(whitelist);

        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item item) {
                    if (skipNamed && item.getItemStack().getItemMeta() != null &&
                            item.getItemStack().getItemMeta().hasDisplayName()) continue;
                    if (whitelistSet.contains(item.getItemStack().getType().name())) continue;
                    item.remove();
                    removed++;
                }
            }
        }
        return removed;
    }

    private int clearMobs() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Monster || entity instanceof Slime) {
                    if (entity.getCustomName() != null) continue;
                    if (entity.isInsideVehicle()) continue;
                    entity.remove();
                    removed++;
                }
            }
        }
        return removed;
    }

    private void handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("rtp-player-only"));
            return;
        }
        // /lag tp <world> <x> <z>
        if (args.length < 4) {
            return;
        }
        World world = Bukkit.getWorld(args[1]);
        if (world == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("prefix") + color("&cWorld not found."));
            return;
        }
        try {
            int x = Integer.parseInt(args[2]);
            int z = Integer.parseInt(args[3]);
            int y = world.getHighestBlockYAt(x, z) + 1;
            player.teleport(new Location(world, x + 0.5, y, z + 0.5));
            player.sendMessage(color("&7Teleported to &e" + x + ", " + y + ", " + z + " &7in &e" + world.getName()));
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessageManager().getMessage("prefix") + color("&cInvalid coordinates."));
        }
    }

    private void showPlayerLag(CommandSender sender, String playerName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Find player UUID from cases DB
            String uuid = plugin.getDatabaseManager().getUuidByName(playerName);
            if (uuid == null) {
                // Try online players
                Player target = Bukkit.getPlayerExact(playerName);
                if (target != null) uuid = target.getUniqueId().toString();
            }

            if (uuid == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(plugin.getMessageManager().getMessage("player-not-found")));
                return;
            }

            long since = System.currentTimeMillis() - (7 * 86400000L); // last 7 days
            List<DatabaseManager.ChunkActivityEntry> activities =
                    plugin.getDatabaseManager().getPlayerChunkActivity(uuid, since);

            String finalPlayerName = playerName;
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(color("&b&l  Lag Activity: " + finalPlayerName));
                sender.sendMessage(color("&8  ─────────────────────────────────"));

                if (activities.isEmpty()) {
                    sender.sendMessage(color("  &7No lag-relevant activity recorded."));
                    return;
                }

                // Aggregate by type
                Map<String, Integer> byType = new LinkedHashMap<>();
                for (DatabaseManager.ChunkActivityEntry e : activities) {
                    String key = e.actionType + (e.detail != null ? ":" + e.detail : "");
                    byType.merge(key, e.count, Integer::sum);
                }

                int totalScore = 0;
                for (Map.Entry<String, Integer> entry : byType.entrySet()) {
                    sender.sendMessage(color("  &7" + entry.getKey() + ": &f" + entry.getValue()));
                    totalScore += entry.getValue();
                }

                sender.sendMessage(color("&8  ─────────────────────────────────"));
                sender.sendMessage(color("  &7Total activity score: &f" + totalScore));
                sender.sendMessage("");
            });
        });
    }

    private String tpsColor(double tps) {
        var health = plugin.getFileManager().getHealth();
        double green = health.getDouble("tps.green-threshold", 19.0);
        double yellow = health.getDouble("tps.yellow-threshold", 15.0);
        if (tps >= green) return "&a";
        if (tps >= yellow) return "&e";
        return "&c";
    }

    private TextComponent makeButton(String text, String command, String hover) {
        TextComponent btn = new TextComponent(color(text));
        btn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        btn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(color(hover)).create()));
        return btn;
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("Axentra.lag")) return Collections.emptyList();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("chunks", "entities", "history", "clear", "player"));
            subs.removeIf(s -> !s.startsWith(args[0].toLowerCase()));
            return subs;
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("clear")) {
                List<String> types = new ArrayList<>(Arrays.asList("items", "mobs", "all"));
                types.removeIf(s -> !s.startsWith(args[1].toLowerCase()));
                return types;
            }
            if (args[0].equalsIgnoreCase("player")) {
                return null; // default player names
            }
        }
        return Collections.emptyList();
    }

    private static class ChunkScore {
        final String world;
        final int chunkX, chunkZ;
        final ChunkScorer.Result result;

        ChunkScore(String world, int chunkX, int chunkZ, ChunkScorer.Result result) {
            this.world = world;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.result = result;
        }
    }
}
