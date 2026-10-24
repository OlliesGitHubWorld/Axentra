package mc.ollie;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DutyManager {

    private final App plugin;
    private final Set<UUID> onDuty = new HashSet<>();

    public DutyManager(App plugin) {
        this.plugin = plugin;
    }

    public boolean isOnDuty(UUID uuid) {
        return onDuty.contains(uuid);
    }

    public void setOnDuty(UUID uuid, boolean duty) {
        if (duty) {
            onDuty.add(uuid);
            plugin.getDatabaseManager().startDutySession(uuid.toString());
        } else {
            onDuty.remove(uuid);
            plugin.getDatabaseManager().endDutySession(uuid.toString());
        }
    }

    public void removeAll() {
        for (UUID uuid : new HashSet<>(onDuty)) {
            setOnDuty(uuid, false);
        }
    }

    public Set<UUID> getOnDutyStaff() {
        return new HashSet<>(onDuty);
    }
}
