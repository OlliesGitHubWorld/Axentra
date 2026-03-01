package mc.ollie;

import org.bukkit.plugin.java.JavaPlugin;
import mc.ollie.command.*;
import mc.ollie.listeners.JoinLeaveListener;
import mc.ollie.listeners.LoginListener;

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

        getServer().getPluginManager().registerEvents(new LoginListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinLeaveListener(this), this);

        getLogger().info("                                        ");
        getLogger().info("  _____                 __             ");
        getLogger().info(" /  _  \\ ___  ______ __/  |_ _______ ");
        getLogger().info("/  /_\\  \\\\  \\/  /  _ \\   __\\\\_  __ \\");
        getLogger().info("/    |    \\>    <(  <_> )  |   |  | \\/");
        getLogger().info("\\____|__  /__/\\_ \\\\____/|__|   |__|   ");
        getLogger().info("        \\/      \\/                    ");
        getLogger().info("                                        ");
        getLogger().info("  Version: " + getDescription().getVersion());
        getLogger().info("  Author:  " + getDescription().getAuthors());
        getLogger().info("  Status:  Successfully started!");
        getLogger().info("                                        ");
    }

    @Override
    public void onDisable() {
        databaseManager.close();

        getLogger().info("                                        ");
        getLogger().info("  Axentra has been stopped. Goodbye!   ");
        getLogger().info("                                        ");
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