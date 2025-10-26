package com.threethan.browser.adapter;

import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.threethan.browser.R;
import com.threethan.browser.browser.GeckoView.CustomMediaSessionDelegate;
import com.threethan.browser.helper.TabManager;
import com.threethan.browser.wrapper.WrapperActivity;

public class ActiveTabsAdapter extends TabsAdapter {
    public ActiveTabsAdapter(WrapperActivity wrapperActivity) {
        super(wrapperActivity);
    }

    @Override
    public String getOpenUrl(String tabIdOrUrl, TabManager tabManager) {
        return tabIdOrUrl;
    }

    @Override
    public void onBindViewHolder(@NonNull TabHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        String playingTabId = CustomMediaSessionDelegate.getPlayingTabId();
        if (playingTabId != null) {
            String tabIdOrUrl = items.get(position);
            View playButton = holder.buttonsViewGroup.findViewById(R.id.play);
            View pauseButton = holder.buttonsViewGroup.findViewById(R.id.pause);
            boolean isPlayingTab = tabIdOrUrl.equals(playingTabId);
            boolean isPlaying = CustomMediaSessionDelegate.getPlayingInstance() != null &&
                    CustomMediaSessionDelegate.getPlayingInstance().isPlaying();
            playButton.setVisibility(isPlayingTab && !isPlaying ? View.VISIBLE : View.GONE);
            pauseButton.setVisibility(isPlayingTab && isPlaying ? View.VISIBLE : View.GONE);
            playButton.setOnClickListener(v -> {
                Log.d("ActiveTabsAdapter", "Play button clicked for tab: " + tabIdOrUrl);
                if (CustomMediaSessionDelegate.getPlayingInstance() != null) {
                    CustomMediaSessionDelegate.getPlayingInstance().play();
                }
            });
            pauseButton.setOnClickListener(v -> {
                Log.d("ActiveTabsAdapter", "Pause button clicked for tab: " + tabIdOrUrl);
                if (CustomMediaSessionDelegate.getPlayingInstance() != null) {
                    CustomMediaSessionDelegate.getPlayingInstance().pause();
                }
            });
        }
    }
}