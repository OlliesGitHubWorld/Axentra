package mc.ollie;

import org.bukkit.plugin.java.JavaPlugin;
import mc.ollie.command.*;
import mc.ollie.listeners.BackListener;
import mc.ollie.listeners.ChatListener;
import mc.ollie.listeners.GodListener;
import mc.ollie.listeners.JoinLeaveListener;
import mc.ollie.listeners.LoginListener;
import mc.ollie.listeners.UpdateListener;
import org.bukkit.command.SimpleCommandMap;
import org.bstats.bukkit.Metrics;

public final class App extends JavaPlugin {

    private FileManager fileManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        this.fileManager = new FileManager(this);
        fileManager.setup();

        this.messageManager = new MessageManager(this);

        this.databaseManager = new DatabaseManager(this);
        databaseManager.setup();

        int pluginId = 30019;
        Metrics metrics = new Metrics(this, pluginId);

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

        getCommand("unban").setExecutor(new UnbanCommand(this));

        BanIpCommand banIpCommand = new BanIpCommand(this);
        getCommand("banip").setExecutor(banIpCommand);
        getCommand("banip").setTabCompleter(banIpCommand);

        getCommand("unbanip").setExecutor(new UnbanIpCommand(this));

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

        getCommand("unmute").setExecutor(new UnmuteCommand(this));

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

        BackCommand backCommand = new BackCommand(this);
        getCommand("back").setExecutor(backCommand);

        TpaCommand tpaCommand = new TpaCommand(this);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpa").setTabCompleter(tpaCommand);
        getCommand("tpaccept").setExecutor(tpaCommand);
        getCommand("tpdeny").setExecutor(tpaCommand);
        getCommand("tpacancel").setExecutor(tpaCommand);
        getCommand("tptoggle").setExecutor(tpaCommand);

        GodCommand godCommand = new GodCommand(this);
        getCommand("god").setExecutor(godCommand);
        getCommand("god").setTabCompleter(godCommand);

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
            });
        } catch (Exception e) {
            getLogger().warning("Failed to register /help command: " + e.getMessage());
        }

        getServer().getPluginManager().registerEvents(new BackListener(backCommand), this);
        getServer().getPluginManager().registerEvents(new GodListener(godCommand), this);
        getServer().getPluginManager().registerEvents(new LoginListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinLeaveListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new UpdateListener(this, axentraCommand), this);

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
}