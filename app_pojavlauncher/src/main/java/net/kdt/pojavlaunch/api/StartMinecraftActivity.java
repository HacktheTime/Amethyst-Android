package net.kdt.pojavlaunch.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import net.kdt.pojavlaunch.BuildConfig;
import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.MainActivity;
import net.kdt.pojavlaunch.PojavProfile;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.lifecycle.ContextAwareDoneListener;
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader;
import net.kdt.pojavlaunch.tasks.MinecraftDownloader;
import net.kdt.pojavlaunch.value.MinecraftAccount;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class StartMinecraftActivity extends Activity {
    public static final String INTENT_PROFILE_ID = "profile_id";
    public static final String INTENT_LAUNCH_USER = "launch_user";
    public static final String ACTION_START_MINECRAFT = "net.kdt.pojavlaunch.action.START_PROFILE";
    private static final String ACTION_GET_PROFILE_IDS = "net.kdt.pojavlaunch.action.GET_PROFILE_IDS";

    private ProgressBar progressBar;
    private TextView usernameTextView;
    private TextView profileNameTextView;
    private ImageView profileIconImageView;
    private TextView appIdTextView;
    private TextView statusTextView;
    private TextView versionTextView;
    private TextView packageTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null && ACTION_START_MINECRAFT.equals(intent.getAction())) {
            setContentView(R.layout.activity_start_minecraft);

            progressBar = findViewById(R.id.progressBar);
            usernameTextView = findViewById(R.id.usernameTextView);
            profileNameTextView = findViewById(R.id.profileNameTextView);
            profileIconImageView = findViewById(R.id.profileIconImageView);
            appIdTextView = findViewById(R.id.appIdTextView);
            statusTextView = findViewById(R.id.statusTextView);
            versionTextView = findViewById(R.id.versionTextView);
            packageTextView = findViewById(R.id.packageTextView);

            String instanceId = intent.getStringExtra(INTENT_PROFILE_ID);
            MinecraftProfile profile;
            if (instanceId == null || instanceId.isEmpty() || instanceId.equalsIgnoreCase("current")) {
                profile = LauncherProfiles.getCurrentProfile();
            } else {
                profile = LauncherProfiles.getProfiles().get(instanceId);
            }
            if (profile != null) {
                profileNameTextView.setText(profile.name);
                versionTextView.setText(getString(R.string.start_screen_version_label) + ": " + profile.lastVersionId);
            } else {
                profileNameTextView.setText(R.string.start_screen_profile_label);
                versionTextView.setText(getString(R.string.start_screen_version_label));
                statusTextView.setText(R.string.error_no_version);
                return;
            }

            String accountDetails = intent.getStringExtra(INTENT_LAUNCH_USER);
            if (accountDetails != null && (accountDetails.isEmpty() || accountDetails.equalsIgnoreCase("current"))) {
                accountDetails = PojavProfile.getCurrentProfileName(this);
            }
            if (TextUtils.isEmpty(accountDetails)) {
                accountDetails = null;
            }

            MinecraftAccount selectedAccount = null;
            if (accountDetails != null) {
                for (MinecraftAccount account : MinecraftAccount.getAllAccounts()) {
                    if (account.username.equalsIgnoreCase(accountDetails) || account.profileId.replace("-", "").equalsIgnoreCase(accountDetails)) {
                        selectedAccount = account;
                        break;
                    }
                }
            } else {
                // Fallback to current profile if any
                String currentName = PojavProfile.getCurrentProfileName(this);
                if (!TextUtils.isEmpty(currentName)) {
                    selectedAccount = MinecraftAccount.load(currentName);
                }
            }

            if (selectedAccount != null) {
                usernameTextView.setText(selectedAccount.username);
                Bitmap face = selectedAccount.getSkinFace();
                if (face != null) {
                    profileIconImageView.setImageBitmap(face);
                } else {
                    profileIconImageView.setImageResource(R.mipmap.ic_launcher);
                }
                try {
                    // Refresh token proactively before launch
                    selectedAccount.refresh(6, TimeUnit.HOURS);
                    accountDetails = selectedAccount.username;
                } catch (Exception e) {
                    Tools.showError(this, e);
                    statusTextView.setText(R.string.start_screen_status_token_refresh_failed);
                }
            } else if (accountDetails != null) {
                usernameTextView.setText(accountDetails);
                profileIconImageView.setImageResource(R.mipmap.ic_launcher);
            } else {
                usernameTextView.setText(R.string.start_screen_account_label);
                profileIconImageView.setImageResource(R.mipmap.ic_launcher);
            }

            appIdTextView.setText(getPackageName());
            packageTextView.setText(ACTION_START_MINECRAFT);
            statusTextView.setText(R.string.start_screen_status_preparing);

            // Start Minecraft download and launch process
            String normalizedVersionId = AsyncMinecraftDownloader.normalizeVersionId(profile.lastVersionId);
            JMinecraftVersionList.Version mcVersion = AsyncMinecraftDownloader.getListedVersion(normalizedVersionId);
            new MinecraftDownloader().start(
                    this,
                    mcVersion,
                    normalizedVersionId,
                    new ContextAwareDoneIntentLaunchListener(this, normalizedVersionId, accountDetails)
            );
        }
        else {
            throw new IllegalArgumentException("No Intent provided");
        }
    }

    private static class ContextAwareDoneIntentLaunchListener extends ContextAwareDoneListener {
        private final String launchUserName;

        public ContextAwareDoneIntentLaunchListener(Context context, String versionId, String launchUserName) {
            super(context, versionId);
            this.launchUserName = launchUserName;
        }

        @Override
        protected Intent createGameStartIntent(Context context) {
            Intent launchIntent = super.createGameStartIntent(context);
            launchIntent.putExtra(MainActivity.INTENT_LAUCH_USER, launchUserName);
            return launchIntent;
        }

        @Override
        public void executeWithApplication(Context context) {
            try {
                Intent gameStartIntent = createGameStartIntent(context);
                gameStartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                gameStartIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                gameStartIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
                context.startActivity(gameStartIntent);
                android.os.Process.killProcess(android.os.Process.myPid());
            } catch (Throwable e) {
                Tools.showError(context, e);
            }
        }
    }
}