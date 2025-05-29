package ca.bkaw.torque.paper;

import ca.bkaw.torque.Torque;
import ca.bkaw.torque.paper.platform.PaperPlatform;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

public final class TorquePaper extends JavaPlugin {
    private Torque torque;

    @Override
    public void onEnable() {
        PaperPlatform platform = new PaperPlatform();
        this.torque = new Torque(platform);

        // Command registration
        PaperTorqueCommand command = new PaperTorqueCommand(platform);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands ->
            command.register(commands.registrar())
        );
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
