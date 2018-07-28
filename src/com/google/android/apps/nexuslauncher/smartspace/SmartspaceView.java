package com.google.android.apps.nexuslauncher.smartspace;

import android.animation.ValueAnimator;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Process;
import android.provider.CalendarContract;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import ch.deletescape.lawnchair.LawnchairAppKt;
import ch.deletescape.lawnchair.LawnchairPreferences;
import ch.deletescape.lawnchair.LawnchairUtilsKt;
import ch.deletescape.lawnchair.smartspace.LawnchairSmartspaceController;
import com.android.launcher3.*;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.graphics.ShadowGenerator;
import com.android.launcher3.util.Themes;
import com.google.android.apps.nexuslauncher.DynamicIconProvider;
import com.google.android.apps.nexuslauncher.graphics.IcuDateTextView;
import org.jetbrains.annotations.NotNull;

public class SmartspaceView extends FrameLayout implements ISmartspace, ValueAnimator.AnimatorUpdateListener, View.OnClickListener, View.OnLongClickListener, Runnable, LawnchairSmartspaceController.Listener {
    private TextView mSubtitleWeatherText;
    private final TextPaint dB;
    private View mTitleSeparator;
    private TextView mTitleText;
    private ViewGroup mTitleWeatherContent;
    private ImageView mTitleWeatherIcon;
    private TextView mTitleWeatherText;
    private final ColorStateList dH;
    private final int mSmartspaceBackgroundRes;
    private IcuDateTextView mClockView;
    private ViewGroup mSmartspaceContent;
    private final SmartspaceController dp;
    private SmartspaceDataContainer dq;
    private BubbleTextView dr;
    private boolean ds;
    private boolean mDoubleLine;
    private final OnClickListener mCalendarClickListener;
    private final OnClickListener mWeatherClickListener;
    private ImageView mSubtitleIcon;
    private TextView mSubtitleText;
    private ViewGroup mSubtitleWeatherContent;
    private ImageView mSubtitleWeatherIcon;
    private boolean mEnableShadow;
    private final Handler mHandler;

    private LawnchairSmartspaceController mController;
    private boolean mFinishedInflate;
    private boolean mWeatherAvailable;
    private LawnchairPreferences mPrefs;

    public SmartspaceView(final Context context, AttributeSet set) {
        super(context, set);

        mController = LawnchairAppKt.getLawnchairApp(context).getSmartspace();
        mPrefs = Utilities.getLawnchairPrefs(context);

        mCalendarClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                final Uri content_URI = CalendarContract.CONTENT_URI;
                final Uri.Builder appendPath = content_URI.buildUpon().appendPath("time");
                ContentUris.appendId(appendPath, System.currentTimeMillis());
                final Intent addFlags = new Intent(Intent.ACTION_VIEW)
                        .setData(appendPath.build())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                try {
                    final Context context = getContext();
                    Launcher.getLauncher(context).startActivitySafely(v, addFlags, null);
                } catch (ActivityNotFoundException ex) {
                    LauncherAppsCompat.getInstance(getContext()).showAppDetailsForProfile(new ComponentName(DynamicIconProvider.GOOGLE_CALENDAR, ""), Process.myUserHandle());
                }
            }
        };

        mWeatherClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mController != null)
                    mController.openWeather(v);
            }
        };

        dp = SmartspaceController.get(context);
        mHandler = new Handler();
        dH = ColorStateList.valueOf(Themes.getAttrColor(getContext(), R.attr.workspaceTextColor));
        ds = dp.cY();
        mSmartspaceBackgroundRes = R.drawable.bg_smartspace;
        dB = new TextPaint();
        dB.setTextSize((float) getResources().getDimensionPixelSize(R.dimen.smartspace_title_size));
        mEnableShadow = !Themes.getAttrBoolean(context, R.attr.isWorkspaceDarkText);

        setClipChildren(false);
    }

    @Override
    public void onDataUpdated(@NotNull LawnchairSmartspaceController.DataContainer data) {
        if (mDoubleLine != data.isDoubleLine()) {
            mDoubleLine = data.isDoubleLine();
            cs();
        }
        setOnClickListener(this);
        setOnLongClickListener(co());
        mWeatherAvailable = data.isWeatherAvailable();
        if (mDoubleLine) {
            loadDoubleLine(data);
        } else {
            loadSingleLine(data);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void loadDoubleLine(final LawnchairSmartspaceController.DataContainer data) {
        setBackgroundResource(mSmartspaceBackgroundRes);
        mTitleText.setText(data.getCard().getTitle());
        mTitleText.setEllipsize(data.getCard().getTitleEllipsize());
        mSubtitleText.setText(data.getCard().getSubtitle());
        mSubtitleText.setEllipsize(data.getCard().getSubtitleEllipsize());
        mSubtitleIcon.setImageTintList(dH);
        mSubtitleIcon.setImageBitmap(data.getCard().getIcon());
        bindWeather(data, mSubtitleWeatherContent, mSubtitleWeatherText, mSubtitleWeatherIcon);
    }

    @SuppressWarnings("ConstantConditions")
    private void loadSingleLine(final LawnchairSmartspaceController.DataContainer data) {
        setBackgroundResource(0);
        bindWeather(data, mTitleWeatherContent, mTitleWeatherText, mTitleWeatherIcon);
        bindClockAndSeparator(false);
    }

    private void bindClockAndSeparator(boolean forced) {
        if (mPrefs.getSmartspaceDate() || mPrefs.getSmartspaceTime()) {
            mClockView.setVisibility(View.VISIBLE);
            mClockView.setOnClickListener(mCalendarClickListener);
            mClockView.setOnLongClickListener(co());
            if (forced)
                mClockView.reloadDateFormat(true);
            LawnchairUtilsKt.setVisible(mTitleSeparator, mWeatherAvailable);
            if (!Utilities.ATLEAST_NOUGAT) {
                mClockView.onVisibilityAggregated(true);
            }
        } else {
            mClockView.setVisibility(View.GONE);
            mTitleSeparator.setVisibility(View.GONE);
        }
    }

    private void bindWeather(LawnchairSmartspaceController.DataContainer data, View container, TextView title, ImageView icon) {
        mWeatherAvailable = data.isWeatherAvailable();
        if (mWeatherAvailable) {
            container.setVisibility(View.VISIBLE);
            container.setOnClickListener(mWeatherClickListener);
            container.setOnLongClickListener(co());
            title.setText(data.getWeather().getTitle(
                    Utilities.getLawnchairPrefs(getContext()).getUseMetricWeatherUnit()));
            icon.setImageBitmap(addShadowToBitmap(data.getWeather().getIcon()));
        } else {
            container.setVisibility(View.GONE);
        }
    }

    public void reloadCustomizations() {
        if (!mDoubleLine) {
            bindClockAndSeparator(true);
        }
    }

    private Bitmap addShadowToBitmap(Bitmap bitmap) {
        return mEnableShadow ? ShadowGenerator.getInstance(getContext()).recreateIcon(bitmap, false) : bitmap;
    }

    private void loadViews() {
        mTitleText = findViewById(R.id.title_text);
        mSubtitleText = findViewById(R.id.subtitle_text);
        mSubtitleIcon = findViewById(R.id.subtitle_icon);
        mTitleWeatherIcon = findViewById(R.id.title_weather_icon);
        mSubtitleWeatherIcon = findViewById(R.id.subtitle_weather_icon);
        mSmartspaceContent = findViewById(R.id.smartspace_content);
        mTitleWeatherContent = findViewById(R.id.title_weather_content);
        mSubtitleWeatherContent = findViewById(R.id.subtitle_weather_content);
        mTitleWeatherText = findViewById(R.id.title_weather_text);
        mSubtitleWeatherText = findViewById(R.id.subtitle_weather_text);
        backportClockVisibility(false);
        mClockView = findViewById(R.id.clock);
        backportClockVisibility(true);
        mTitleSeparator = findViewById(R.id.title_sep);
    }

    private String cn() {
        final boolean b = true;
        final SmartspaceCard dp = dq.dP;
        return dp.cC(TextUtils.ellipsize(dp.cB(b), dB, getWidth() - getPaddingLeft() - getPaddingRight() - getResources().getDimensionPixelSize(R.dimen.smartspace_horizontal_padding) - dB.measureText(dp.cA(b)), TextUtils.TruncateAt.END).toString());
    }

    private OnLongClickListener co() {
        return ds ? this : null;
    }

    private void cs() {
        final int indexOfChild = indexOfChild(mSmartspaceContent);
        removeView(mSmartspaceContent);
        final LayoutInflater from = LayoutInflater.from(getContext());
        addView(from.inflate(mDoubleLine ?
                R.layout.smartspace_twolines :
                R.layout.smartspace_singleline, this, false), indexOfChild);
        loadViews();
    }

    public void cq() {
        ds = dp.cY();
        if (dq != null) {
            cr(dq);
        } else {
            Log.d("SmartspaceView", "onGsaChanged but no data present");
        }
    }

    public void cr(final SmartspaceDataContainer dq2) {
        dq = dq2;
        boolean visible = mSmartspaceContent.getVisibility() == View.VISIBLE;
        if (!visible) {
            mSmartspaceContent.setVisibility(View.VISIBLE);
            mSmartspaceContent.setAlpha(0f);
            mSmartspaceContent.animate().setDuration(200L).alpha(1f);
        }
    }

    public void onAnimationUpdate(final ValueAnimator valueAnimator) {
        invalidate();
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        dp.da(this);
        if (mController != null && mFinishedInflate)
            mController.addListener(this);
    }

    public void onClick(final View view) {
        if (dq != null && dq.cS()) {
            dq.dP.click(view);
        }
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        SmartspaceController.get(getContext()).da(null);
        if (mController != null)
            mController.removeListener(this);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        loadViews();
        mFinishedInflate = true;
        dr = findViewById(R.id.dummyBubbleTextView);
        dr.setTag(new ItemInfo() {
            @Override
            public ComponentName getTargetComponent() {
                return new ComponentName(getContext(), "");
            }
        });
        dr.setContentDescription("");
        if (isAttachedToWindow() && mController != null)
            mController.addListener(this);
    }

    protected void onLayout(final boolean b, final int n, final int n2, final int n3, final int n4) {
        super.onLayout(b, n, n2, n3, n4);
        if (dq != null && dq.cS() && dq.dP.cv()) {
            final String cn = cn();
            if (!cn.equals(mTitleText.getText())) {
                mTitleText.setText(cn);
            }
        }
    }

    public boolean onLongClick(final View view) {
        LawnchairUtilsKt.openPopupMenu(dr, new SmartspacePreferencesShortcut());
        return true;
    }

    public void onPause() {
        mHandler.removeCallbacks(this);
        backportClockVisibility(false);
    }

    public void onResume() {
        backportClockVisibility(true);
    }

    private void backportClockVisibility(boolean show) {
        if (!Utilities.ATLEAST_NOUGAT && mClockView != null) {
            mClockView.onVisibilityAggregated(show && !mDoubleLine);
        }
    }

    @Override
    public void run() {

    }

    @Override
    public void setPadding(final int n, final int n2, final int n3, final int n4) {
        super.setPadding(0, 0, 0, 0);
    }

    final class h implements OnClickListener {
        final SmartspaceView dZ;

        h(final SmartspaceView dz) {
            dZ = dz;
        }

        public void onClick(final View view) {
            final Uri content_URI = CalendarContract.CONTENT_URI;
            final Uri.Builder appendPath = content_URI.buildUpon().appendPath("time");
            ContentUris.appendId(appendPath, System.currentTimeMillis());
            final Intent addFlags = new Intent(Intent.ACTION_VIEW).setData(appendPath.build()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            try {
                final Context context = dZ.getContext();
                Launcher.getLauncher(context).startActivitySafely(view, addFlags, null);
            } catch (ActivityNotFoundException ex) {
                LauncherAppsCompat.getInstance(dZ.getContext()).showAppDetailsForProfile(new ComponentName(DynamicIconProvider.GOOGLE_CALENDAR, ""), Process.myUserHandle());
            }
        }
    }
}