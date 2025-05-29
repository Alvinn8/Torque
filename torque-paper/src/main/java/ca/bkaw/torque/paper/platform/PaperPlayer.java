package ca.bkaw.torque.paper.platform;

import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.UUID;

public record PaperPlayer(org.bukkit.entity.Player entity) implements ca.bkaw.torque.platform.Player {
    @Override
    public void sendResourcePack(UUID id, String url, byte[] hash, boolean required, @Nullable String prompt) {
        System.out.println(3);
        this.entity.addResourcePack(id, url, hash, prompt, required);
    }

    @Override
    public @Nullable InetSocketAddress getAddress() {
        return this.entity.getAddress();
    }
}
