package com.threethan.browser.browser.GeckoView.Delegate;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.threethan.browser.R;
import com.threethan.browser.browser.BrowserActivity;
import com.threethan.browser.browser.BrowserService;
import com.threethan.browser.helper.Dialog;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.WebExtensionController;

public class ExtensionPromptDelegate implements WebExtensionController.PromptDelegate {
    final static String EXTENSIONS_URL = "https://addons.mozilla.org/firefox/extensions/";

    @Nullable
    @Override
    public GeckoResult<AllowOrDeny> onOptionalPrompt(@NonNull WebExtension extension, @NonNull String[] permissions, @NonNull String[] origins, @NonNull String[] dataCollectionPermissions) {
        return GeckoResult.fromValue(AllowOrDeny.ALLOW);
    }

    @Nullable
    @Override
    public GeckoResult<AllowOrDeny> onUpdatePrompt(@NonNull WebExtension extension, @NonNull String[] newPermissions, @NonNull String[] newOrigins, @NonNull String[] newDataCollectionPermissions) {
        return GeckoResult.fromValue(AllowOrDeny.ALLOW);
    }

    @Nullable
    @Override
    public GeckoResult<WebExtension.PermissionPromptResponse> onInstallPromptRequest(@NonNull WebExtension extension, @NonNull String[] permissions, @NonNull String[] origins, @NonNull String[] dataCollectionPermissions) {
        return GeckoResult.fromValue(new WebExtension.PermissionPromptResponse(true, true, false));
    }

    public void showList() {

        final Activity activity = Dialog.getActivityContext();
        if (activity == null) return;

        final AlertDialog dialog = Dialog.build(activity, R.layout.dialog_webextensions);
        assert dialog != null;

        dialog.findViewById(R.id.getMoreButton).setOnClickListener(view -> {
            if (activity instanceof BrowserActivity) {
                ((BrowserActivity) activity).loadUrl(EXTENSIONS_URL);
                dialog.dismiss();
            }
        });

        View dismiss = dialog.findViewById(R.id.dismissButton);
        dismiss.setOnClickListener(view -> dialog.dismiss());

        final ArrayAdapter<WebExtension> adapter =
            new ArrayAdapter<>(activity, R.layout.lv_webext) {
                private LayoutInflater mInflater;
                @NonNull
                @Override
                public View getView(final int position, View view, @NonNull final ViewGroup parent) {
                    if (mInflater == null) {
                        mInflater = LayoutInflater.from(activity);
                    }
                    WebExtension item = getItem(position);
                    assert item != null;
                    if (view == null) {
                        view =
                                mInflater.inflate(R.layout.lv_webext,
                                        parent,
                                        false);
                    }
                    final WebExtensionController extensionController = BrowserService.sRuntime.getWebExtensionController();
                    View finalView = view;

                    final boolean[] enabled = {item.metaData.disabledFlags == 0};

                    View main = view.findViewById(R.id.main);
                    View options = view.findViewById(R.id.options);

                    View disabledOverlay = view.findViewById(R.id.disabledOverlay);
                    disabledOverlay.setVisibility(enabled[0] ? View.GONE : View.VISIBLE);
                    options.setVisibility(item.metaData.optionsPageUrl == null || ! enabled[0] ? View.GONE : View.VISIBLE);
                    main.setOnClickListener(v -> {
                        if (enabled[0])
                            extensionController.disable(item, WebExtensionController.EnableSource.USER);
                        else
                            extensionController.enable(item, WebExtensionController.EnableSource.USER);
                        enabled[0] = !enabled[0];
                        disabledOverlay.setVisibility(enabled[0] ? View.GONE : View.VISIBLE);

                        options.setVisibility(enabled[0] ? View.VISIBLE : View.GONE);
                        options.postDelayed(() ->
                            options.setVisibility(item.metaData.optionsPageUrl == null || ! enabled[0] ? View.GONE : View.VISIBLE)
                        , 250);

                        Dialog.toast(item.metaData.name,
                                getContext().getString(enabled[0] ? R.string.enabled : R.string.disabled),
                                false);
                    });
                    main.setOnLongClickListener(v -> {
                        if (item.metaData.amoListingUrl != null) {
                            ((BrowserActivity) activity).loadUrl(item.metaData.amoListingUrl);
                            dialog.dismiss();
                            return true;
                        }
                        return false;
                    });

                    view.setOnFocusChangeListener((view1, hasFocus) -> {
                        if (hasFocus) {
                            main.requestFocus();
                        }
                    });

                    view.findViewById(R.id.uninstall).setOnClickListener(v -> {
                        extensionController.uninstall(item);
                        finalView.setVisibility(View.GONE);
                        dialog.dismiss();
                        showList();
                    });
                    options.setOnClickListener(v -> {
                        if (activity instanceof BrowserActivity
                                && item.metaData.optionsPageUrl != null) {
                            ((BrowserActivity) activity).loadUrl(item.metaData.optionsPageUrl);
                            dialog.dismiss();
                        }
                    });
                    TextView name = view.findViewById(R.id.name);
                    name.setText(item.metaData.name);
                    TextView info = view.findViewById(R.id.info);
                    info.setText(item.metaData.description);
                    return view;
                }
            };

        BrowserService.sRuntime.getWebExtensionController().list()
                .then(value -> {
                    assert value != null;
                    Log.v("WEBEXTLIST", value.toString());
                    value.removeIf(webExtension -> webExtension.id.equals("fixes@internal.ext"));
                    adapter.addAll(value);

                    dialog.show();

                    ListView lv = dialog.findViewById(R.id.listView);
                    lv.post(() -> lv.setAdapter(adapter));
                    return null;
                });
    }
}

