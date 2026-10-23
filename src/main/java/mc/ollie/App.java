package mc.ollie;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import mc.ollie.command.*;
import mc.ollie.listeners.BackListener;
import mc.ollie.listeners.ChatListener;
import mc.ollie.listeners.GodListener;
import mc.ollie.listeners.JoinLeaveListener;
import mc.ollie.listeners.LoginListener;
import mc.ollie.listeners.ModerationGUIListener;
import mc.ollie.listeners.StaffGUIListener;
import mc.ollie.listeners.UpdateListener;
import mc.ollie.listeners.VanishListener;
import org.bukkit.command.SimpleCommandMap;
import org.bstats.bukkit.Metrics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class App extends JavaPlugin {

    private FileManager fileManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private ChatListener chatListener;

    // Chat control state
    private boolean chatMuted = false;
    private int slowChatSeconds = 0;
    private final Map<UUID, Long> lastChatTime = new HashMap<>();
    private final Set<UUID> staffChatToggled = new HashSet<>();

    // Messaging state
    private final Map<UUID, UUID> lastMessaged = new HashMap<>();
    private final Set<UUID> msgToggleOff = new HashSet<>();

    @Override
    public void onEnable() {
        this.fileManager = new FileManager(this);
        fileManager.setup();

        this.messageManager = new MessageManager(this);

        this.databaseManager = new DatabaseManager(this);
        databaseManager.setup();

        int pluginId = 30019;
        Metrics metrics = new Metrics(this, pluginId);

        FileConfiguration settings = fileManager.getSettings();

        ClearCommand clearCommand = new ClearCommand(this);
        getCommand("clear").setExecutor(clearCommand);
        getCommand("clear").setTabCompleter(clearCommand);

        getCommand("repair").setExecutor(new RepairCommand(this));

        KickCommand kickCommand = new KickCommand(this);
        getCommand("kick").setExecutor(kickCommand);
        getCommand("kick").setTabCompleter(kickCommand);

        AxentraCommand axentraCommand = new AxentraCommand(this);
        getCommand("axentra").setExecutor(axentraCommand);
        getCommand("axentra").setTabCompleter(axentraCommand);

        BanCommand banCommand = new BanCommand(this);
        getCommand("ban").setExecutor(banCommand);
        getCommand("ban").setTabCompleter(banCommand);

        TempBanCommand tempBanCommand = new TempBanCommand(this);
        getCommand("tempban").setExecutor(tempBanCommand);
        getCommand("tempban").setTabCompleter(tempBanCommand);

        UnbanCommand unbanCommand = new UnbanCommand(this);
        getCommand("unban").setExecutor(unbanCommand);
        getCommand("unban").setTabCompleter(unbanCommand);

        BanIpCommand banIpCommand = new BanIpCommand(this);
        getCommand("banip").setExecutor(banIpCommand);
        getCommand("banip").setTabCompleter(banIpCommand);

        UnbanIpCommand unbanIpCommand = new UnbanIpCommand(this);
        getCommand("unbanip").setExecutor(unbanIpCommand);
        getCommand("unbanip").setTabCompleter(unbanIpCommand);

        TempBanIpCommand tempBanIpCommand = new TempBanIpCommand(this);
        getCommand("tempbanip").setExecutor(tempBanIpCommand);
        getCommand("tempbanip").setTabCompleter(tempBanIpCommand);

        FlyCommand flyCommand = new FlyCommand(this);
        getCommand("fly").setExecutor(flyCommand);
        getCommand("fly").setTabCompleter(flyCommand);

        HealCommand healCommand = new HealCommand(this);
        getCommand("heal").setExecutor(healCommand);
        getCommand("heal").setTabCompleter(healCommand);

        FeedCommand feedCommand = new FeedCommand(this);
        getCommand("feed").setExecutor(feedCommand);
        getCommand("feed").setTabCompleter(feedCommand);

        WarnCommand warnCommand = new WarnCommand(this);
        getCommand("warn").setExecutor(warnCommand);
        getCommand("warn").setTabCompleter(warnCommand);

        WarnsCommand warnsCommand = new WarnsCommand(this);
        getCommand("warns").setExecutor(warnsCommand);
        getCommand("warns").setTabCompleter(warnsCommand);

        ClearWarnsCommand clearWarnsCommand = new ClearWarnsCommand(this);
        getCommand("clearwarns").setExecutor(clearWarnsCommand);
        getCommand("clearwarns").setTabCompleter(clearWarnsCommand);

        MuteCommand muteCommand = new MuteCommand(this);
        getCommand("mute").setExecutor(muteCommand);
        getCommand("mute").setTabCompleter(muteCommand);

        TempMuteCommand tempMuteCommand = new TempMuteCommand(this);
        getCommand("tempmute").setExecutor(tempMuteCommand);
        getCommand("tempmute").setTabCompleter(tempMuteCommand);

        UnmuteCommand unmuteCommand = new UnmuteCommand(this);
        getCommand("unmute").setExecutor(unmuteCommand);
        getCommand("unmute").setTabCompleter(unmuteCommand);

        AnvilCommand anvilCommand = new AnvilCommand(this);
        getCommand("anvil").setExecutor(anvilCommand);
        getCommand("anvil").setTabCompleter(anvilCommand);

        WorkbenchCommand workbenchCommand = new WorkbenchCommand(this);
        getCommand("workbench").setExecutor(workbenchCommand);
        getCommand("workbench").setTabCompleter(workbenchCommand);

        CartographyCommand cartographyCommand = new CartographyCommand(this);
        getCommand("cartography").setExecutor(cartographyCommand);
        getCommand("cartography").setTabCompleter(cartographyCommand);

        GrindstoneCommand grindstoneCommand = new GrindstoneCommand(this);
        getCommand("grindstone").setExecutor(grindstoneCommand);
        getCommand("grindstone").setTabCompleter(grindstoneCommand);

        LoomCommand loomCommand = new LoomCommand(this);
        getCommand("loom").setExecutor(loomCommand);
        getCommand("loom").setTabCompleter(loomCommand);

        SmithingTableCommand smithingTableCommand = new SmithingTableCommand(this);
        getCommand("smithingtable").setExecutor(smithingTableCommand);
        getCommand("smithingtable").setTabCompleter(smithingTableCommand);

        StonecutterCommand stonecutterCommand = new StonecutterCommand(this);
        getCommand("stonecutter").setExecutor(stonecutterCommand);
        getCommand("stonecutter").setTabCompleter(stonecutterCommand);

        EnderChestCommand enderChestCommand = new EnderChestCommand(this);
        getCommand("enderchest").setExecutor(enderChestCommand);
        getCommand("enderchest").setTabCompleter(enderChestCommand);

        PingCommand pingCommand = new PingCommand(this);
        getCommand("ping").setExecutor(pingCommand);
        getCommand("ping").setTabCompleter(pingCommand);

        SuicideCommand suicideCommand = new SuicideCommand(this);
        getCommand("suicide").setExecutor(suicideCommand);
        getCommand("suicide").setTabCompleter(suicideCommand);

        WeatherCommand weatherCommand = new WeatherCommand(this);
        getCommand("weather").setExecutor(weatherCommand);
        getCommand("weather").setTabCompleter(weatherCommand);
        getCommand("sun").setExecutor(weatherCommand);
        getCommand("sun").setTabCompleter(weatherCommand);
        getCommand("sky").setExecutor(weatherCommand);
        getCommand("sky").setTabCompleter(weatherCommand);
        getCommand("rain").setExecutor(weatherCommand);
        getCommand("rain").setTabCompleter(weatherCommand);
        getCommand("storm").setExecutor(weatherCommand);
        getCommand("storm").setTabCompleter(weatherCommand);

        HatCommand hatCommand = new HatCommand(this);
        getCommand("hat").setExecutor(hatCommand);
        getCommand("hat").setTabCompleter(hatCommand);

        SpeedCommand speedCommand = new SpeedCommand(this);
        getCommand("speed").setExecutor(speedCommand);
        getCommand("speed").setTabCompleter(speedCommand);

        GamemodeCommand gamemodeCommand = new GamemodeCommand(this);
        getCommand("gamemode").setExecutor(gamemodeCommand);
        getCommand("gamemode").setTabCompleter(gamemodeCommand);
        getCommand("gm").setExecutor(gamemodeCommand);
        getCommand("gm").setTabCompleter(gamemodeCommand);
        getCommand("gms").setExecutor(gamemodeCommand);
        getCommand("gms").setTabCompleter(gamemodeCommand);
        getCommand("gmc").setExecutor(gamemodeCommand);
        getCommand("gmc").setTabCompleter(gamemodeCommand);
        getCommand("gma").setExecutor(gamemodeCommand);
        getCommand("gma").setTabCompleter(gamemodeCommand);
        getCommand("gmsp").setExecutor(gamemodeCommand);
        getCommand("gmsp").setTabCompleter(gamemodeCommand);

        BackCommand backCommand = null;
        if (settings.getBoolean("enabled-modules.back", true)) {
            backCommand = new BackCommand(this);
            getCommand("back").setExecutor(backCommand);
        }

        if (settings.getBoolean("enabled-modules.tpa", true)) {
            TpaCommand tpaCommand = new TpaCommand(this);
            getCommand("tpa").setExecutor(tpaCommand);
            getCommand("tpa").setTabCompleter(tpaCommand);
            getCommand("tpaccept").setExecutor(tpaCommand);
            getCommand("tpdeny").setExecutor(tpaCommand);
            getCommand("tpacancel").setExecutor(tpaCommand);
            getCommand("tptoggle").setExecutor(tpaCommand);
        }

        GodCommand godCommand = new GodCommand(this);
        getCommand("god").setExecutor(godCommand);
        getCommand("god").setTabCompleter(godCommand);

        VanishCommand vanishCommand = new VanishCommand(this);
        getCommand("vanish").setExecutor(vanishCommand);
        getCommand("vanish").setTabCompleter(vanishCommand);

        EnderChestSeeCommand enderChestSeeCommand = new EnderChestSeeCommand(this);
        getCommand("enderchestsee").setExecutor(enderChestSeeCommand);
        getCommand("enderchestsee").setTabCompleter(enderChestSeeCommand);

        InvSeeCommand invSeeCommand = new InvSeeCommand(this);
        getCommand("invsee").setExecutor(invSeeCommand);
        getCommand("invsee").setTabCompleter(invSeeCommand);
        getServer().getPluginManager().registerEvents(invSeeCommand, this);

        if (settings.getBoolean("enabled-modules.spawn", true)) {
            SpawnCommand spawnCommand = new SpawnCommand(this);
            getCommand("spawn").setExecutor(spawnCommand);
            getCommand("spawn").setTabCompleter(spawnCommand);

            getCommand("setspawn").setExecutor(new SetSpawnCommand(this));
        }

        // Chat control
        BroadcastCommand broadcastCommand = new BroadcastCommand(this);
        getCommand("broadcast").setExecutor(broadcastCommand);
        getCommand("broadcast").setTabCompleter(broadcastCommand);

        ClearchatCommand clearchatCommand = new ClearchatCommand(this);
        getCommand("clearchat").setExecutor(clearchatCommand);
        getCommand("clearchat").setTabCompleter(clearchatCommand);

        MutechatCommand mutechatCommand = new MutechatCommand(this);
        getCommand("mutechat").setExecutor(mutechatCommand);
        getCommand("mutechat").setTabCompleter(mutechatCommand);

        SlowchatCommand slowchatCommand = new SlowchatCommand(this);
        getCommand("slowchat").setExecutor(slowchatCommand);
        getCommand("slowchat").setTabCompleter(slowchatCommand);

        StaffchatCommand staffchatCommand = new StaffchatCommand(this);
        getCommand("staffchat").setExecutor(staffchatCommand);
        getCommand("staffchat").setTabCompleter(staffchatCommand);

        // Messaging
        if (settings.getBoolean("enabled-modules.messaging", true)) {
            MsgCommand msgCommand = new MsgCommand(this);
            getCommand("msg").setExecutor(msgCommand);
            getCommand("msg").setTabCompleter(msgCommand);

            ReplyCommand replyCommand = new ReplyCommand(this);
            getCommand("reply").setExecutor(replyCommand);
            getCommand("reply").setTabCompleter(replyCommand);

            MsgtoggleCommand msgtoggleCommand = new MsgtoggleCommand(this);
            getCommand("msgtoggle").setExecutor(msgtoggleCommand);
            getCommand("msgtoggle").setTabCompleter(msgtoggleCommand);
        }

        // Formatting
        MeCommand meCommand = new MeCommand(this);
        getCommand("me").setExecutor(meCommand);
        getCommand("me").setTabCompleter(meCommand);

        NickCommand nickCommand = new NickCommand(this);
        getCommand("nick").setExecutor(nickCommand);
        getCommand("nick").setTabCompleter(nickCommand);

        RealnameCommand realnameCommand = new RealnameCommand(this);
        getCommand("realname").setExecutor(realnameCommand);
        getCommand("realname").setTabCompleter(realnameCommand);

        SocialCommand socialCommand = new SocialCommand(this);
        getCommand("discord").setExecutor(socialCommand);
        getCommand("discord").setTabCompleter(socialCommand);
        getCommand("website").setExecutor(socialCommand);
        getCommand("website").setTabCompleter(socialCommand);
        getCommand("store").setExecutor(socialCommand);
        getCommand("store").setTabCompleter(socialCommand);

        TimeCommand timeCommand = new TimeCommand(this);
        getCommand("time").setExecutor(timeCommand);
        getCommand("time").setTabCompleter(timeCommand);
        getCommand("day").setExecutor(timeCommand);
        getCommand("day").setTabCompleter(timeCommand);
        getCommand("night").setExecutor(timeCommand);
        getCommand("night").setTabCompleter(timeCommand);
        getCommand("noon").setExecutor(timeCommand);
        getCommand("noon").setTabCompleter(timeCommand);
        getCommand("midnight").setExecutor(timeCommand);
        getCommand("midnight").setTabCompleter(timeCommand);

        // ── Smart Moderation commands (v1.5.0) ──

        CaseCommand caseCommand = new CaseCommand(this);
        getCommand("case").setExecutor(caseCommand);
        getCommand("case").setTabCompleter(caseCommand);

        HistoryCommand historyCommand = new HistoryCommand(this);
        getCommand("history").setExecutor(historyCommand);
        getCommand("history").setTabCompleter(historyCommand);

        NoteCommand noteCommand = new NoteCommand(this);
        getCommand("note").setExecutor(noteCommand);
        getCommand("note").setTabCompleter(noteCommand);

        NotesCommand notesCommand = new NotesCommand(this);
        getCommand("notes").setExecutor(notesCommand);
        getCommand("notes").setTabCompleter(notesCommand);

        PunishCommand punishCommand = new PunishCommand(this);
        getCommand("punish").setExecutor(punishCommand);
        getCommand("punish").setTabCompleter(punishCommand);

        AuditLogCommand auditLogCommand = new AuditLogCommand(this);
        getCommand("auditlog").setExecutor(auditLogCommand);
        getCommand("auditlog").setTabCompleter(auditLogCommand);

        // ── /help override ──

        try {
            java.lang.reflect.Field commandMapField = getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            SimpleCommandMap commandMap = (SimpleCommandMap) commandMapField.get(getServer());

            // Use reflection to get the knownCommands field from SimpleCommandMap
            java.lang.reflect.Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            java.util.Map<String, org.bukkit.command.Command> knownCommands =
                    (java.util.Map<String, org.bukkit.command.Command>) knownCommandsField.get(commandMap);

            knownCommands.remove("help");
            knownCommands.remove("bukkit:help");

            HelpCommand helpCommand = new HelpCommand(this);
            commandMap.register("axentra", new org.bukkit.command.Command("help") {
                @Override
                public boolean execute(org.bukkit.command.CommandSender sender, String label, String[] args) {
                    return helpCommand.onCommand(sender, this, label, args);
                }

                @Override
                public java.util.List<String> tabComplete(org.bukkit.command.CommandSender sender, String alias, String[] args) {
                    return helpCommand.onTabComplete(sender, this, alias, args);
                }
            });
        } catch (Exception e) {
            getLogger().warning("Failed to register /help command: " + e.getMessage());
        }

        // ── Listeners ──

        if (backCommand != null) {
            getServer().getPluginManager().registerEvents(new BackListener(backCommand), this);
        }
        getServer().getPluginManager().registerEvents(new GodListener(godCommand), this);
        getServer().getPluginManager().registerEvents(new LoginListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinLeaveListener(this), this);

        this.chatListener = new ChatListener(this, staffchatCommand);
        getServer().getPluginManager().registerEvents(chatListener, this);

        getServer().getPluginManager().registerEvents(new StaffGUIListener(this, axentraCommand.getStaffGUI()), this);
        getServer().getPluginManager().registerEvents(new UpdateListener(this, axentraCommand), this);
        getServer().getPluginManager().registerEvents(new VanishListener(this, vanishCommand), this);
        getServer().getPluginManager().registerEvents(new ModerationGUIListener(this), this);

        String CYAN = "\u001B[36m";
        String GREEN = "\u001B[32m";
        String RESET = "\u001B[0m";

        getLogger().info(CYAN + "  ================================  " + RESET);
        getLogger().info(CYAN + "       A  X  E  N  T  R  A         " + RESET);
        getLogger().info(CYAN + "  ================================  " + RESET);
        getLogger().info(CYAN + "  Version: " + RESET + getDescription().getVersion());
        getLogger().info(CYAN + "  Author:  " + RESET + String.join(", ", getDescription().getAuthors()));
        getLogger().info(GREEN + "  Status:  Successfully started!   " + RESET);
        getLogger().info(CYAN + "  ================================  " + RESET);
    }

    @Override
    public void onDisable() {
        databaseManager.close();

        String CYAN = "\u001B[36m";
        String RED = "\u001B[31m";
        String RESET = "\u001B[0m";

        getLogger().info(CYAN + "  ================================  " + RESET);
        getLogger().info(RED +  "  Axentra has been stopped. Goodbye!" + RESET);
        getLogger().info(CYAN + "  ================================  " + RESET);
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ChatListener getChatListener() {
        return chatListener;
    }

    // Chat control
    public boolean isChatMuted() { return chatMuted; }
    public void setChatMuted(boolean muted) { this.chatMuted = muted; }

    public int getSlowChatSeconds() { return slowChatSeconds; }
    public void setSlowChatSeconds(int seconds) { this.slowChatSeconds = seconds; }

    public Map<UUID, Long> getLastChatTime() { return lastChatTime; }

    public boolean isStaffChatToggled(UUID uuid) { return staffChatToggled.contains(uuid); }
    public boolean toggleStaffChat(UUID uuid) {
        if (staffChatToggled.contains(uuid)) { staffChatToggled.remove(uuid); return false; }
        else { staffChatToggled.add(uuid); return true; }
    }

    // Messaging
    public UUID getLastMessaged(UUID uuid) { return lastMessaged.get(uuid); }
    public void setLastMessaged(UUID sender, UUID target) {
        if (target != null) lastMessaged.put(sender, target);
    }

    public boolean isMsgToggleOff(UUID uuid) { return msgToggleOff.contains(uuid); }
    public boolean toggleMsgToggle(UUID uuid) {
        if (msgToggleOff.contains(uuid)) { msgToggleOff.remove(uuid); return false; }
        else { msgToggleOff.add(uuid); return true; }
    }
}
