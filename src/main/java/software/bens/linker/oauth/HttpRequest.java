package software.bens.linker.oauth;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ben Kinney
 */
public final class HttpRequest
{
    private String url;
    private Map<String, Object> headerMap = new HashMap<>();
    private String contentType;
    private byte[] content = new byte[0];
    private boolean hasContent = true;

    public HttpRequest(String url)
    {
        setUrl(url);
    }

    public HttpURLConnection request(String method) throws IOException
    {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);

        headerMap.forEach((key, value) -> connection.setRequestProperty(key, value.toString()));

        if(contentType != null)
            connection.setRequestProperty("Content-Type", contentType);
        if(content.length != 0)
            connection.setRequestProperty("Content-Length", Integer.toString(content.length));

        connection.setUseCaches(false);
        connection.setDoInput(true);

        if(contentType != null)
        {
            connection.setDoOutput(true);
            OutputStream out = connection.getOutputStream();
            out.write(content);
            out.flush();
        }

        connection.connect();
        return connection;
    }

    public HttpRequest setUrl(String url)
    {
        if(url != null)
            this.url = url;
        return this;
    }

    public HttpRequest withContent(String type, byte[] content)
    {
        this.contentType = type;
        this.content = content;
        return this;
    }

    public HttpRequest withJSONContent(JSONObject json)
    {
        this.contentType = "application/json";
        this.content = json.toString().getBytes(StandardCharsets.UTF_8);
        return this;
    }

    public HttpRequest withFormContent(String key, String value) throws UnsupportedEncodingException
    {
        if(content == null)
            content = new byte[0];

        if(!"application/x-www-form-urlencoded".equals(contentType))
        {
            contentType = "application/x-www-form-urlencoded";
            content = new byte[0];
        }

        StringBuilder contentBuilder = new StringBuilder(new String(content, StandardCharsets.UTF_8));
        if(contentBuilder.length() != 0)
            contentBuilder.append("&");
        contentBuilder.append(URLEncoder.encode(key, "UTF-8")).append('=').append(URLEncoder.encode(value, "UTF-8"));
        content = contentBuilder.toString().getBytes(StandardCharsets.UTF_8);

        return this;
    }

    public HttpRequest withHeader(String key, Object value)
    {
        headerMap.put(key, value);
        return this;
    }
}
