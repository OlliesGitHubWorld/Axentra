package mc.ollie;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import mc.ollie.command.*;
import mc.ollie.health.*;
import mc.ollie.listeners.*;
import org.bukkit.command.SimpleCommandMap;
import org.bstats.bukkit.Metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class App extends JavaPlugin {

    private FileManager fileManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private ChatListener chatListener;
    private DutyManager dutyManager;
    private StaffmodeManager staffmodeManager;
    private SnapshotManager snapshotManager;
    private TrustManager trustManager;
    private VpnCheckManager vpnCheckManager;
    private TickProfiler tickProfiler;
    private HealthScoreService healthScoreService;
    private ChunkActivityTracker chunkActivityTracker;
    private TpsAlertManager tpsAlertManager;
    private CleanupManager cleanupManager;
    private RestartManager restartManager;
    private MaintenanceManager maintenanceManager;
    private FreezeManager freezeManager;
    private mc.ollie.discord.DiscordManager discordManager;
    private mc.ollie.discord.SlashCommandManager slashCommandManager;
    private mc.ollie.discord.LinkService linkService;
    private mc.ollie.discord.ChannelRelay channelRelay;
    private mc.ollie.discord.EmbedFactory embedFactory;
    private mc.ollie.discord.DiscordCommands discordCommands;
    private mc.ollie.discord.AppealService appealService;
    private mc.ollie.discord.ConsoleBridge consoleBridge;
    private mc.ollie.gui.AppealsGUI appealsGUI;
    private VanishCommand vanishCommand;
    private GodCommand godCommand;
    private AfkManager afkManager;
    private mc.ollie.punish.PunishmentService punishmentService;
    private final Map<UUID, Long> reportCooldowns = new HashMap<>();
    private boolean verboseLogging = true;

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

        this.dutyManager = new DutyManager(this);
        this.staffmodeManager = new StaffmodeManager();
        this.punishmentService = new mc.ollie.punish.PunishmentService(this);

        int pluginId = 30019;
        Metrics metrics = new Metrics(this, pluginId);

        FileConfiguration settings = fileManager.getSettings();
        this.verboseLogging = settings.getBoolean("verbose-logging", true);

        // ── Core (always enabled) ──

        AxentraCommand axentraCommand = new AxentraCommand(this);
        getCommand("axentra").setExecutor(axentraCommand);
        getCommand("axentra").setTabCompleter(axentraCommand);

        // ── Moderation (always enabled — core feature) ──

        KickCommand kickCommand = new KickCommand(this);
        getCommand("kick").setExecutor(kickCommand);
        getCommand("kick").setTabCompleter(kickCommand);

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

        BroadcastCommand broadcastCommand = new BroadcastCommand(this);
        getCommand("broadcast").setExecutor(broadcastCommand);
        getCommand("broadcast").setTabCompleter(broadcastCommand);

        // ── Smart Moderation (v1.5.0) ──

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

        // ── Module: staff-management ──

        StaffmodeListener staffmodeListener = null;
        if (settings.getBoolean("enabled-modules.staff-management", true)) {
            DutyCommand dutyCommand = new DutyCommand(this);
            getCommand("duty").setExecutor(dutyCommand);
            getCommand("duty").setTabCompleter(dutyCommand);

            StaffmodeCommand staffmodeCommand = new StaffmodeCommand(this);
            getCommand("staffmode").setExecutor(staffmodeCommand);
            getCommand("staffmode").setTabCompleter(staffmodeCommand);

            ReportCommand reportCommand = new ReportCommand(this);
            getCommand("report").setExecutor(reportCommand);
            getCommand("report").setTabCompleter(reportCommand);

            ReportsCommand reportsCommand = new ReportsCommand(this);
            getCommand("reports").setExecutor(reportsCommand);
            getCommand("reports").setTabCompleter(reportsCommand);

            StaffstatsCommand staffstatsCommand = new StaffstatsCommand(this);
            getCommand("staffstats").setExecutor(staffstatsCommand);
            getCommand("staffstats").setTabCompleter(staffstatsCommand);

            StafflistCommand stafflistCommand = new StafflistCommand(this);
            getCommand("stafflist").setExecutor(stafflistCommand);
            getCommand("stafflist").setTabCompleter(stafflistCommand);

            staffmodeListener = new StaffmodeListener(this);
        }

        // ── Module: player-utilities ──

        this.godCommand = new GodCommand(this);
        if (settings.getBoolean("enabled-modules.player-utilities", true)) {
            ClearCommand clearCommand = new ClearCommand(this);
            getCommand("clear").setExecutor(clearCommand);
            getCommand("clear").setTabCompleter(clearCommand);

            getCommand("repair").setExecutor(new RepairCommand(this));

            FlyCommand flyCommand = new FlyCommand(this);
            getCommand("fly").setExecutor(flyCommand);
            getCommand("fly").setTabCompleter(flyCommand);

            HealCommand healCommand = new HealCommand(this);
            getCommand("heal").setExecutor(healCommand);
            getCommand("heal").setTabCompleter(healCommand);

            FeedCommand feedCommand = new FeedCommand(this);
            getCommand("feed").setExecutor(feedCommand);
            getCommand("feed").setTabCompleter(feedCommand);

            getCommand("god").setExecutor(godCommand);
            getCommand("god").setTabCompleter(godCommand);

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

            PingCommand pingCommand = new PingCommand(this);
            getCommand("ping").setExecutor(pingCommand);
            getCommand("ping").setTabCompleter(pingCommand);

            SuicideCommand suicideCommand = new SuicideCommand(this);
            getCommand("suicide").setExecutor(suicideCommand);
            getCommand("suicide").setTabCompleter(suicideCommand);
        }

        // ── Module: vanish ──

        this.vanishCommand = new VanishCommand(this);
        if (settings.getBoolean("enabled-modules.vanish", true)) {
            getCommand("vanish").setExecutor(vanishCommand);
            getCommand("vanish").setTabCompleter(vanishCommand);
        }

        // ── Module: invsee ──

        InvSeeCommand invSeeCommand = null;
        if (settings.getBoolean("enabled-modules.invsee", true)) {
            EnderChestSeeCommand enderChestSeeCommand = new EnderChestSeeCommand(this);
            getCommand("enderchestsee").setExecutor(enderChestSeeCommand);
            getCommand("enderchestsee").setTabCompleter(enderChestSeeCommand);

            invSeeCommand = new InvSeeCommand(this);
            getCommand("invsee").setExecutor(invSeeCommand);
            getCommand("invsee").setTabCompleter(invSeeCommand);
            getServer().getPluginManager().registerEvents(invSeeCommand, this);
        }

        // ── Module: workbench-commands ──

        if (settings.getBoolean("enabled-modules.workbench-commands", true)) {
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
        }

        // ── Module: weather-time ──

        if (settings.getBoolean("enabled-modules.weather-time", true)) {
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
        }

        // ── Module: spawn ──

        if (settings.getBoolean("enabled-modules.spawn", true)) {
            SpawnCommand spawnCommand = new SpawnCommand(this);
            getCommand("spawn").setExecutor(spawnCommand);
            getCommand("spawn").setTabCompleter(spawnCommand);

            getCommand("setspawn").setExecutor(new SetSpawnCommand(this));

            getServer().getPluginManager().registerEvents(new RespawnListener(spawnCommand), this);
        }

        // ── Module: back ──

        BackCommand backCommand = null;
        if (settings.getBoolean("enabled-modules.back", true)) {
            backCommand = new BackCommand(this);
            getCommand("back").setExecutor(backCommand);
        }

        // ── Module: tpa ──

        if (settings.getBoolean("enabled-modules.tpa", true)) {
            TpaCommand tpaCommand = new TpaCommand(this);
            getCommand("tpa").setExecutor(tpaCommand);
            getCommand("tpa").setTabCompleter(tpaCommand);
            getCommand("tpaccept").setExecutor(tpaCommand);
            getCommand("tpdeny").setExecutor(tpaCommand);
            getCommand("tpacancel").setExecutor(tpaCommand);
            getCommand("tptoggle").setExecutor(tpaCommand);
        }

        // ── Module: chat-management ──

        StaffchatCommand staffchatCommand = new StaffchatCommand(this);
        if (settings.getBoolean("enabled-modules.chat-management", true)) {
            ClearchatCommand clearchatCommand = new ClearchatCommand(this);
            getCommand("clearchat").setExecutor(clearchatCommand);
            getCommand("clearchat").setTabCompleter(clearchatCommand);

            MutechatCommand mutechatCommand = new MutechatCommand(this);
            getCommand("mutechat").setExecutor(mutechatCommand);
            getCommand("mutechat").setTabCompleter(mutechatCommand);

            SlowchatCommand slowchatCommand = new SlowchatCommand(this);
            getCommand("slowchat").setExecutor(slowchatCommand);
            getCommand("slowchat").setTabCompleter(slowchatCommand);

            getCommand("staffchat").setExecutor(staffchatCommand);
            getCommand("staffchat").setTabCompleter(staffchatCommand);
        }

        // ── Module: messaging ──

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

        // ── Module: nickname ──

        if (settings.getBoolean("enabled-modules.nickname", true)) {
            MeCommand meCommand = new MeCommand(this);
            getCommand("me").setExecutor(meCommand);
            getCommand("me").setTabCompleter(meCommand);

            NickCommand nickCommand = new NickCommand(this);
            getCommand("nick").setExecutor(nickCommand);
            getCommand("nick").setTabCompleter(nickCommand);

            RealnameCommand realnameCommand = new RealnameCommand(this);
            getCommand("realname").setExecutor(realnameCommand);
            getCommand("realname").setTabCompleter(realnameCommand);
        }

        // ── Module: social-commands ──

        if (settings.getBoolean("enabled-modules.social-commands", true)) {
            SocialCommand socialCommand = new SocialCommand(this);
            getCommand("discord").setExecutor(socialCommand);
            getCommand("discord").setTabCompleter(socialCommand);
            getCommand("website").setExecutor(socialCommand);
            getCommand("website").setTabCompleter(socialCommand);
            getCommand("store").setExecutor(socialCommand);
            getCommand("store").setTabCompleter(socialCommand);
        }

        // ── Module: snapshots ──

        if (settings.getBoolean("enabled-modules.snapshots", true)) {
            this.snapshotManager = new SnapshotManager(this);

            RestoreInvCommand restoreInvCommand = new RestoreInvCommand(this);
            getCommand("restoreinv").setExecutor(restoreInvCommand);
            getCommand("restoreinv").setTabCompleter(restoreInvCommand);
        }

        // ── Module: trust ──

        if (settings.getBoolean("enabled-modules.trust", true)) {
            this.trustManager = new TrustManager(this);
            trustManager.start();

            TrustCommand trustCommand = new TrustCommand(this);
            getCommand("trust").setExecutor(trustCommand);
            getCommand("trust").setTabCompleter(trustCommand);
        }

        // ── Module: alts ──

        if (settings.getBoolean("enabled-modules.alts", true)) {
            AltsCommand altsCommand = new AltsCommand(this);
            getCommand("alts").setExecutor(altsCommand);
            getCommand("alts").setTabCompleter(altsCommand);
        }

        // ── Module: vpn ──

        if (settings.getBoolean("enabled-modules.vpn", true)) {
            this.vpnCheckManager = new VpnCheckManager(this);
        }

        // ── Module: freeze ──

        if (settings.getBoolean("enabled-modules.freeze", true)) {
            this.freezeManager = new FreezeManager(this);
            freezeManager.start();

            FreezeCommand freezeCommand = new FreezeCommand(this, freezeManager);
            getCommand("freeze").setExecutor(freezeCommand);
            getCommand("freeze").setTabCompleter(freezeCommand);
            getCommand("unfreeze").setExecutor(freezeCommand);
            getCommand("unfreeze").setTabCompleter(freezeCommand);
            getCommand("tempfreeze").setExecutor(freezeCommand);
            getCommand("tempfreeze").setTabCompleter(freezeCommand);
        }

        // ── Module: timeline ──

        if (settings.getBoolean("enabled-modules.timeline", true)) {
            TimelineCommand timelineCommand = new TimelineCommand(this);
            getCommand("timeline").setExecutor(timelineCommand);
            getCommand("timeline").setTabCompleter(timelineCommand);
        }

        // ── Module: homes ──

        if (settings.getBoolean("enabled-modules.homes", true)) {
            SetHomeCommand setHomeCommand = new SetHomeCommand(this);
            getCommand("sethome").setExecutor(setHomeCommand);
            getCommand("sethome").setTabCompleter(setHomeCommand);

            HomeCommand homeCommand = new HomeCommand(this);
            getCommand("home").setExecutor(homeCommand);
            getCommand("home").setTabCompleter(homeCommand);

            DelHomeCommand delHomeCommand = new DelHomeCommand(this);
            getCommand("delhome").setExecutor(delHomeCommand);
            getCommand("delhome").setTabCompleter(delHomeCommand);

            HomesCommand homesCommand = new HomesCommand(this);
            getCommand("homes").setExecutor(homesCommand);
            getCommand("homes").setTabCompleter(homesCommand);
        }

        // ── Module: teleport ──

        if (settings.getBoolean("enabled-modules.teleport", true)) {
            TpCommand tpCommand = new TpCommand(this);
            getCommand("tp").setExecutor(tpCommand);
            getCommand("tp").setTabCompleter(tpCommand);
            getCommand("tphere").setExecutor(tpCommand);
            getCommand("tphere").setTabCompleter(tpCommand);
            getCommand("tpall").setExecutor(tpCommand);
            getCommand("tpall").setTabCompleter(tpCommand);
            getCommand("tpahere").setExecutor(tpCommand);
            getCommand("tpahere").setTabCompleter(tpCommand);
            getCommand("top").setExecutor(tpCommand);
            getCommand("top").setTabCompleter(tpCommand);
        }

        // ── Module: lag / health (v1.8.0) ──

        if (settings.getBoolean("enabled-modules.lag", true)) {
            this.tickProfiler = new TickProfiler(this);
            tickProfiler.start();

            if (settings.getBoolean("enabled-modules.health-score", true)) {
                this.healthScoreService = new HealthScoreService(this);
            }

            TpsHistoryRecorder recorder = new TpsHistoryRecorder(this);
            recorder.start();

            this.chunkActivityTracker = new ChunkActivityTracker(this);
            chunkActivityTracker.start();

            LagCommand lagCommand = new LagCommand(this);
            getCommand("lag").setExecutor(lagCommand);
            getCommand("lag").setTabCompleter(lagCommand);
        }

        if (settings.getBoolean("enabled-modules.alerts", true) && tickProfiler != null) {
            this.tpsAlertManager = new TpsAlertManager(this);
            tpsAlertManager.start();
        }

        if (settings.getBoolean("enabled-modules.cleanup", true)) {
            this.cleanupManager = new CleanupManager(this);
            cleanupManager.start();
        }

        if (settings.getBoolean("enabled-modules.restarts", true)) {
            this.restartManager = new RestartManager(this);
            restartManager.start();

            RestartCommand restartCommand = new RestartCommand(this);
            getCommand("restart").setExecutor(restartCommand);
            getCommand("restart").setTabCompleter(restartCommand);
        }

        if (settings.getBoolean("enabled-modules.maintenance", true)) {
            this.maintenanceManager = new MaintenanceManager(this);
            maintenanceManager.load();

            MaintenanceCommand maintenanceCommand = new MaintenanceCommand(this);
            getCommand("maintenance").setExecutor(maintenanceCommand);
            getCommand("maintenance").setTabCompleter(maintenanceCommand);
        }

        // ── Module: warps ──

        if (settings.getBoolean("enabled-modules.warps", true)) {
            WarpCommand warpCommand = new WarpCommand(this);
            getCommand("setwarp").setExecutor(warpCommand);
            getCommand("setwarp").setTabCompleter(warpCommand);
            getCommand("delwarp").setExecutor(warpCommand);
            getCommand("delwarp").setTabCompleter(warpCommand);
            getCommand("warp").setExecutor(warpCommand);
            getCommand("warp").setTabCompleter(warpCommand);
            getCommand("editwarp").setExecutor(warpCommand);
            getCommand("editwarp").setTabCompleter(warpCommand);

            WarpsCommand warpsCommand = new WarpsCommand(this);
            getCommand("warps").setExecutor(warpsCommand);
            getCommand("warps").setTabCompleter(warpsCommand);
        }

        // ── Module: rtp ──

        if (settings.getBoolean("enabled-modules.rtp", true)) {
            RtpCommand rtpCommand = new RtpCommand(this);
            getCommand("rtp").setExecutor(rtpCommand);
            getCommand("rtp").setTabCompleter(rtpCommand);
        }

        // ── Player info (v1.9.0) ──

        if (settings.getBoolean("enabled-modules.afk", true)) {
            this.afkManager = new AfkManager(this);
            getServer().getPluginManager().registerEvents(afkManager, this);
            afkManager.start();
            AfkCommand afkCommand = new AfkCommand(this);
            getCommand("afk").setExecutor(afkCommand);
            getCommand("afk").setTabCompleter(afkCommand);
        }
        if (settings.getBoolean("enabled-modules.seen", true)) {
            SeenCommand seenCommand = new SeenCommand(this);
            getCommand("seen").setExecutor(seenCommand);
            getCommand("seen").setTabCompleter(seenCommand);
        }
        if (settings.getBoolean("enabled-modules.whois", true)) {
            WhoisCommand whoisCommand = new WhoisCommand(this);
            getCommand("whois").setExecutor(whoisCommand);
            getCommand("whois").setTabCompleter(whoisCommand);
        }
        if (settings.getBoolean("enabled-modules.near", true)) {
            NearCommand nearCommand = new NearCommand(this);
            getCommand("near").setExecutor(nearCommand);
            getCommand("near").setTabCompleter(nearCommand);
        }
        if (settings.getBoolean("enabled-modules.list", true)) {
            ListCommand listCommand = new ListCommand(this);
            getCommand("list").setExecutor(listCommand);
            getCommand("list").setTabCompleter(listCommand);
        }

        // ── /help override ──

        try {
            java.lang.reflect.Field commandMapField = getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            SimpleCommandMap commandMap = (SimpleCommandMap) commandMapField.get(getServer());

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

            // ── /restart override ──
            // Spigot registers its own /restart before plugins, so ours only gets axentra:restart.
            // Point the plain label at ours; Spigot's stays reachable as /spigot:restart.
            if (settings.getBoolean("enabled-modules.restarts", true)) {
                knownCommands.put("restart", getCommand("restart"));
            }
            // ── /list override (v1.9.0) — vanilla's /list stays reachable as /minecraft:list ──
            if (settings.getBoolean("enabled-modules.list", true)) {
                knownCommands.put("list", getCommand("list"));
            }

            // ── Unregister commands for disabled modules ──
            // Fully removes disabled commands from the server so no other plugin can see them
            // and players get "Unknown command" instead of usage text.

            List<String> toRemove = new ArrayList<>();

            if (!settings.getBoolean("enabled-modules.tpa", true)) {
                toRemove.addAll(java.util.Arrays.asList("tpa", "tpaccept", "tpdeny", "tpacancel", "tptoggle"));
            }
            if (!settings.getBoolean("enabled-modules.spawn", true)) {
                toRemove.addAll(java.util.Arrays.asList("spawn", "setspawn"));
            }
            if (!settings.getBoolean("enabled-modules.back", true)) {
                toRemove.addAll(java.util.Arrays.asList("back"));
            }
            if (!settings.getBoolean("enabled-modules.messaging", true)) {
                toRemove.addAll(java.util.Arrays.asList("msg", "tell", "whisper", "w", "message", "pm", "reply", "r", "msgtoggle"));
            }
            if (!settings.getBoolean("enabled-modules.chat-management", true)) {
                toRemove.addAll(java.util.Arrays.asList("clearchat", "cc", "mutechat", "mc", "slowchat", "slowmode", "staffchat", "sc"));
            }
            if (!settings.getBoolean("enabled-modules.nickname", true)) {
                toRemove.addAll(java.util.Arrays.asList("me", "nick", "nickname", "realname"));
            }
            if (!settings.getBoolean("enabled-modules.social-commands", true)) {
                toRemove.addAll(java.util.Arrays.asList("discord", "website", "store"));
            }
            if (!settings.getBoolean("enabled-modules.player-utilities", true)) {
                toRemove.addAll(java.util.Arrays.asList("clear", "c", "repair", "fly", "heal", "feed", "god", "hat", "speed",
                        "gamemode", "gm", "gms", "gmc", "gma", "gmsp", "ping", "suicide"));
            }
            if (!settings.getBoolean("enabled-modules.vanish", true)) {
                toRemove.addAll(java.util.Arrays.asList("vanish", "v"));
            }
            if (!settings.getBoolean("enabled-modules.invsee", true)) {
                toRemove.addAll(java.util.Arrays.asList("invsee", "enderchestsee", "ecsee", "echestsee"));
            }
            if (!settings.getBoolean("enabled-modules.workbench-commands", true)) {
                toRemove.addAll(java.util.Arrays.asList("anvil", "workbench", "wb", "craft", "craftingtable",
                        "cartography", "cartographytable", "carto", "grindstone", "grind", "loom",
                        "smithingtable", "smithing", "smith", "stonecutter", "stone", "enderchest", "ec", "echest"));
            }
            if (!settings.getBoolean("enabled-modules.weather-time", true)) {
                toRemove.addAll(java.util.Arrays.asList("weather", "sun", "sky", "rain", "storm", "time", "day", "night", "noon", "midnight"));
            }
            if (!settings.getBoolean("enabled-modules.staff-management", true)) {
                toRemove.addAll(java.util.Arrays.asList("duty", "staffmode", "sm", "report", "reports", "staffstats", "stafflist"));
            }
            if (!settings.getBoolean("enabled-modules.snapshots", true)) {
                toRemove.addAll(java.util.Arrays.asList("restoreinv"));
            }
            if (!settings.getBoolean("enabled-modules.trust", true)) {
                toRemove.addAll(java.util.Arrays.asList("trust"));
            }
            if (!settings.getBoolean("enabled-modules.alts", true)) {
                toRemove.addAll(java.util.Arrays.asList("alts"));
            }
            if (!settings.getBoolean("enabled-modules.freeze", true)) {
                toRemove.addAll(java.util.Arrays.asList("freeze", "unfreeze", "tempfreeze"));
            }
            if (!settings.getBoolean("enabled-modules.timeline", true)) {
                toRemove.addAll(java.util.Arrays.asList("timeline"));
            }
            if (!settings.getBoolean("enabled-modules.homes", true)) {
                toRemove.addAll(java.util.Arrays.asList("sethome", "home", "delhome", "homes"));
            }
            if (!settings.getBoolean("enabled-modules.teleport", true)) {
                toRemove.addAll(java.util.Arrays.asList("tp", "tphere", "tpall", "tpahere", "top"));
            }
            if (!settings.getBoolean("enabled-modules.lag", true)) {
                toRemove.addAll(java.util.Arrays.asList("lag"));
            }
            if (!settings.getBoolean("enabled-modules.restarts", true)) {
                toRemove.addAll(java.util.Arrays.asList("restart"));
            }
            if (!settings.getBoolean("enabled-modules.maintenance", true)) {
                toRemove.addAll(java.util.Arrays.asList("maintenance"));
            }
            if (!settings.getBoolean("enabled-modules.warps", true)) {
                toRemove.addAll(java.util.Arrays.asList("setwarp", "delwarp", "warp", "warps", "editwarp"));
            }
            if (!settings.getBoolean("enabled-modules.rtp", true)) {
                toRemove.addAll(java.util.Arrays.asList("rtp", "wild"));
            }
            if (!settings.getBoolean("enabled-modules.discord", true)) {
                toRemove.addAll(java.util.Arrays.asList("link", "unlink", "appeals"));
            }
            if (!settings.getBoolean("enabled-modules.afk", true)) toRemove.add("afk");
            if (!settings.getBoolean("enabled-modules.seen", true)) toRemove.add("seen");
            if (!settings.getBoolean("enabled-modules.whois", true)) toRemove.add("whois");
            if (!settings.getBoolean("enabled-modules.near", true)) toRemove.add("near");
            if (!settings.getBoolean("enabled-modules.list", true)) {
                // Only remove our own entries, so vanilla /list keeps working
                knownCommands.remove("axentra:list");
                knownCommands.remove("axentra:online");
                knownCommands.remove("axentra:who");
                if (knownCommands.get("list") == getCommand("list")) knownCommands.remove("list");
                if (knownCommands.get("online") == getCommand("list")) knownCommands.remove("online");
                if (knownCommands.get("who") == getCommand("list")) knownCommands.remove("who");
            }

            for (String cmd : toRemove) {
                knownCommands.remove(cmd);
                knownCommands.remove("axentra:" + cmd);
            }

        } catch (Exception e) {
            getLogger().warning("Failed to register /help command: " + e.getMessage());
        }

        // ── Listeners ──

        if (backCommand != null) {
            getServer().getPluginManager().registerEvents(new BackListener(backCommand), this);
        }
        if (settings.getBoolean("enabled-modules.player-utilities", true)) {
            getServer().getPluginManager().registerEvents(new GodListener(godCommand), this);
        }
        getServer().getPluginManager().registerEvents(new LoginListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinLeaveListener(this), this);

        if (settings.getBoolean("enabled-modules.chat-management", true)) {
            this.chatListener = new ChatListener(this, staffchatCommand);
            getServer().getPluginManager().registerEvents(chatListener, this);
        }

        getServer().getPluginManager().registerEvents(new StaffGUIListener(this, axentraCommand.getStaffGUI()), this);
        getServer().getPluginManager().registerEvents(new UpdateListener(this, axentraCommand), this);
        if (settings.getBoolean("enabled-modules.vanish", true)) {
            getServer().getPluginManager().registerEvents(new VanishListener(this, vanishCommand), this);
        }
        getServer().getPluginManager().registerEvents(new MenuProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new ModerationGUIListener(this), this);

        if (staffmodeListener != null) {
            getServer().getPluginManager().registerEvents(staffmodeListener, this);
            getServer().getPluginManager().registerEvents(new ReportsGUIListener(this), this);
        }

        // Protection listener (session tracking, IP/name tracking, snapshots)
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);

        // Freeze listener (DB-backed, replaces old in-memory freeze)
        if (freezeManager != null) {
            getServer().getPluginManager().registerEvents(new FreezeListener(this, freezeManager), this);
        }

        // Chunk activity listener (v1.8.0)
        if (chunkActivityTracker != null) {
            getServer().getPluginManager().registerEvents(new ChunkActivityListener(this), this);
        }

        // Entity limit listener (v1.8.0)
        if (settings.getBoolean("enabled-modules.entity-limits", true)) {
            getServer().getPluginManager().registerEvents(new EntityLimitListener(this), this);
        }

        // Maintenance listener (v1.8.0)
        if (maintenanceManager != null) {
            getServer().getPluginManager().registerEvents(maintenanceManager, this);
        }

        // Warps GUI listener (v1.8.0)
        if (settings.getBoolean("enabled-modules.warps", true)) {
            getServer().getPluginManager().registerEvents(
                    (WarpsCommand) getCommand("warps").getExecutor(), this);
        }

        // Close any open sessions from a previous unclean shutdown
        databaseManager.closeOpenSessions();

        // Discord (v1.9.0) — connects async, the server never waits for it.
        // Features register their listeners and slash commands before the bot starts.
        if (settings.getBoolean("enabled-modules.discord", true)) {
            LinkCommand linkCommand = new LinkCommand(this);
            getCommand("link").setExecutor(linkCommand);
            getCommand("link").setTabCompleter(linkCommand);
            getCommand("unlink").setExecutor(linkCommand);
            getCommand("unlink").setTabCompleter(linkCommand);
            AppealsCommand appealsCommand = new AppealsCommand(this);
            getCommand("appeals").setExecutor(appealsCommand);
            getCommand("appeals").setTabCompleter(appealsCommand);
            try {
                this.discordManager = new mc.ollie.discord.DiscordManager(this);
                this.slashCommandManager = new mc.ollie.discord.SlashCommandManager(this, discordManager);
                this.linkService = new mc.ollie.discord.LinkService(this, discordManager, slashCommandManager);
                getServer().getPluginManager().registerEvents(linkService, this);
                mc.ollie.discord.RoleSyncService roleSync = new mc.ollie.discord.RoleSyncService(this, discordManager, linkService);
                getServer().getPluginManager().registerEvents(roleSync, this);
                this.embedFactory = new mc.ollie.discord.EmbedFactory(this, discordManager);
                this.channelRelay = new mc.ollie.discord.ChannelRelay(this, discordManager, embedFactory);
                getServer().getPluginManager().registerEvents(channelRelay, this);
                getServer().getPluginManager().registerEvents(
                        new mc.ollie.discord.ChatBridge(this, discordManager, channelRelay), this);
                this.consoleBridge = new mc.ollie.discord.ConsoleBridge(this, discordManager);
                this.discordCommands = new mc.ollie.discord.DiscordCommands(this, discordManager, embedFactory,
                        channelRelay, slashCommandManager);
                new mc.ollie.discord.DiscordModeration(this, discordManager, channelRelay, discordCommands);
                new mc.ollie.discord.WeeklyReportService(this, discordManager, embedFactory);
                this.appealService = new mc.ollie.discord.AppealService(this, discordManager, embedFactory, slashCommandManager);
                this.appealsGUI = new mc.ollie.gui.AppealsGUI(this, appealService);
                getServer().getPluginManager().registerEvents(appealsGUI, this);
                discordManager.start();
            } catch (LinkageError e) {
                // JDA is downloaded through plugin.yml libraries; this only fails without internet on first start
                discordManager = null;
                slashCommandManager = null;
                linkService = null;
                channelRelay = null;
                embedFactory = null;
                discordCommands = null;
                appealService = null;
                appealsGUI = null;
                getLogger().warning("The Discord library (JDA) could not be loaded, so Discord features are off. "
                        + "Make sure the server can reach Maven Central on its first start.");
            }
        }

        if (verboseLogging) {
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
    }

    @Override
    public void onDisable() {
        if (dutyManager != null) dutyManager.removeAll();
        if (restartManager != null) restartManager.stop();
        if (cleanupManager != null) cleanupManager.stop();
        if (freezeManager != null) freezeManager.stop();
        if (afkManager != null) afkManager.stop();
        if (consoleBridge != null) consoleBridge.stop();
        if (discordManager != null) {
            discordManager.stop();
            discordManager.webhooks().shutdown();
        }
        databaseManager.close();

        if (verboseLogging) {
            String CYAN = "\u001B[36m";
            String RED = "\u001B[31m";
            String RESET = "\u001B[0m";

            getLogger().info(CYAN + "  ================================  " + RESET);
            getLogger().info(RED +  "  Axentra has been stopped. Goodbye!" + RESET);
            getLogger().info(CYAN + "  ================================  " + RESET);
        }
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

    // Staff management
    public DutyManager getDutyManager() { return dutyManager; }
    public StaffmodeManager getStaffmodeManager() { return staffmodeManager; }
    public Map<UUID, Long> getReportCooldowns() { return reportCooldowns; }

    // Protection managers
    public SnapshotManager getSnapshotManager() { return snapshotManager; }
    public TrustManager getTrustManager() { return trustManager; }
    public VpnCheckManager getVpnCheckManager() { return vpnCheckManager; }

    // Health managers (v1.8.0)
    public TickProfiler getTickProfiler() { return tickProfiler; }
    public HealthScoreService getHealthScoreService() { return healthScoreService; }
    public ChunkActivityTracker getChunkActivityTracker() { return chunkActivityTracker; }
    public TpsAlertManager getTpsAlertManager() { return tpsAlertManager; }
    public CleanupManager getCleanupManager() { return cleanupManager; }
    public RestartManager getRestartManager() { return restartManager; }
    public MaintenanceManager getMaintenanceManager() { return maintenanceManager; }

    // Freeze
    public FreezeManager getFreezeManager() { return freezeManager; }

    // Discord (v1.9.0) — null when the module is off or JDA couldn't be loaded
    public mc.ollie.discord.DiscordManager getDiscordManager() { return discordManager; }
    public mc.ollie.discord.SlashCommandManager getSlashCommandManager() { return slashCommandManager; }
    public mc.ollie.discord.LinkService getLinkService() { return linkService; }
    public mc.ollie.discord.ChannelRelay getChannelRelay() { return channelRelay; }
    public mc.ollie.discord.EmbedFactory getEmbedFactory() { return embedFactory; }

    /**
     * The "appeal with /appeal <case> in Discord" line for ban screens, or "" when Discord appeals
     * are off. The ban-appeal-link from config.yml keeps working either way.
     */
    public String appealHint(int caseId) {
        if (caseId <= 0 || discordManager == null) return "";
        FileConfiguration discord = fileManager.getDiscord();
        if (!discord.getBoolean("enabled", false) || !discord.getBoolean("appeals.enabled", true)) return "";
        String hint = fileManager.getMessages().getString("ban-appeal-discord", "");
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', hint.replace("%case%", String.valueOf(caseId)));
    }

    /** Opens the /appeals GUI. Returns false when Discord (and so appeals) is unavailable. */
    public boolean openAppealsGui(org.bukkit.entity.Player player, boolean onlyOpen) {
        if (appealsGUI == null) return false;
        appealsGUI.open(player, onlyOpen, 0);
        return true;
    }

    /** Sends in-game staff chat to the Discord staffchat channel, if the bot is running. */
    public void relayStaffChat(String senderName, String message) {
        if (channelRelay != null) channelRelay.relayStaffChat(senderName, message);
    }

    public mc.ollie.punish.PunishmentService getPunishmentService() { return punishmentService; }

    public boolean isGod(org.bukkit.entity.Player player) {
        return godCommand != null && godCommand.isGod(player);
    }

    /** "X is AFK" for someone messaging the target, or null when the target isn't AFK. */
    public String afkNotice(org.bukkit.entity.Player target) {
        if (afkManager == null || !afkManager.isAfk(target)) return null;
        String reason = afkManager.getReason(target);
        String key = reason != null && !reason.isBlank() ? "msg-target-afk-reason" : "msg-target-afk";
        return messageManager.getMessage(key).replace("%player%", target.getName())
                .replace("%reason%", reason != null ? reason : "");
    }

    /** AFK manager (v1.9.0), null when the afk module is off. */
    public AfkManager getAfkManager() { return afkManager; }

    /** Whether a player is vanished with /vanish (false when the vanish module is off). */
    public boolean isVanished(org.bukkit.entity.Player player) {
        return vanishCommand != null && vanishCommand.isVanished(player);
    }

    // Logging
    public boolean isVerboseLogging() { return verboseLogging; }

    /** Logs an info message only if verbose-logging is enabled in config. */
    public void logInfo(String message) {
        if (verboseLogging) getLogger().info(message);
    }
}
