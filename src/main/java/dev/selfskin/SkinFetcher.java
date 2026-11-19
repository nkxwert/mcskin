package dev.selfskin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** 닉네임으로 Mojang API에서 스킨 png를 받아 skins 폴더에 저장한다. (백그라운드 스레드에서 호출) */
public final class SkinFetcher {

    public static final class Result {
        public final File file;
        public final String model;

        Result(File file, String model) {
            this.file = file;
            this.model = model;
        }
    }

    private SkinFetcher() {}

    public static boolean isValidName(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{1,16}");
    }

    public static Result fetch(String username, File skinsDir) throws Exception {
        if (!isValidName(username)) throw new IllegalArgumentException("닉네임 형식이 올바르지 않습니다.");

        String profileJson = get("https://api.mojang.com/users/profiles/minecraft/" + username);
        if (profileJson == null || profileJson.trim().isEmpty()) throw new IllegalStateException("존재하지 않는 닉네임입니다.");
        JsonObject profile = new JsonParser().parse(profileJson).getAsJsonObject();
        String uuid = profile.get("id").getAsString();
        String exactName = profile.get("name").getAsString();

        String sessionJson = get("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
        if (sessionJson == null) throw new IllegalStateException("프로필을 불러오지 못했습니다.");
        JsonArray props = new JsonParser().parse(sessionJson).getAsJsonObject().getAsJsonArray("properties");

        String texturesB64 = null;
        for (JsonElement p : props) {
            JsonObject o = p.getAsJsonObject();
            if ("textures".equals(o.get("name").getAsString())) texturesB64 = o.get("value").getAsString();
        }
        if (texturesB64 == null) throw new IllegalStateException("스킨 정보가 없습니다.");

        String decoded = new String(Base64.getDecoder().decode(texturesB64), StandardCharsets.UTF_8);
        JsonObject textures = new JsonParser().parse(decoded).getAsJsonObject().getAsJsonObject("textures");
        if (textures == null || !textures.has("SKIN")) throw new IllegalStateException("기본 스킨을 쓰는 계정입니다.");
        JsonObject skin = textures.getAsJsonObject("SKIN");
        String url = skin.get("url").getAsString();
        if (url.startsWith("http://textures.minecraft.net/")) url = "https://" + url.substring("http://".length());
        String model = SkinConfig.MODEL_CLASSIC;
        if (skin.has("metadata") && skin.getAsJsonObject("metadata").has("model")
                && "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString())) {
            model = SkinConfig.MODEL_SLIM;
        }

        byte[] png = getBytes(url);
        File out = new File(skinsDir, exactName + ".png");
        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(png);
        }
        return new Result(out, model);
    }

    private static String get(String url) throws Exception {
        byte[] b = getBytes(url);
        return b == null ? null : new String(b, StandardCharsets.UTF_8);
    }

    private static byte[] getBytes(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        c.setRequestProperty("User-Agent", "SelfSkin/1.0");
        int code = c.getResponseCode();
        if (code == 204 || code == 404) return null;
        if (code != 200) throw new IllegalStateException("HTTP " + code + " (" + url + ")");
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
            return bos.toByteArray();
        } finally {
            c.disconnect();
        }
    }
}
