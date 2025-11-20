package net.kdt.pojavlaunch.value;

import net.kdt.pojavlaunch.*;
import java.io.*;
import com.google.gson.*;

public class MinecraftAccount {
    public String accessToken = "0"; // access token
    public String clientToken = "0"; // clientID: refresh and invalidate
    public String profileId = "00000000-0000-0000-0000-000000000000"; // authenticate UUID
    public String username = "Knight";

    public String save(String outPath) throws IOException {
        Tools.write(outPath, Tools.GLOBAL_GSON.toJson(this));
        return outPath;
    }

    public String save() throws IOException {
        return save(Tools.DIR_ACCOUNT_NEW + "/" + username + ".json");
    }

    public static MinecraftAccount parse(String content) throws JsonSyntaxException {
        MinecraftAccount account = Tools.GLOBAL_GSON.fromJson(content, MinecraftAccount.class);
        return account;
    }

    public static MinecraftAccount load(String name) throws IOException, JsonSyntaxException {
        try {
            MinecraftAccount acc = parse(Tools.read(Tools.DIR_ACCOUNT_NEW + "/" + name + ".json"));
            if (acc.accessToken == null) {
                acc.accessToken = "0";
            }
            if (acc.clientToken == null) {
                acc.clientToken = "0";
            }
            if (acc.profileId == null) {
                acc.profileId = "0";
            }
            if (acc.username == null) {
                acc.username = "Knight";
            }
            return acc;
        } catch (Exception e) {
            MinecraftAccount acc = new MinecraftAccount();
            acc.username = "Knight";
            return acc;
        }
    }
}
