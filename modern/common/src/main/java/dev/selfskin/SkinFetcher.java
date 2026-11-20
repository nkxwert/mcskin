package dev.selfskin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

/** 닉네임으로 Mojang API에서 스킨 png를 받아 skins 폴더에 저장한다. (백그라운드 스레드에서 호출) */
public final class SkinFetcher {

    public record Result(Path file, boolean slim) {}

    /** 사용자에게 보여 줄 오류 */
    public static final class FetchException extends Exception {
        public final Component message;

        FetchException(Component message) {
            super(message.getString());
            this.message = message;
        }
    }

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private SkinFetcher() {}

    public static boolean isValidName(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{1,16}");
    }

    public static Result fetch(String username, Path skinsDir) throws Exception {
        if (!isValidName(username)) throw new FetchException(L.c("error.badUsername"));

        String profileJson = getString("https://api.mojang.com/users/profiles/minecraft/" + username);
        if (profileJson == null || profileJson.isBlank()) throw new FetchException(L.c("error.noPlayer"));
        JsonObject profile = JsonParser.parseString(profileJson).getAsJsonObject();
        String uuid = profile.get("id").getAsString();
        String exactName = profile.get("name").getAsString();

        String sessionJson = getString("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
        if (sessionJson == null) throw new FetchException(L.c("error.profileFailed"));
        JsonArray props = JsonParser.parseString(sessionJson).getAsJsonObject().getAsJsonArray("properties");

        String texturesB64 = null;
        for (JsonElement p : props) {
            JsonObject o = p.getAsJsonObject();
            if ("textures".equals(o.get("name").getAsString())) texturesB64 = o.get("value").getAsString();
        }
        if (texturesB64 == null) throw new FetchException(L.c("error.noTextures"));

        String decoded = new String(Base64.getDecoder().decode(texturesB64), StandardCharsets.UTF_8);
        JsonObject textures = JsonParser.parseString(decoded).getAsJsonObject().getAsJsonObject("textures");
        if (textures == null || !textures.has("SKIN")) throw new FetchException(L.c("error.defaultSkin"));
        JsonObject skin = textures.getAsJsonObject("SKIN");
        String url = skin.get("url").getAsString();
        if (url.startsWith("http://textures.minecraft.net/")) url = "https://" + url.substring("http://".length());
        boolean slim = skin.has("metadata") && skin.getAsJsonObject("metadata").has("model")
                && "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString());

        byte[] png = getBytes(url);
        if (png == null) throw new FetchException(L.c("error.noTextures"));
        Path out = skinsDir.resolve(exactName + ".png");
        Files.write(out, png);
        return new Result(out, slim);
    }

    private static String getString(String url) throws Exception {
        byte[] b = getBytes(url);
        return b == null ? null : new String(b, StandardCharsets.UTF_8);
    }

    private static byte[] getBytes(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "SelfSkin/2.0")
                .GET().build();
        HttpResponse<byte[]> res = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
        int code = res.statusCode();
        if (code == 204 || code == 404) return null;
        if (code != 200) throw new FetchException(Component.literal("HTTP " + code));
        return res.body();
    }
}
