package mc.ollie;

import org.bukkit.plugin.java.JavaPlugin;
import mc.ollie.command.*;
import mc.ollie.listeners.JoinLeaveListener;
import mc.ollie.listeners.LoginListener;
import mc.ollie.listeners.UpdateListener;
import org.bukkit.command.SimpleCommandMap;

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

        getCommand("unban").setExecutor(new UnbanCommand(this));

        FlyCommand flyCommand = new FlyCommand(this);
        getCommand("fly").setExecutor(flyCommand);
        getCommand("fly").setTabCompleter(flyCommand);

        HealCommand healCommand = new HealCommand(this);
        getCommand("heal").setExecutor(healCommand);
        getCommand("heal").setTabCompleter(healCommand);

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

        getServer().getPluginManager().registerEvents(new LoginListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinLeaveListener(this), this);
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