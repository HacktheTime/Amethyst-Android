package net.kdt.pojavlaunch.api;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import net.kdt.pojavlaunch.BuildConfig;
import com.google.gson.JsonObject;
import net.kdt.pojavlaunch.value.MinecraftAccount;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.util.ArrayList;
import java.util.List;

public class LaunchIntentDataProviderService extends IntentService {
    public static final String ACTION_GET_INSTANCE_IDS = "net.kdt.pojavlaunch.action.GET_PROFILE_IDS";
    public static final String EXTRA_RESULT_RECEIVER = "result_receiver";

    public LaunchIntentDataProviderService() {
        super("LaunchIntentDataProviderService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null && ACTION_GET_INSTANCE_IDS.equals(intent.getAction())) {
            List<MinecraftProfile> rawProfiles = new ArrayList<>(LauncherProfiles.getProfiles().values());
            List<JsonObject> profiles = new ArrayList<>();
            for (MinecraftProfile rawProfile : rawProfiles) {
                JsonObject profile = new JsonObject();
                profile.addProperty("name", rawProfile.name);
                profile.addProperty("render_name", rawProfile.pojavRendererName);
                profile.addProperty("last_version", rawProfile.lastVersionId);
                profile.addProperty("last_used", rawProfile.lastUsed);
                profiles.add(profile);
            }
            List<MinecraftAccount> rawAccounts = MinecraftAccount.getAllAccounts();
            List<JsonObject> accounts = new ArrayList<>();
            for (MinecraftAccount rawAccount : rawAccounts) {
                JsonObject account = new JsonObject();
                account.addProperty("name", rawAccount.username);
                account.addProperty("uuid", rawAccount.profileId);
                accounts.add(account);
            }

            ResultReceiver receiver = intent.getParcelableExtra(EXTRA_RESULT_RECEIVER);
            if (receiver != null) {
                Bundle resultData = new Bundle();
                resultData.putString("profiles", profiles.toString());
                resultData.putString("accounts", accounts.toString());
                receiver.send(0, resultData);
            } else {
                Log.e("GameInstanceGetter", "No ResultReceiver provided!");
            }
        }
    }
}