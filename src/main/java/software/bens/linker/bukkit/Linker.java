package software.bens.linker.bukkit;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import software.bens.linker.Token;
import software.bens.linker.webfactory.HttpWebFactory;
import software.bens.linker.webfactory.HttpsWebFactory;
import org.json.JSONObject;
import software.bens.linker.oauth.OAuth2;
import software.bens.linker.oauth.OAuth2Builder;
import software.bens.linker.oauth.Scope;
import software.bens.linker.oauth.UserJoinSettings;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

/**
 * @author Ben Kinney
 */
public final class Linker extends JavaPlugin implements HttpHandler, Listener
{
    private static final Random RANDOM = new SecureRandom();
    private static final char[] CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private HttpServer         httpServer;
    private OAuth2             auth;
    private BiMap<UUID, Long>  discordIdMap = HashBiMap.create();
    private Map<String, Token> tokenMap     = new HashMap<>();

    private String inviteLink;

    private Token getToken(String key)
    {
        Token token = tokenMap.get(key);
        if(token == null)
            return null;
        if(!token.isValid())
        {
            tokenMap.remove(key, token);
            return null;
        }
        return token;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(command.getName().equals("discord"))
        {
            if(sender instanceof Player)
            {
                UUID uuid = ((Player) sender).getUniqueId();
                if(discordIdMap.containsKey(uuid))
                {
                    StringBuilder messageBuilder = new StringBuilder(ChatColor.RED + "You have already joined the Discord server.\nWant to invite a friend? ");
                    if(inviteLink == null)
                        messageBuilder.append("Have them join and type /").append(label);
                    else
                        messageBuilder.append("Send them the Discord invite link, ").append(inviteLink);
                    sender.sendMessage(messageBuilder.toString());
                    return true;
                }

                String token = newToken();
                tokenMap.put(token, new Token(token, uuid));
                TextComponent message = (TextComponent) TextComponent.fromLegacyText(ChatColor.GREEN + "\nClick this message to join the Discord!\n")[0];
                message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, auth.getRedirectUri() + "?state=" + token));
                ((Player) sender).spigot().sendMessage(message);
            }
            else
                sender.sendMessage(ChatColor.RED + "You must be a player to do this.");
        }
        else if(sender instanceof Player && sender.hasPermission("linker.redirecturi"))
        {
            TextComponent message = (TextComponent) TextComponent.fromLegacyText(ChatColor.GREEN + "\nClick this message to be able to copy the redirect URI.\n")[0];
            message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, auth.getRedirectUri()));
            ((Player) sender).spigot().sendMessage(message);
        }
        else
            sender.sendMessage(ChatColor.RED + "You do not have permission for this.");
        return true;
    }

    @Override
    public void onEnable()
    {
        saveResource("web/failure.html", false);
        saveResource("web/instruct.html", false);
        saveResource("web/success.html", false);

        saveDefaultConfig();
        final FileConfiguration config = getConfig();
        inviteLink = config.getString("invite-link");
        auth = new OAuth2Builder().setClientId(config.getString("client.id"))
                                  .setClientSecret(config.getString("client.secret"))
                                  .setRedirectUri("http" + (config.getBoolean("web.https.enabled") ? 's' : "") + "://"
                                                  + config.getString("web.external-ip")
                                                  + (config.getBoolean("web.using-srv-record") ? "" : ":" + config.getString("web.port"))
                                                  + config.getString("web.context"))
                                  .setBotToken(config.getString("client.token"))
                                  .setScopes(Scope.IDENTIFY, Scope.GUILDS_JOIN)
                                  .build();


        try
        {
            httpServer = (config.getBoolean("web.https.enabled") ? new HttpsWebFactory() : new HttpWebFactory()).createServer(config.getConfigurationSection("web"));
            httpServer.createContext(config.getString("web.context"), this);
            httpServer.start();
        }
        catch(Throwable throwable)
        {
            throwable.printStackTrace();
        }
    }

    @Override
    public void onDisable()
    {
        if(httpServer != null)
            httpServer.stop(0);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        Optional<Long> optDiscordId = Optional.ofNullable(discordIdMap.get(player.getUniqueId()));
        optDiscordId.ifPresent(discordId -> getServer().getPluginManager().callEvent(new DoSyncNameEvent(discordId, player.getUniqueId())));
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException
    {
        try
        {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> parameters = new HashMap<>();
            if(query != null)
                while(query.length() != 0)
                {
                    int nextAnd = query.indexOf('&');
                    if(nextAnd == -1)
                        nextAnd = query.length();
                    String[] parameter = query.substring(0, nextAnd).split("=");
                    parameters.put(URLDecoder.decode(parameter[0], "UTF-8"), URLDecoder.decode(parameter[1], "UTF-8"));
                    if(nextAnd == query.length())
                        break;
                    query = query.substring(nextAnd + 1);
                }

            String code = parameters.get("code");
            String state = parameters.get("state");

            if(state != null)
            {
                Token token = getToken(state);
                if(token == null)
                    throw new RuntimeException();

                if(code == null)
                {
                    exchange.getResponseHeaders().add("Location", auth.createAuthURL(state));
                    exchange.sendResponseHeaders(301, 0);
                }
                else
                {
                    UUID uuid = token.getCreator();
                    String accessToken = auth.getAccessToken(code);
                    JSONObject user = auth.getUser(accessToken);
                    UserJoinSettings settings = new UserJoinSettings(user.getString("id"));
                    if(getConfig().getBoolean("sync-names"))
                        settings.setNickname(getServer().getOfflinePlayer(uuid).getName());
                    auth.join(accessToken, getConfig().getString("server-id"), settings);

                    BufferedReader reader = new BufferedReader(new FileReader(new File(getDataFolder() + "/web", "success.html")));
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while((line = reader.readLine()) != null)
                        builder.append(line.replace("{username}", getServer().getOfflinePlayer(uuid).getName())).append("\n");
                    exchange.getResponseHeaders().add("Content-Encoding", "utf-8");
                    exchange.getResponseHeaders().add("Content-Type", "text/html");
                    exchange.sendResponseHeaders(200, builder.length());
                    OutputStream out = exchange.getResponseBody();
                    out.write(builder.toString().getBytes(StandardCharsets.UTF_8));
                    out.flush();

                    discordIdMap.put(uuid, user.getLong("id"));
                    getServer().getPluginManager().callEvent(new DiscordLinkEvent(user.getLong("id"), uuid));
                }
            }
            else
            {
                BufferedReader reader = new BufferedReader(new FileReader(new File(getDataFolder() + "/web", "instruct.html")));
                StringBuilder builder = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null)
                    builder.append(line).append("\n");
                exchange.getResponseHeaders().add("Content-Encoding", "utf-8");
                exchange.getResponseHeaders().add("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, builder.length());
                OutputStream out = exchange.getResponseBody();
                out.write(builder.toString().getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        }
        catch(Throwable throwable)
        {
            BufferedReader reader = new BufferedReader(new FileReader(new File(getDataFolder() + "/web", "failure.html")));
            StringBuilder builder = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null)
                builder.append(line).append("\n");
            exchange.getResponseHeaders().add("Content-Encoding", "utf-8");
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(500, builder.length());
            OutputStream out = exchange.getResponseBody();
            out.write(builder.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
        exchange.close();
    }

    private static String newToken()
    {
        int length = 10;
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < length; i++)
            builder.append(CHARS[RANDOM.nextInt(CHARS.length)]);
        return builder.toString();
    }
}
