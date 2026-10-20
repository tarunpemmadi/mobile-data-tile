package com.mobiledata.tile;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    private TextView usageView;
    private TextView statusView;
    private Button grantButton;
    private RadioGroup modeGroup;
    private NumberPicker dayPicker;
    private TextView dayLabel;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(UsageRepository.PREFS_NAME, MODE_PRIVATE);

        usageView = findViewById(R.id.usage_display);
        statusView = findViewById(R.id.status_text);
        grantButton = findViewById(R.id.grant_button);
        modeGroup = findViewById(R.id.mode_group);
        dayPicker = findViewById(R.id.day_picker);
        dayLabel = findViewById(R.id.day_label);

        dayPicker.setMinValue(1);
        dayPicker.setMaxValue(31);
        dayPicker.setWrapSelectorWheel(true);
        dayPicker.setValue(prefs.getInt(UsageRepository.KEY_CYCLE_START_DAY, 1));

        boolean cycle = UsageRepository.MODE_CYCLE.equals(
                prefs.getString(UsageRepository.KEY_MODE, UsageRepository.MODE_DAILY));
        modeGroup.check(cycle ? R.id.mode_cycle : R.id.mode_daily);
        updateDayPickerEnabled(cycle);

        modeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isCycle = checkedId == R.id.mode_cycle;
            saveMode(isCycle ? UsageRepository.MODE_CYCLE : UsageRepository.MODE_DAILY);
            updateDayPickerEnabled(isCycle);
        });

        dayPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            prefs.edit().putInt(UsageRepository.KEY_CYCLE_START_DAY, newVal).apply();
            afterSettingsChanged();
        });

        grantButton.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            } catch (Exception e) {
                // Some OEMs remove the aggregate list; fall back to app settings.
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatusAndUsage();
    }

    private void saveMode(String mode) {
        prefs.edit().putString(UsageRepository.KEY_MODE, mode).apply();
        afterSettingsChanged();
    }

    private void updateDayPickerEnabled(boolean enabled) {
        dayPicker.setEnabled(enabled);
        dayLabel.setEnabled(enabled);
        dayLabel.setAlpha(enabled ? 1f : 0.4f);
    }

    private void afterSettingsChanged() {
        TileRefreshScheduler.scheduleDailyRefresh(this);
        DataTileService.requestTileRefresh(this);
        refreshStatusAndUsage();
    }

    private void refreshStatusAndUsage() {
        boolean granted = UsageRepository.hasUsageAccess(this);
        statusView.setText(granted
                ? R.string.usage_access_granted
                : R.string.usage_access_missing);
        grantButton.setVisibility(granted ? android.view.View.GONE
                : android.view.View.VISIBLE);

        if (!granted) {
            usageView.setText(R.string.usage_placeholder);
            return;
        }

        usageView.setText(R.string.usage_placeholder);
        new Thread(() -> {
            long bytes = UsageRepository.queryMobileBytes(this);
            String text = bytes < 0
                    ? getString(R.string.usage_placeholder)
                    : UsageRepository.formatBytes(bytes);
            runOnUiThread(() -> usageView.setText(text));
        }).start();
    }
}
