package software.bens.linker.webfactory;

import com.sun.net.httpserver.HttpServer;
import org.bukkit.configuration.ConfigurationSection;

import java.net.InetSocketAddress;

/**
 * @author Ben Kinney
 */
public final class HttpWebFactory implements WebFactory
{
    @Override
    public HttpServer createServer(ConfigurationSection section) throws Throwable
    {
        HttpServer server = HttpServer.create(new InetSocketAddress(section.getInt("port")), 0);
        return server;
    }
}
