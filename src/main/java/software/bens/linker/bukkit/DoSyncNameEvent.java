package software.bens.linker.bukkit;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * @author Ben Kinney
 */
public final class DoSyncNameEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();

    public long discordId;
    public UUID uuid;

    public DoSyncNameEvent(long discordId, UUID uuid)
    {
        this.discordId = discordId;
        this.uuid = uuid;
    }

    public long getDiscordId()
    {
        return discordId;
    }

    public UUID getUniqueId()
    {
        return uuid;
    }

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }

    public static HandlerList getHandlerList()
    {
        return handlers;
    }
}
