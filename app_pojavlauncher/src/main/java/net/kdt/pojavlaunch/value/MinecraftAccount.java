package net.kdt.pojavlaunch.value;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.Keep;
import com.google.gson.JsonSyntaxException;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.authenticator.microsoft.MicrosoftBackgroundLogin;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("IOStreamConstructor")
@Keep
public class MinecraftAccount {
    public String accessToken = "0"; // access token
    public String clientToken = "0"; // clientID: refresh and invalidate
    public String profileId = "00000000-0000-0000-0000-000000000000"; // profile UUID, for obtaining skin
    public String username = "Steve";
    public String selectedVersion = "1.7.10";
    public boolean isMicrosoft = false;
    public String msaRefreshToken = "0";
    public String xuid;
    public long expiresAt;
    public String skinFaceBase64;
    private Bitmap mFaceCache;

    void updateSkinFace(String uuid) {
        try {
            File skinFile = getSkinFaceFile(username);
            Tools.downloadFile("https://mc-heads.net/head/" + uuid + "/100", skinFile.getAbsolutePath());

            Log.i("SkinLoader", "Update skin face success");
        } catch (IOException e) {
            // Skin refresh limit, no internet connection, etc...
            // Simply ignore updating skin face
            Log.w("SkinLoader", "Could not update skin face", e);
        }
    }

    public boolean isLocal() {
        return accessToken.equals("0") && !username.startsWith("Demo.");
    }

    public boolean isDemo(){
        return username.startsWith("Demo.");
    }

    public void updateSkinFace() {
        updateSkinFace(profileId);
    }

    public String save(String outPath) throws IOException {
        Tools.write(outPath, Tools.GLOBAL_GSON.toJson(this));
        return username;
    }

    public String save() throws IOException {
        return save(Tools.DIR_ACCOUNT_NEW + "/" + username + ".json");
    }

    public static MinecraftAccount parse(String content) throws JsonSyntaxException {
        return Tools.GLOBAL_GSON.fromJson(content, MinecraftAccount.class);
    }

    public static List<MinecraftAccount> getAllAccounts() {
        List<File> files = Tools.listFiles(Tools.DIR_ACCOUNT_NEW);
        List<MinecraftAccount> accounts = new ArrayList<>();
        for (File file : files) {
            if (!file.getName().matches("[a-zA-Z0-9_]+.json")) continue;
            try {
                MinecraftAccount account = parse(Tools.read(file.getAbsolutePath()));
                if (account != null) {
                    accounts.add(account);
                }
            } catch (IOException | JsonSyntaxException e) {
                Log.e(MinecraftAccount.class.getName(), "Caught an exception while loading the profile", e);
            }
        }
        return accounts;
    }

    @Nullable
    public static MinecraftAccount load(String name) {
        if (!accountExists(name)) return null;
        try {
            MinecraftAccount acc = parse(Tools.read(Tools.DIR_ACCOUNT_NEW + "/" + name + ".json"));
            if (acc.accessToken == null) {
                acc.accessToken = "0";
            }
            if (acc.clientToken == null) {
                acc.clientToken = "0";
            }
            if (acc.profileId == null) {
                acc.profileId = "00000000-0000-0000-0000-000000000000";
            }
            if (acc.username == null) {
                acc.username = "0";
            }
            if (acc.selectedVersion == null) {
                acc.selectedVersion = "1.7.10";
            }
            if (acc.msaRefreshToken == null) {
                acc.msaRefreshToken = "0";
            }
            return acc;
        } catch (NullPointerException | IOException | JsonSyntaxException e) {
            Log.e(MinecraftAccount.class.getName(), "Caught an exception while loading the profile", e);
            return null;
        }
    }

    public Bitmap getSkinFace() {
        if (isLocal()) return null;

        File skinFaceFile = getSkinFaceFile(username);
        if (!skinFaceFile.exists()) {
            // Legacy version, storing the head inside the json as base 64
            if (skinFaceBase64 == null) return null;
            byte[] faceIconBytes = Base64.decode(skinFaceBase64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(faceIconBytes, 0, faceIconBytes.length);
        } else {
            if (mFaceCache == null) {
                mFaceCache = BitmapFactory.decodeFile(skinFaceFile.getAbsolutePath());
            }
        }

        return mFaceCache;
    }

    public static Bitmap getSkinFace(String username) {
        return BitmapFactory.decodeFile(getSkinFaceFile(username).getAbsolutePath());
    }

    private static File getSkinFaceFile(String username) {
        return new File(Tools.DIR_CACHE, username + ".png");
    }

    private static boolean accountExists(String username) {
        return new File(Tools.DIR_ACCOUNT_NEW + "/" + username + ".json").exists();
    }

    /**
     * @param maxAge the maximum age of the token
     * @param timeUnit the time unit of the maxAge
     * @throws ExecutionException if the login fails
     * @throws InterruptedException if the login is interrupted
     * @throws IllegalStateException if the account is not a Microsoft account
     * @throws IOException if a Error Occurs while saving the account
     */
    public void refresh(int maxAge, TimeUnit timeUnit) throws ExecutionException, InterruptedException, IOException {
        if (isMicrosoft) {
            if (System.currentTimeMillis() > (expiresAt - timeUnit.toMillis(maxAge))) {
                new MicrosoftBackgroundLogin(true, msaRefreshToken)
                        .performLogin(null, null, null).get();
                save();
            }
            return;
        }
        throw new IllegalStateException("Not a Microsoft account");
    }
}
