package software.bens.linker.webfactory;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.bukkit.configuration.ConfigurationSection;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

/**
 * @author Ben Kinney
 */
public final class HttpsWebFactory implements WebFactory
{
    @Override
    public HttpServer createServer(ConfigurationSection section) throws Throwable
    {
        final HttpsServer server = HttpsServer.create(new InetSocketAddress(section.getInt("port")), 0);
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        final KeyStore keystore = KeyStore.getInstance("JKS");
        final char[] password = section.getString("https.password").toCharArray();
        keystore.load( new FileInputStream(section.getString("https.key-store")), password);

        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keystore, password);
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keystore);
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        server.setHttpsConfigurator(new HttpsConfigurator(sslContext)
        {
            public void configure(HttpsParameters params)
            {
                SSLContext context = getSSLContext();
                SSLEngine engine = context.createSSLEngine();
                params.setNeedClientAuth(false);
                params.setCipherSuites(engine.getEnabledCipherSuites());
                params.setProtocols(engine.getEnabledProtocols());
                SSLParameters sslParameters = context.getSupportedSSLParameters();
                params.setSSLParameters(sslParameters);
            }
        });

        return server;
    }
}
