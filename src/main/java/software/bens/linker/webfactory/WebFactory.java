package software.bens.linker.webfactory;

import com.sun.net.httpserver.HttpServer;
import org.bukkit.configuration.ConfigurationSection;

/**
 * @author Ben Kinney
 */
public interface WebFactory
{
    HttpServer createServer(ConfigurationSection section) throws Throwable;
}
