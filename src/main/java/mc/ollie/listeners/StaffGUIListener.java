package mc.ollie.listeners;

import mc.ollie.App;
import mc.ollie.gui.StaffGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class StaffGUIListener implements Listener {

    private final App plugin;
    private final StaffGUI staffGUI;

    public StaffGUIListener(App plugin, StaffGUI staffGUI) {
        this.plugin = plugin;
        this.staffGUI = staffGUI;
    }

    private String getMessage(String path) {
        String prefix = plugin.getFileManager().getMessages().getString("prefix");
        if (prefix == null) prefix = "";
        String message = plugin.getFileManager().getMessages().getString(path);
        if (message == null) return "Message not found: " + path;
        message = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getView().getTitle() == null) return;
        if (!event.getView().getTitle().equals(StaffGUI.GUI_TITLE)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        if (!player.hasPermission("Axentra.admin")) {
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        switch (slot) {
            // ── Row 1: Chat Control ──

            // Mute Chat toggle
            case 11: {
                boolean newState = !plugin.isChatMuted();
                plugin.setChatMuted(newState);
                String senderName = player.getName();
                if (newState) {
                    Bukkit.broadcastMessage(getMessage("mutechat-enabled").replace("%player%", senderName));
                } else {
                    Bukkit.broadcastMessage(getMessage("mutechat-disabled").replace("%player%", senderName));
                }
                refresh(player);
                break;
            }

            // Slow Chat toggle/cycle
            case 12: {
                if (event.isRightClick()) {
                    int current = plugin.getSlowChatSeconds();
                    int[] durations = {3, 5, 10, 30, 60};
                    int next = 0;
                    for (int i = 0; i < durations.length; i++) {
                        if (durations[i] == current && i + 1 < durations.length) {
                            next = durations[i + 1];
                            break;
                        } else if (durations[i] > current) {
                            next = durations[i];
                            break;
                        }
                    }
                    if (next == 0) next = durations[0];
                    plugin.setSlowChatSeconds(next);
                    Bukkit.broadcastMessage(getMessage("slowchat-enabled").replace("%seconds%", String.valueOf(next)));
                } else {
                    if (plugin.getSlowChatSeconds() > 0) {
                        plugin.setSlowChatSeconds(0);
                        Bukkit.broadcastMessage(getMessage("slowchat-disabled"));
                    } else {
                        int defaultSeconds = plugin.getFileManager().getSettings().getInt("slowchat-default-seconds", 5);
                        plugin.setSlowChatSeconds(defaultSeconds);
                        Bukkit.broadcastMessage(getMessage("slowchat-enabled").replace("%seconds%", String.valueOf(defaultSeconds)));
                    }
                }
                refresh(player);
                break;
            }

            // Clear Chat
            case 13: {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.hasPermission("Axentra.clearchat.bypass")) {
                        for (int i = 0; i < 100; i++) {
                            online.sendMessage("");
                        }
                    }
                }
                Bukkit.broadcastMessage(getMessage("clearchat-cleared").replace("%player%", player.getName()));
                break;
            }

            // ── Row 2: Announcements ──

            // Announce kick
            case 20: {
                toggleConfig("announce-kick");
                refresh(player);
                break;
            }

            // Announce ban
            case 21: {
                toggleConfig("announce-ban");
                refresh(player);
                break;
            }

            // Announce unban
            case 22: {
                toggleConfig("announce-unban");
                refresh(player);
                break;
            }

            // Announce mute
            case 23: {
                toggleConfig("announce-mute");
                refresh(player);
                break;
            }

            // Announce unmute
            case 24: {
                toggleConfig("announce-unmute");
                refresh(player);
                break;
            }

            // Announce warn
            case 25: {
                toggleConfig("announce-warn");
                refresh(player);
                break;
            }

            // ── Row 3: Gameplay ──

            // Spawn countdown
            case 29: {
                toggleConfig("spawn-countdown");
                refresh(player);
                break;
            }

            // TPA title countdown
            case 30: {
                toggleConfig("tpa-title-countdown");
                refresh(player);
                break;
            }

            // InvSee read-only
            case 31: {
                toggleConfig("invsee-read-only");
                refresh(player);
                break;
            }

            // ── Row 4: Vanish ──

            // Vanish fake join/quit
            case 38: {
                toggleConfig("vanish-fake-join-quit");
                refresh(player);
                break;
            }

            // Vanish ghost effect
            case 39: {
                toggleConfig("vanish-ghost-effect");
                refresh(player);
                break;
            }

            // Vanish night vision
            case 40: {
                toggleConfig("vanish-night-vision");
                refresh(player);
                break;
            }

            // Vanish god mode
            case 41: {
                toggleConfig("vanish-god-mode");
                refresh(player);
                break;
            }

            // Vanish no pickup
            case 42: {
                toggleConfig("vanish-no-pickup");
                refresh(player);
                break;
            }

            // ── Row 5: Close ──

            case 49: {
                player.closeInventory();
                break;
            }
        }
    }

    private void toggleConfig(String key) {
        boolean current = plugin.getFileManager().getSettings().getBoolean(key, true);
        plugin.getConfig().set(key, !current);
        plugin.saveConfig();
    }

    private void refresh(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> staffGUI.open(player), 1L);
    }
}
