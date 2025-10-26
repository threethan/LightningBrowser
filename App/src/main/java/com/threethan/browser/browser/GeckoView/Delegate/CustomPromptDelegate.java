

/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.threethan.browser.browser.GeckoView.Delegate;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.text.InputType;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.threethan.browser.R;
import com.threethan.browser.browser.BrowserActivity;
import com.threethan.browser.helper.CustomDialog;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.Autocomplete;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

/** @noinspection DataFlowIssue*/
public final class CustomPromptDelegate implements GeckoSession.PromptDelegate {
    private static final String TAG = "CustomPromptDelegate";

    public BrowserActivity mActivity;
    public int filePickerRequestCode = 10;

    public CustomPromptDelegate(BrowserActivity mActivity) {
        this.mActivity = mActivity;
    }

    @Override
    public GeckoResult<PromptResponse> onAlertPrompt(
            @NonNull final GeckoSession session, @NonNull final AlertPrompt prompt) {
        final Activity activity = mActivity;
        if (activity == null) {
            return GeckoResult.fromValue(prompt.dismiss());
        }
        final AlertDialog.Builder builder =
                new CustomDialog.Builder(activity)
                        .setTitle(prompt.title)
                        .setMessage(prompt.message)
                        .setPositiveButton(android.R.string.ok, /* onClickListener */ null);
        GeckoResult<PromptResponse> res = new GeckoResult<>();
        createStandardDialog(builder, prompt, res).show();
        return res;
    }

    @Override
    public GeckoResult<PromptResponse> onButtonPrompt(
            @NonNull final GeckoSession session, @NonNull final ButtonPrompt prompt) {
        final Activity activity = mActivity;
        if (activity == null) {
            return GeckoResult.fromValue(prompt.dismiss());
        }
        final AlertDialog.Builder builder =
                new AlertDialog.Builder(activity).setTitle(prompt.title).setMessage(prompt.message);

        GeckoResult<PromptResponse> res = new GeckoResult<>();

        final DialogInterface.OnClickListener listener =
                (dialog, which) -> {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        res.complete(prompt.confirm(ButtonPrompt.Type.POSITIVE));
                    } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                        res.complete(prompt.confirm(ButtonPrompt.Type.NEGATIVE));
                    } else {
                        res.complete(prompt.dismiss());
                    }
                };

        builder.setPositiveButton(android.R.string.ok, listener);
        builder.setNegativeButton(android.R.string.cancel, listener);

        createStandardDialog(builder, prompt, res).show();
        return res;
    }

    @NonNull
    @Override
    public GeckoResult<PromptResponse> onRepostConfirmPrompt(
            @NonNull final GeckoSession session, @NonNull final RepostConfirmPrompt prompt) {
        final Activity activity = mActivity;
        if (activity == null) {
            return GeckoResult.fromValue(prompt.dismiss());
        }
        final AlertDialog.Builder builder =
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.repost_confirm_title)
                        .setMessage(R.string.repost_confirm_message);

        GeckoResult<PromptResponse> res = new GeckoResult<>();

        final DialogInterface.OnClickListener listener =
                (dialog, which) -> {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        res.complete(prompt.confirm(AllowOrDeny.ALLOW));
                    } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                        res.complete(prompt.confirm(AllowOrDeny.DENY));
                    } else {
                        res.complete(prompt.dismiss());
                    }
                };

        builder.setPositiveButton(R.string.repost_confirm_resend, listener);
        builder.setNegativeButton(R.string.repost_confirm_cancel, listener);

        createStandardDialog(builder, prompt, res).show();
        return res;
    }

    @Nullable
    @Override
    public GeckoResult<PromptResponse> onCreditCardSave(
            @NonNull GeckoSession session,
            @NonNull AutocompleteRequest<Autocomplete.CreditCardSaveOption> request) {
        Log.i(TAG, "onCreditCardSave " + request.options[0].value);
        return null;
    }

    @NonNull
    @Override
    public GeckoResult<PromptResponse> onLoginSave(
            @NonNull GeckoSession session,
            @NonNull AutocompleteRequest<Autocomplete.LoginSaveOption> request) {
        Log.i(TAG, "onLoginSave");
        return GeckoResult.fromValue(request.confirm(request.options[0]));
    }

    @Nullable
    @Override
    public GeckoResult<PromptResponse> onAddressSave(@NonNull GeckoSession session, @NonNull AutocompleteRequest<Autocomplete.AddressSaveOption> request) {
        return GeckoSession.PromptDelegate.super.onAddressSave(session, request);
    }

    @NonNull
    @Override
    public GeckoResult<PromptResponse> onBeforeUnloadPrompt(
            @NonNull final GeckoSession session, @NonNull final BeforeUnloadPrompt prompt) {
        final Activity activity = mActivity;
        if (activity == null) {
            return GeckoResult.fromValue(prompt.dismiss());
        }
        final AlertDialog.Builder builder =
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.before_unload_title)
                        .setMessage(R.string.before_unload_message);

        GeckoResult<PromptResponse> res = new GeckoResult<>();

        final DialogInterface.OnClickListener listener =
                (dialog, which) -> {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        res.complete(prompt.confirm(AllowOrDeny.ALLOW));
                    } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                        res.complete(prompt.confirm(AllowOrDeny.DENY));
                    } else {
                        res.complete(prompt.dismiss());
                    }
                };

        builder.setPositiveButton(R.string.before_unload_leave_page, listener);
        builder.setNegativeButton(R.string.before_unload_stay, listener);

        createStandardDialog(builder, prompt, res).show();
        return res;
    }

    private int getViewPadding(final AlertDialog.Builder builder) {
        //noinspection resource
        final TypedArray attr =
                builder
                        .getContext()
                        .obtainStyledAttributes(new int[] {android.R.attr.listPreferredItemPaddingLeft});
        final int padding = attr.getDimensionPixelSize(0, 1);
        attr.recycle();
        return padding;
    }

    private LinearLayout addStandardLayout(
            final AlertDialog.Builder builder, final String title, final String msg) {
        final ScrollView scrollView = new ScrollView(builder.getContext());
        final LinearLayout container = new LinearLayout(builder.getContext());
        final int horizontalPadding = getViewPadding(builder);
        final int verticalPadding = (msg == null || msg.isEmpty()) ? horizontalPadding : 0;
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(
                /* left */ horizontalPadding, /* top */ verticalPadding,
                /* right */ horizontalPadding, /* bottom */ verticalPadding);
        scrollView.addView(container);
        builder.setTitle(title).setMessage(msg).setView(scrollView);
        return container;
    }

    private AlertDialog createStandardDialog(
            final AlertDialog.Builder builder,
            final BasePrompt prompt,
            final GeckoResult<PromptResponse> response) {
        final AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(
                dialog1 -> {
                    if (!prompt.isComplete()) {
                        response.complete(prompt.dismiss());
                    }
                });
        return dialog;
    }

    @Override
    public GeckoResult<PromptResponse> onTextPrompt(
            @NonNull final GeckoSession session, @NonNull final TextPrompt prompt) {
        final Activity activity = mActivity;
        if (activity == null) {
            return GeckoResult.fromValue(prompt.dismiss());
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final LinearLayout container = addStandardLayout(builder, prompt.title, prompt.message);
        final EditText editText = new EditText(builder.getContext());
        editText.setText(prompt.defaultValue);
        container.addView(editText);

        GeckoResult<PromptResponse> res = new GeckoResult<>();

        builder
                .setNegativeButton(android.R.string.cancel, /* listener */ null)
                .setPositiveButton(
                        android.R.string.ok,
                        (dialog, which) -> res.complete(prompt.confirm(editText.getText().toString())));

        createStandardDialog(builder, prompt, res).show();
        return res;
    }

    @Override
    public GeckoResult<PromptResponse> onAuthPrompt(
            @NonNull final GeckoSession session, @NonNull final AuthPrompt prompt) {
        final Activity activity = mActivity;
        if (activity == null) {
            return GeckoResult.fromValue(prompt.dismiss());
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final LinearLayout container = addStandardLayout(builder, prompt.title, prompt.message);

        final int flags = prompt.authOptions.flags;
        final int level = prompt.authOptions.level;
        final EditText username;
        if ((flags & AuthPrompt.AuthOptions.Flags.ONLY_PASSWORD) == 0) {
            username = new EditText(builder.getContext());
            username.setHint(R.string.username);
            username.setText(prompt.authOptions.username);
            container.addView(username);
        } else {
            username = null;
        }

        final EditText password = new EditText(builder.getContext());
        password.setHint(R.string.password);
        password.setText(prompt.authOptions.password);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(password);

        if (level != AuthPrompt.AuthOptions.Level.NONE) {
            final ImageView secure = new ImageView(builder.getContext());
            secure.setImageResource(android.R.drawable.ic_lock_lock);
            container.addView(secure);
        }

        GeckoResult<PromptResponse> res = new GeckoResult<>();

        builder
                .setNegativeButton(android.R.string.cancel, /* listener */ null)
                .setPositiveButton(
                        android.R.string.ok,
                        (dialog, which) -> {
                            if ((flags & AuthPrompt.AuthOptions.Flags.ONLY_PASSWORD) == 0) {
                                assert username != null;
                                res.complete(
                                        prompt.confirm(username.getText().toString(), password.getText().toString()));

                            } else {
                                res.complete(prompt.confirm(password.getText().toString()));
                            }
                        });
        createStandardDialog(builder, prompt, res).show();

        return res;
    }

    private static class ModifiableChoice {
        public boolean modifiableSelected;
        public String modifiableLabel;
        public final ChoicePrompt.Choice choice;

        public ModifiableChoice(ChoicePrompt.Choice c) {
            choice = c;
            modifiableSelected = choice.selected;
            modifiableLabel = choice.label;
        }
    }

    private void addChoiceItems(
            final int type,
            final ArrayAdapter<ModifiableChoice> list,
            final ChoicePrompt.Choice[] items,
            final String indent) {
        if (type == ChoicePrompt.Type.MENU) {
            for (final ChoicePrompt.Choice item : items) {
                list.add(new ModifiableChoice(item));
            }
            return;
        }

        for (final ChoicePrompt.Choice item : items) {
            final ModifiableChoice modItem = new ModifiableChoice(item);

            final ChoicePrompt.Choice[] children = item.items;

            if (indent != null && children == null) {
                modItem.modifiableLabel = indent + modItem.modifiableLabel;
            }
            list.add(modItem);

            if (children != null) {
                final String newIndent;
                if (type == ChoicePrompt.Type.SINGLE || type == ChoicePrompt.Type.MULTIPLE) {
                    newIndent = (indent != null) ? indent + '\t' : "\t";
                } else {
                    newIndent = null;
                }
                addChoiceItems(type, list, children, newIndent);
            }
        }
    }

    private void onChoicePromptImpl(
            final String title,
            final String message,
            final int type,
            final ChoicePrompt.Choice[] choices,
            final ChoicePrompt prompt,
            final GeckoResult<PromptResponse> res) {
        final Activity activity = mActivity;
        if (activity == null) {
            res.complete(prompt.dismiss());
            return;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        addStandardLayout(builder, title, message);

        final ListView list = new ListView(builder.getContext());
        if (type == ChoicePrompt.Type.MULTIPLE) {
            list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        }

        final ArrayAdapter<ModifiableChoice> adapter =
                new ArrayAdapter<>(
                        builder.getContext(), android.R.layout.simple_list_item_1) {
                    private static final int TYPE_MENU_ITEM = 0;
                    private static final int TYPE_MENU_CHECK = 1;
                    private static final int TYPE_SEPARATOR = 2;
                    private static final int TYPE_GROUP = 3;
                    private static final int TYPE_SINGLE = 4;
                    private static final int TYPE_MULTIPLE = 5;
                    private static final int TYPE_COUNT = 6;

                    private LayoutInflater mInflater;
                    private View mSeparator;

                    @Override
                    public int getViewTypeCount() {
                        return TYPE_COUNT;
                    }

                    @Override
                    public int getItemViewType(final int position) {
                        final ModifiableChoice item = getItem(position);
                        assert item != null;
                        if (item.choice.separator) {
                            return TYPE_SEPARATOR;
                        } else if (type == ChoicePrompt.Type.MENU) {
                            return item.modifiableSelected ? TYPE_MENU_CHECK : TYPE_MENU_ITEM;
                        } else if (item.choice.items != null) {
                            return TYPE_GROUP;
                        } else if (type == ChoicePrompt.Type.SINGLE) {
                            return TYPE_SINGLE;
                        } else if (type == ChoicePrompt.Type.MULTIPLE) {
                            return TYPE_MULTIPLE;
                        } else {
                            throw new UnsupportedOperationException();
                        }
                    }

                    @Override
                    public boolean isEnabled(final int position) {
                        final ModifiableChoice item = getItem(position);
                        assert item != null;
                        return !item.choice.separator
                                && !item.choice.disabled
                                && ((type != ChoicePrompt.Type.SINGLE && type != ChoicePrompt.Type.MULTIPLE)
                                || item.choice.items == null);
                    }

                    @NonNull
                    @Override
                    public View getView(final int position, View view, @NonNull final ViewGroup parent) {
                        final int itemType = getItemViewType(position);
                        final int layoutId;
                        if (itemType == TYPE_SEPARATOR) {
                            if (mSeparator == null) {
                                mSeparator = new View(getContext());
                                mSeparator.setLayoutParams(
                                        new ListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2, itemType));
                                //noinspection resource
                                final TypedArray attr =
                                        getContext().obtainStyledAttributes(new int[]{android.R.attr.listDivider});
                                mSeparator.setBackgroundResource(attr.getResourceId(0, 0));
                                attr.recycle();
                            }
                            return mSeparator;
                        } else if (itemType == TYPE_MENU_ITEM) {
                            layoutId = android.R.layout.simple_list_item_1;
                        } else if (itemType == TYPE_MENU_CHECK) {
                            layoutId = android.R.layout.simple_list_item_checked;
                        } else if (itemType == TYPE_GROUP) {
                            layoutId = android.R.layout.preference_category;
                        } else if (itemType == TYPE_SINGLE) {
                            layoutId = android.R.layout.simple_list_item_single_choice;
                        } else if (itemType == TYPE_MULTIPLE) {
                            layoutId = android.R.layout.simple_list_item_multiple_choice;
                        } else {
                            throw new UnsupportedOperationException();
                        }

                        if (view == null) {
                            if (mInflater == null) {
                                mInflater = LayoutInflater.from(builder.getContext());
                            }
                            view = mInflater.inflate(layoutId, parent, false);
                        }

                        final ModifiableChoice item = getItem(position);
                        final TextView text = (TextView) view;
                        assert item != null;
                        text.setEnabled(!item.choice.disabled);
                        text.setText(item.modifiableLabel);
                        if (view instanceof CheckedTextView) {
                            final boolean selected = item.modifiableSelected;
                            if (itemType == TYPE_MULTIPLE) {
                                list.setItemChecked(position, selected);
                            } else {
                                ((CheckedTextView) view).setChecked(selected);
                            }
                        }
                        return view;
                    }
                };
        addChoiceItems(type, adapter, choices, /* indent */ null);

        list.setAdapter(adapter);
        builder.setView(list);

        final AlertDialog dialog;
        if (type == ChoicePrompt.Type.SINGLE || type == ChoicePrompt.Type.MENU) {
            dialog = createStandardDialog(builder, prompt, res);
            list.setOnItemClickListener(
                    (parent, v, position, id) -> {
                        final ModifiableChoice item = adapter.getItem(position);
                        if (type == ChoicePrompt.Type.MENU) {
                            assert item != null;
                            final ChoicePrompt.Choice[] children = item.choice.items;
                            if (children != null) {
                                // Show sub-menu.
                                dialog.setOnDismissListener(null);
                                dialog.dismiss();
                                onChoicePromptImpl(
                                        item.modifiableLabel, /* msg */ null, type, children, prompt, res);
                                return;
                            }
                        }
                        assert item != null;
                        res.complete(prompt.confirm(Objects.requireNonNull(item).choice));
                        dialog.dismiss();
                    });
        } else if (type == ChoicePrompt.Type.MULTIPLE) {
            list.setOnItemClickListener(
                    (parent, v, position, id) -> {
                        final ModifiableChoice item = adapter.getItem(position);
                        assert item != null;
                        item.modifiableSelected = ((CheckedTextView) v).isChecked();
                    });
            builder
                    .setNegativeButton(android.R.string.cancel, /* listener */ null)
                    .setPositiveButton(
                            android.R.string.ok,
                            (dialog1, which) -> {
                                final int len = adapter.getCount();
                                ArrayList<String> items = new ArrayList<>(len);
                                for (int i = 0; i < len; i++) {
                                    final ModifiableChoice item = adapter.getItem(i);
                                    assert item != null;
                                    if (item.modifiableSelected) {
                                        items.add(item.choice.id);
                                    }
                                }
                                res.complete(prompt.confirm(items.toArray(new String[0])));
                            });
            dialog = createStandardDialog(builder, prompt, res);
        } else {
            throw new UnsupportedOperationException();
        }
        dialog.show();

        prompt.setDelegate(
                new PromptInstanceDelegate() {
                    @Override
                    public void onPromptDismiss(@NonNull final BasePrompt prompt) {
                        dialog.dismiss();
                    }

                    @Override
                    public void onPromptUpdate(@NonNull final BasePrompt prompt) {
                        dialog.setOnDismissListener(null);
                        dialog.dismiss();
                        final ChoicePrompt newPrompt = (ChoicePrompt) prompt;
                        onChoicePromptImpl(
                                newPrompt.title,
                                newPrompt.message,
                                newPrompt.type,
                                newPrompt.choices,
                                newPrompt,
                                res);
                    }
                });
    }

    @Override
    public GeckoResult<PromptResponse> onChoicePrompt(
            @NonNull final GeckoSession session, @NonNull final ChoicePrompt prompt) {
        final GeckoResult<PromptResponse> res = new GeckoResult<>();
        onChoicePromptImpl(
                prompt.title, prompt.message, prompt.type, prompt.choices, prompt, res);
        return res;
    }

    private static int parseColor(final String value) {
        try {
            return Color.parseColor(value);
        } catch (final IllegalArgumentException e) {
            return 0;
        }
    }

    @Override
    public GeckoResult<PromptResponse> onColorPrompt(
            @NonNull final GeckoSession session, @NonNull final ColorPrompt prompt) {
        final Activity activity = mActivity;
        if (activity == null) {
            return GeckoResult.fromValue(prompt.dismiss());
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        addStandardLayout(builder, prompt.title, /* msg */ null);

        final int initial = parseColor(prompt.defaultValue /* def */);
        final ArrayAdapter<Integer> adapter =
                new ArrayAdapter<>(builder.getContext(), android.R.layout.simple_list_item_1) {
                    private LayoutInflater mInflater;

                    @Override
                    public int getViewTypeCount() {
                        return 2;
                    }

                    @Override
                    public int getItemViewType(final int position) {
                        return (getItem(position) == initial) ? 1 : 0;
                    }

                    @NonNull
                    @Override
                    public View getView(final int position, View view, @NonNull final ViewGroup parent) {
                        if (mInflater == null) {
                            mInflater = LayoutInflater.from(builder.getContext());
                        }
                        final int color = getItem(position);
                        if (view == null) {
                            view =
                                    mInflater.inflate(
                                            (color == initial)
                                                    ? android.R.layout.simple_list_item_checked
                                                    : android.R.layout.simple_list_item_1,
                                            parent,
                                            false);
                        }
                        view.setBackgroundResource(android.R.drawable.editbox_background);
                        view.getBackground().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
                        return view;
                    }
                };

        adapter.addAll(
                0xffff4444 /* holo_red_light */,
                0xffcc0000 /* holo_red_dark */,
                0xffffbb33 /* holo_orange_light */,
                0xffff8800 /* holo_orange_dark */,
                0xff99cc00 /* holo_green_light */,
                0xff669900 /* holo_green_dark */,
                0xff33b5e5 /* holo_blue_light */,
                0xff0099cc /* holo_blue_dark */,
                0xffaa66cc /* holo_purple */,
                0xffffffff /* white */,
                0xffaaaaaa /* lighter_gray */,
                0xff555555 /* darker_gray */,
                0xff000000 /* black */);

        final ListView list = new ListView(builder.getContext());
        list.setAdapter(adapter);
        builder.setView(list);

        GeckoResult<PromptResponse> res = new GeckoResult<>();

        final AlertDialog dialog = createStandardDialog(builder, prompt, res);
        list.setOnItemClickListener(
                (parent, v, position, id) -> {
                    res.complete(
                            prompt.confirm(String.format("#%06x", 0xffffff & adapter.getItem(position))));
                    dialog.dismiss();
                });
        dialog.show();

        return res;
    }

    private static Date parseDate(
            final SimpleDateFormat formatter, final String value, final boolean defaultToNow) {
        try {
            if (value != null && !value.isEmpty()) {
                return formatter.parse(value);
            }
        } catch (final ParseException ignored) {
        }
        return defaultToNow ? new Date() : null;
    }

    private static void setTimePickerTime(final TimePicker picker, final Calendar cal) {
        picker.setHour(cal.get(Calendar.HOUR_OF_DAY));
        picker.setMinute(cal.get(Calendar.MINUTE));
    }

    private static void setCalendarTime(final Calendar cal, final TimePicker picker) {
        cal.set(Calendar.HOUR_OF_DAY, picker.getHour());
        cal.set(Calendar.MINUTE, picker.getMinute());
    }

    @Override
    public GeckoResult<PromptResponse> onDateTimePrompt(
            @NonNull final GeckoSession session, @NonNull final DateTimePrompt prompt) {
        final Activity activity = mActivity;
        if (activity == null) {
            return GeckoResult.fromValue(prompt.dismiss());
        }
        final String format;
        if (prompt.type == DateTimePrompt.Type.DATE) {
            format = "yyyy-MM-dd";
        } else if (prompt.type == DateTimePrompt.Type.MONTH) {
            format = "yyyy-MM";
        } else if (prompt.type == DateTimePrompt.Type.WEEK) {
            format = "yyyy-'W'ww";
        } else if (prompt.type == DateTimePrompt.Type.TIME) {
            format = "HH:mm";
        } else if (prompt.type == DateTimePrompt.Type.DATETIME_LOCAL) {
            format = "yyyy-MM-dd'T'HH:mm";
        } else {
            throw new UnsupportedOperationException();
        }

        final SimpleDateFormat formatter = new SimpleDateFormat(format, Locale.ROOT);
        final Date minDate = parseDate(formatter, prompt.minValue, /* defaultToNow */ false);
        final Date maxDate = parseDate(formatter, prompt.maxValue, /* defaultToNow */ false);
        final Date date = parseDate(formatter, prompt.defaultValue, /* defaultToNow */ true);
        final Calendar cal = formatter.getCalendar();
        if(date != null) cal.setTime(date);

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        final DatePicker datePicker;
        if (prompt.type == DateTimePrompt.Type.DATE
                || prompt.type == DateTimePrompt.Type.MONTH
                || prompt.type == DateTimePrompt.Type.WEEK
                || prompt.type == DateTimePrompt.Type.DATETIME_LOCAL) {
            @SuppressLint("DiscouragedApi") final int resId =
                    builder
                            .getContext()
                            .getResources()
                            .getIdentifier("date_picker_dialog", "layout", "android");
            DatePicker picker = null;
            if (resId != 0) {
                try {
                    picker = (DatePicker) inflater.inflate(resId, /* root */ null);
                } catch (final ClassCastException | InflateException ignored) {
                }
            }
            if (picker == null) {
                picker = new DatePicker(builder.getContext());
            }
            picker.init(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH), /* listener */
                    null);
            if (minDate != null) {
                picker.setMinDate(minDate.getTime());
            }
            if (maxDate != null) {
                picker.setMaxDate(maxDate.getTime());
            }
            datePicker = picker;
        } else {
            datePicker = null;
        }

        final TimePicker timePicker;
        if (prompt.type == DateTimePrompt.Type.TIME
                || prompt.type == DateTimePrompt.Type.DATETIME_LOCAL) {
            @SuppressLint("DiscouragedApi") final int resId =
                    builder
                            .getContext()
                            .getResources()
                            .getIdentifier("time_picker_dialog", "layout", "android");
            TimePicker picker = null;
            if (resId != 0) {
                try {
                    picker = (TimePicker) inflater.inflate(resId, /* root */ null);
                } catch (final ClassCastException | InflateException ignored) {
                }
            }
            if (picker == null) {
                picker = new TimePicker(builder.getContext());
            }
            setTimePickerTime(picker, cal);
            picker.setIs24HourView(DateFormat.is24HourFormat(builder.getContext()));
            timePicker = picker;
        } else {
            timePicker = null;
        }

        final LinearLayout container = addStandardLayout(builder, prompt.title, /* msg */ null);
        container.setPadding(/* left */ 0, /* top */ 0, /* right */ 0, /* bottom */ 0);
        if (datePicker != null) {
            container.addView(datePicker);
        }
        if (timePicker != null) {
            container.addView(timePicker);
        }

        GeckoResult<PromptResponse> res = new GeckoResult<>();

        final DialogInterface.OnClickListener listener =
                (dialog, which) -> {
                    if (which == DialogInterface.BUTTON_NEUTRAL) {
                        // Clear
                        res.complete(prompt.confirm(""));
                        return;
                    }
                    if (datePicker != null) {
                        cal.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                    }
                    if (timePicker != null) {
                        setCalendarTime(cal, timePicker);
                    }
                    res.complete(prompt.confirm(formatter.format(cal.getTime())));
                };
        builder
                .setNegativeButton(android.R.string.cancel, /* listener */ null)
                .setNeutralButton(R.string.clear_field, listener)
                .setPositiveButton(android.R.string.ok, listener);

        final AlertDialog dialog = createStandardDialog(builder, prompt, res);
        dialog.show();

        prompt.setDelegate(
                new PromptInstanceDelegate() {
                    @Override
                    public void onPromptDismiss(@NonNull final BasePrompt prompt) {
                        dialog.dismiss();
                    }
                });
        return res;
    }

    @Override
    public GeckoResult<PromptResponse> onFilePrompt(@NonNull GeckoSession session, @NonNull FilePrompt prompt) {
        if (mActivity == null) {
            return GeckoResult.fromValue(prompt.dismiss());
        }

        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        if (prompt.type == FilePrompt.Type.MULTIPLE) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        if ((prompt.mimeTypes != null ? prompt.mimeTypes.length : 0) > 0) {
            List<String> mimeTypes = new ArrayList<>();
            for (String mimeType : prompt.mimeTypes) {
                if (MimeTypeMap.getSingleton().hasMimeType(mimeType)) {
                    mimeTypes.add(mimeType);
                } else {
                    String extension = mimeType.strip().replace(".", "");
                    mimeTypes.add(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
                }
            }
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toArray(new String[0]));
        }
        Log.i(TAG, "Launching file picker with mime types: "
                + Arrays.toString(intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)));
        GeckoResult<PromptResponse> res = new GeckoResult<>();
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        try {
            filePickerResult = res;
            filePickerPromptRef = new WeakReference<>(prompt);
            filePickerRequestCode = new Random().nextInt(0xFFFF);
            mActivity.startActivityForResult(intent, filePickerRequestCode);
        } catch (final ActivityNotFoundException e) {
            Log.e(TAG, "Cannot launch activity", e);
            return GeckoResult.fromValue(prompt.dismiss());
        }

        return res;
    }
    private GeckoResult<PromptResponse> filePickerResult;
    private WeakReference<FilePrompt> filePickerPromptRef;
    @SuppressLint("WrongConstant")
    public void handleFilePickerResult(int resultCode, Intent data) {
        /*
            Intents such as content://media/picker_get_content/0/com.android.providers.media.photopicker/media/1000016741 work,
            but intents such as content://com.android.providers.downloads.documents/document/3953 do not,
            why?
         */

        Log.i(TAG, "Handling file picker result");
        if (filePickerResult == null || filePickerPromptRef == null
                || filePickerPromptRef.get() == null) {
            return;
        }
        Log.i(TAG, "File picker result found, processing...");
        FilePrompt prompt = filePickerPromptRef.get();
        if (resultCode != Activity.RESULT_OK || data == null) {
            filePickerResult.cancel();
            filePickerResult = null;
            filePickerPromptRef = null;
        } else {
            Log.i(TAG, "File picker returned data: " + data.getDataString());
            List<Uri> uris = new ArrayList<>();
            if (data.getData() != null) {
                uris.add(data.getData());
            } else if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    ClipData.Item item = clipData.getItemAt(i);
                    if (item.getUri() != null) {
                        uris.add(item.getUri());
                    }
                }
            }

            Log.i(TAG, "File picker selected " + uris.size() + " files." +
                    (!uris.isEmpty() ? " First: " + uris.get(0).toString() : ""));

            mActivity.runOnUiThread(() -> {
                PromptResponse fResponse = prompt.confirm(mActivity, uris.toArray(new Uri[0]));
                filePickerResult.complete(fResponse);
            });
        }
        filePickerResult = null;
    }
    @Override
    public GeckoResult<PromptResponse> onPopupPrompt(
            @NonNull final GeckoSession session, final PopupPrompt prompt) {
        return GeckoResult.fromValue(prompt.confirm(AllowOrDeny.ALLOW));
    }
}