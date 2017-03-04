/**
 *   Nextcloud Android client application
 *
 *   @author Bartek Przybylski
 *   @author masensio
 *   @author David A. Velasco
 *   Copyright (C) 2011 Bartek Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.ui.activity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static android.content.Context.INPUT_METHOD_SERVICE;


class SoftKeyboardUtil {
    private static final String TAG = SoftKeyboardUtil.class.getSimpleName();

    private AppCompatActivity mActivity;
    private boolean mIsSoftKeyboardOpened;
    private SoftKeyboardListener mSoftKeyboardListener;
    private int mHideCount;

    public interface SoftKeyboardListener {
        void onClose();
    }

    private void setListenerToRootView() {
        View view = mActivity.findViewById(android.R.id.content).getRootView();
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // navigation bar height
                int navigationBarHeight = 0;
                int resourceId = mActivity.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
                if (resourceId > 0) {
                    navigationBarHeight = mActivity.getResources().getDimensionPixelSize(resourceId);
                }

                // status bar height
                int statusBarHeight = 0;
                resourceId = mActivity.getResources().getIdentifier("status_bar_height", "dimen", "android");
                if (resourceId > 0) {
                    statusBarHeight = mActivity.getResources().getDimensionPixelSize(resourceId);
                }

                // display window size for the app layout
                Rect rect = new Rect();
                mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
                int appHeight = rect.height();

                // screen height
                View view = mActivity.findViewById(android.R.id.content).getRootView();
                int screenHeight = view.getHeight();

                // soft keyboard height
                int softKeyboardHeight = screenHeight - (statusBarHeight + navigationBarHeight + appHeight);

                if (softKeyboardHeight <= 0) {
                    if (mIsSoftKeyboardOpened) {
                        // soft keyboard was closed
                        if (mHideCount == 0) {
                            // back key was pressed
                            mSoftKeyboardListener.onClose();
                        }
                        mHideCount = 0;
                    }
                    mIsSoftKeyboardOpened = false;
                } else {
                    mIsSoftKeyboardOpened = true;
                }
            }
        });
    }

    public SoftKeyboardUtil(AppCompatActivity activity, SoftKeyboardListener softKeyboardListener) {
        mActivity = activity;
        mSoftKeyboardListener = softKeyboardListener;
        if (softKeyboardListener != null) {
            setListenerToRootView();
        }
        mHideCount = 0;
    }

    public void initHidden() {
        mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public void initVisible() {
        mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    public void show() {
        mHideCount = 0;
        View focusedView = mActivity.getCurrentFocus();
        if (focusedView != null) {
            InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(focusedView, 0);
        } else {
            Log_OC.i(TAG, "focusedView = null in show()");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    show();
                }
            }, 10);
        }
    }

    public void hide() {
        mHideCount++;
        View focusedView = mActivity.getCurrentFocus();
        if (focusedView != null) {
            InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        } else {
            Log_OC.i(TAG, "focusedView = null in hide()");
        }
    }

    public void appClose() {
        mHideCount = 0;
        mIsSoftKeyboardOpened = false;
    }
}

public class PassCodeActivity extends AppCompatActivity implements SoftKeyboardUtil.SoftKeyboardListener {

    private static final String TAG = PassCodeActivity.class.getSimpleName();

    public final static String ACTION_REQUEST_WITH_RESULT = "ACTION_REQUEST_WITH_RESULT";
    public final static String ACTION_CHECK_WITH_RESULT = "ACTION_CHECK_WITH_RESULT";
    public final static String ACTION_CHECK = "ACTION_CHECK";

    public final static String KEY_PASSCODE = "KEY_PASSCODE";
    public final static String KEY_CHECK_RESULT = "KEY_CHECK_RESULT";

    // NOTE: PREFERENCE_SET_PASSCODE must have the same value as preferences.xml-->android:key for passcode preference
    public final static String PREFERENCE_SET_PASSCODE = "set_pincode";

    public final static String PREFERENCE_PASSCODE_D = "PrefPinCode";
    public final static String PREFERENCE_PASSCODE_D1 = "PrefPinCode1";
    public final static String PREFERENCE_PASSCODE_D2 = "PrefPinCode2";
    public final static String PREFERENCE_PASSCODE_D3 = "PrefPinCode3";
    public final static String PREFERENCE_PASSCODE_D4 = "PrefPinCode4";

    private static final String AUTO_PREF__SOFT_KEYBOARD_MODE = "prefs_soft_keyboard_mode";

    // Preference
    private static final boolean INIT_SOFT_KEYBOARD_MODE = true;  // true=soft keyboard / false=buttons
    private static final boolean KEYPAD_SUBTEXT = true;
    private static final boolean KEYPAD_SIZE_FILL = false;
    private static final float KEYPAD_SIZE_INCH = 1.4F;   // = 3.556cm
    private static final int KEYPAD_POS = Gravity.CENTER | Gravity.BOTTOM;

    private static final boolean ENABLE_GO_HOME = false;
    private static final boolean ENABLE_SWITCH_SOFT_KEYBOARD = true;
    private static final int GUARD_TIME = 3000;    // (ms)
    private static final boolean ENABLE_SUFFLE_BUTTONS = false;

    private TextView mPassCodeHdr;
    private TextView mPassCodeHdrExplanation;
    private EditText mPassCodeEditText;

    private String mConfirmingPassCode;
    private boolean mConfirmingPassCodeFlag = false;
    private static final String KEY_CONFIRMING_PASSCODE = "CONFIRMING_PASSCODE";

    private static final int mButtonsIDList[] = {
            R.id.button0,
            R.id.button1,
            R.id.button2,
            R.id.button3,
            R.id.button4,
            R.id.button5,
            R.id.button6,
            R.id.button7,
            R.id.button8,
            R.id.button9,
            R.id.clear,
            R.id.back,
    };
    private static final String mButtonsMainStr[] = {
            "0",
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8",
            "9",
            "Clear",
            "Back"
    };
    private static String mButtonsSubStr[] = {
            "...",
            "",
            "abc",
            "def",
            "ghi",
            "jkl",
            "mno",
            "pqrs",
            "tuv",
            "wxyz",
            "",
            "softkey..."
    };
    private static final String mButtonsCtrlStr[] = {
            "",
            "h+",
            "fill",
            "w+",
            "h-",
            "reset",
            "w-",
            "right",
            "center",
            "left",
            "subtext",
            "set"
    };
    private static final String mButtonFormat1 = "<big>%s</big><br/><font color=\"grey\"><small></small></font>";
    private static final String mButtonFormat2 = "<big>%s</big><br/><font color=\"grey\"><small>%s</small></font>";
    private Integer[] mButtonsIDListShuffle = new Integer[12];
    private AppCompatButton[] mButtonsList = new AppCompatButton[12];
    private int mButtonVisibilityPrev;          // 0=startup/1=visible/2=invisible
    private boolean mSoftKeyboardMode;          // true=soft keyboard / false=buttons
    private boolean mCtrlKeyboardMode;
    private boolean mShowButtonsWhenSoftKeyboardClose = true;
    private SoftKeyboardUtil mSoftKeyboard;
    private SharedPreferences mPref;
    private boolean mNeedKeyboardSetup;
    private int count;
    ArrayList<Integer> passFields = new ArrayList<> ();


    /**
     * Initializes the activity.
     * <p>
     * An intent with a valid ACTION is expected; if none is found, an
     * {@link IllegalArgumentException} will be thrown.
     *
     * @param savedInstanceState Previously saved state - irrelevant in this case
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passcodelock);

        mPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mPassCodeHdr = (TextView) findViewById(R.id.header);
        mPassCodeHdrExplanation = (TextView) findViewById(R.id.explanation);
        setupButtons();
        setupPassCodeEditText();
        mSoftKeyboardMode = mPref.getBoolean(AUTO_PREF__SOFT_KEYBOARD_MODE, INIT_SOFT_KEYBOARD_MODE);
        mSoftKeyboard = new SoftKeyboardUtil(this, this);
        if (!ENABLE_SWITCH_SOFT_KEYBOARD) {
            mButtonsSubStr[11] = "";
        }

        passFields.add (R.id.pass_1);
        passFields.add (R.id.pass_2);
        passFields.add (R.id.pass_3);
        passFields.add (R.id.pass_4);

        mNeedKeyboardSetup = true;
        if (ACTION_CHECK.equals(getIntent().getAction())) {
            /// this is a pass code request; the user has to input the right value
            mPassCodeHdr.setText(R.string.pass_code_enter_pass_code);
            mPassCodeHdrExplanation.setVisibility(View.INVISIBLE);
            setCancelButtonEnabled(false);      // no option to cancel

        } else if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction())) {
            if (savedInstanceState != null) {
                mConfirmingPassCodeFlag = savedInstanceState.getBoolean(PassCodeActivity.KEY_CONFIRMING_PASSCODE);
                mConfirmingPassCode = savedInstanceState.getString(PassCodeActivity.KEY_PASSCODE);
            }
            if (!mConfirmingPassCodeFlag) {
                /// pass code preference has just been activated in Preferences;
                // will receive and confirm pass code value
                mPassCodeHdr.setText(R.string.pass_code_configure_your_pass_code);
                mPassCodeHdrExplanation.setVisibility(View.VISIBLE);

                if (mSoftKeyboardMode) {
                    mNeedKeyboardSetup = false;
                    setButtonsVisibility(false);
                }
                View view = findViewById(android.R.id.content);
                Snackbar
                        .make(view, R.string.pass_code_configure_your_pass_code_explanation,
                                Snackbar.LENGTH_LONG)
                        .setAction(R.string.common_ok, new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // nothing to do
                            }
                        })
                        .setCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                if (mSoftKeyboardMode) {
                                    setupKeyboard();
                                }
                            }
                        })
                        .show();
            } else {
                //the app was in the passcodeconfirmation
                requestPassCodeConfirmation();
            }
            setCancelButtonEnabled(true);

        } else if (ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
            /// pass code preference has just been disabled in Preferences;
            // will confirm user knows pass code, then remove it
            mPassCodeHdr.setText(R.string.pass_code_remove_your_pass_code);
            mPassCodeHdrExplanation.setVisibility(View.INVISIBLE);
            setCancelButtonEnabled(true);

        } else {
            throw new IllegalArgumentException("A valid ACTION is needed in the Intent passed to "
                    + TAG);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        clearPassCodeEditText();
        mButtonVisibilityPrev = 0;
        if (mNeedKeyboardSetup) {
            setupKeyboard();
            if (mSoftKeyboardMode) {
                mSoftKeyboard.initVisible();
            } else {
                mSoftKeyboard.initHidden();
            }
        }
        mNeedKeyboardSetup = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSoftKeyboard.appClose();
        SharedPreferences.Editor editor = mPref.edit();
        editor.putBoolean(AUTO_PREF__SOFT_KEYBOARD_MODE, mSoftKeyboardMode);
        editor.apply();
    }

    private void setupKeyboard() {
        mShowButtonsWhenSoftKeyboardClose = true;
        if (mSoftKeyboardMode) {
            showSoftKeyboard();
            setButtonsVisibility(false);
        } else {
            hideSoftKeyboard();
            setupKeypadParam();
            setButtonsVisibility(true);
        }
    }

    private void setupButtons() {
        for (int i = 0; i < mButtonsIDList.length; i++) {
            mButtonsIDListShuffle[i] = i;
        }
        for (int i = 0; i < mButtonsIDList.length; i++) {
            mButtonsList[i] = (AppCompatButton) findViewById(mButtonsIDList[i]);
            mButtonsList[i].setAllCaps(false);
        }
    }

    private void buttonAnimation(View view, boolean visible, int duration) {
        ObjectAnimator objectAnimator;
        if (visible) {
            objectAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        } else {
            objectAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        }
        objectAnimator.setDuration(duration);
        objectAnimator.start();
    }

    private void suffleButtonsIDList() {
        if (ENABLE_SUFFLE_BUTTONS) {
            List<Integer> list = Arrays.asList(mButtonsIDListShuffle);
            Collections.shuffle(list);
            mButtonsIDListShuffle = list.toArray(new Integer[list.size()]);
        }
    }

    private void setKeypadString(AppCompatButton b, int j, KeypadParam keypadParam) {
        boolean switch_soft_keyboard = (j == 11 && ENABLE_SWITCH_SOFT_KEYBOARD);
        boolean switch_ctrl_keyboard = (j == 0);
        String s;
//        if (keypadParam.subtext || switch_ctrl_keyboard || switch_soft_keyboard) {
//            s = String.format(mButtonFormat2, mButtonsMainStr[j], mButtonsSubStr[j]);
//        } else {
//            s = String.format(mButtonFormat1, mButtonsMainStr[j]);
//        }
//        b.setText(Html.fromHtml(s));
        b.setText(mButtonsMainStr[j]);
    }
    
    private void showKeypad() {
        int duration = mButtonVisibilityPrev == 0 ? 0 : 500;
        suffleButtonsIDList();
        KeypadParam keypadParam = getKeypadParam();
        for (int i = 0; i < mButtonsList.length; i++) {
            AppCompatButton b = mButtonsList[i];
            buttonAnimation(b, true, duration);
            b.setClickable(true);
            int j = ENABLE_SUFFLE_BUTTONS ? mButtonsIDListShuffle[i] : i;
            setKeypadString(b, j, keypadParam);
            b.setOnClickListener(new ButtonClicked(mPassCodeEditText, j));
            boolean switch_soft_keyboard = (j == 11 && ENABLE_SWITCH_SOFT_KEYBOARD);
            if (switch_soft_keyboard) {
                b.setLongClickable(true);
                b.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        mSoftKeyboardMode = !mSoftKeyboardMode;
                        setupKeyboard();
                        return true;
                    }
                });
            }
            boolean switch_ctrl_keyboard = (j == 0);
            if (switch_ctrl_keyboard) {
                b.setLongClickable(true);
                b.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        mCtrlKeyboardMode = true;
                        mSoftKeyboardMode = false;
                        setButtonsVisibility(false);
                        setupKeyboard();
                        return true;
                    }
                });
            }
        }
    }

    private void showCtrlKeypad() {
        int duration = mButtonVisibilityPrev == 0 ? 0 : 500;
        KeypadParam keypadParam = getKeypadParam();
        for (int i = 0; i < mButtonsList.length; i++) {
            AppCompatButton b = mButtonsList[i];
            buttonAnimation(b, true, duration);
            b.setClickable(true);
            b.setText(mButtonsCtrlStr[i]);
            b.setOnClickListener(new CtrlButtonClicked(i, this, keypadParam));
        }
    }

    private void hideKeypad() {
        int duration = mButtonVisibilityPrev == 0 ? 0 : 500;
        for (AppCompatButton b: mButtonsList) {
            buttonAnimation(b, false, duration);
            b.setClickable(false);
            b.setOnClickListener(null);
            b.setLongClickable(false);
            b.setOnLongClickListener(null);
        }
    }
    
    private void setButtonsVisibility(boolean visible) {
        if (visible && mButtonVisibilityPrev != 1) {
            if (!mCtrlKeyboardMode) {
                showKeypad();
            } else {
                showCtrlKeypad();
            }
            mButtonVisibilityPrev = 1;
        }
        if (!visible && mButtonVisibilityPrev != 2) {
            hideKeypad();
            mButtonVisibilityPrev = 2;
        }
    }

    /**
     * Enables or disables the cancel button to allow the user interrupt the ACTION
     * requested to the activity.
     *
     * @param enabled 'True' makes the cancel button available, 'false' hides it.
     */
    protected void setCancelButtonEnabled(boolean enabled) {
        AppCompatButton cancel = (AppCompatButton) findViewById(R.id.cancel);
        if (enabled) {
            cancel.setVisibility(View.VISIBLE);
            cancel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        } else {
            cancel.setVisibility(View.GONE);
            cancel.setOnClickListener(null);
        }
    }

    protected void setupPassCodeEditText() {
        TextInputLayout til = (TextInputLayout) findViewById(R.id.passcode);
        EditText et = til.getEditText();
        //et.setTextIsSelectable(false);     // TODO:no effect for double tap?
        //et.setContextClickable(false);
        if (Build.VERSION.SDK_INT >= 21) {
            et.setShowSoftInputOnFocus(false);  // for disabling popup soft keyboard when double clicked
        }
        et.requestFocus();
        et.addTextChangedListener(new TextWatcher(){
            @Override
            public void afterTextChanged(Editable s) {
                String passCode = mPassCodeEditText.getText().toString();
                if (passCode.length() == 4) {
                    processFullPassCode(passCode);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // nothing to do
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // nothing to do
            }
        });
        mPassCodeEditText = et;
    }

    /**
     * Processes the pass code entered by the user just after the last digit was in.
     * <p>
     * Takes into account the action requested to the activity, the currently saved pass code and
     * the previously typed pass code, if any.
     */
    private void processFullPassCode(String passCode) {
        if (ACTION_CHECK.equals(getIntent().getAction())) {
            if (checkPassCode(passCode)) {
                /// pass code accepted in request, user is allowed to access the app
                finish();

            } else {
                mPassCodeHdr.setText(R.string.pass_code_enter_pass_code);
                showErrorMessage(R.string.pass_code_wrong);
                startGuard();
            }

        } else if (ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
            if (checkPassCode(passCode)) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(KEY_CHECK_RESULT, true);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                mPassCodeHdr.setText(R.string.pass_code_enter_pass_code);
                showErrorMessage(R.string.pass_code_wrong);
                startGuard();
            }

        } else if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction())) {
            /// enabling pass code
            if (!mConfirmingPassCodeFlag) {
                requestPassCodeConfirmation();
                mConfirmingPassCodeFlag = true;
                mConfirmingPassCode = passCode;

            } else if (mConfirmingPassCode.equals(passCode)) {
                /// confirmed: user typed the same pass code twice
                Intent resultIntent = new Intent();
                resultIntent.putExtra(KEY_PASSCODE, mConfirmingPassCode);
                setResult(RESULT_OK, resultIntent);
                finish();

            } else {
                mPassCodeHdr.setText(R.string.pass_code_configure_your_pass_code);
                showErrorMessage(R.string.pass_code_mismatch);
                startGuard();
            }
        }
    }

    private void showErrorMessage(int errorMessage) {
        CharSequence errorSeq = getString(errorMessage);
        TextInputLayout til = (TextInputLayout) findViewById(R.id.passcode);
        til.setError(errorSeq);
    }

    private void eraseErrorMessage() {
        TextInputLayout til = (TextInputLayout) findViewById(R.id.passcode);
        til.setError(null);
    }

    private void startGuard() {
        if (!mSoftKeyboardMode) {
            setButtonsVisibility(false);
        } else {
            hideSoftKeyboard();
        }
//        mPassCodeEditText.setFocusable(false);
        mPassCodeEditText.setClickable(false);
        Animation animation = AnimationUtils.loadAnimation(PassCodeActivity.this, R.anim.shake);
        mPassCodeEditText.startAnimation(animation);
        TextInputLayout til = (TextInputLayout) findViewById(R.id.passcode);
        til.setPasswordVisibilityToggleEnabled(false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                endGuard();
            }
        }, GUARD_TIME);
    }

    private void endGuard() {
        mPassCodeEditText.setClickable(true);
        mPassCodeEditText.requestFocus();
        eraseErrorMessage();
        TextInputLayout til = (TextInputLayout) findViewById(R.id.passcode);
        til.setPasswordVisibilityToggleEnabled(true);
        if (ACTION_CHECK.equals(getIntent().getAction())) {
            setupKeyboard();
            clearPassCodeEditText();
        } else if (ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(KEY_CHECK_RESULT, true);
            setResult(RESULT_CANCELED, resultIntent);
            finish();
        } else if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction())) {
            setupKeyboard();
            clearPassCodeEditText();
            mPassCodeHdrExplanation.setVisibility(View.VISIBLE);
            mConfirmingPassCodeFlag = false;
        }
    }

    private void hideSoftKeyboard() {
        mShowButtonsWhenSoftKeyboardClose = false;
        mSoftKeyboard.hide();
    }

    private void showSoftKeyboard() {
        mSoftKeyboard.show();
    }

    /**
     * Ask to the user for retyping the pass code just entered before saving it as the current pass
     * code.
     */
    protected void requestPassCodeConfirmation() {
        clearPassCodeEditText();
        mPassCodeHdr.setText(R.string.pass_code_reenter_your_pass_code);
        mPassCodeHdrExplanation.setVisibility(View.INVISIBLE);
    }

    /**
     * Compares pass code entered by the user with the value currently saved in the app.
     *
     * @return 'True' if entered pass code equals to the saved one.
     */
    protected boolean checkPassCode(String passCode) {
        String savedPassCode = "";
        savedPassCode += mPref.getString(PREFERENCE_PASSCODE_D1, null);
        savedPassCode += mPref.getString(PREFERENCE_PASSCODE_D2, null);
        savedPassCode += mPref.getString(PREFERENCE_PASSCODE_D3, null);
        savedPassCode += mPref.getString(PREFERENCE_PASSCODE_D4, null);

        return passCode.equals(savedPassCode);
    }

    /**
     * Sets the input fields to empty strings and puts the focus on the first one.
     */
    protected void clearPassCodeEditText() {
        mPassCodeEditText.setText("");
    }

    /**
     * Overrides click on the BACK arrow to correctly cancel ACTION_ENABLE or ACTION_DISABLE, while
     * preventing than ACTION_CHECK may be worked around.
     *
     * @param keyCode Key code of the key that triggered the down event.
     * @param event   Event triggered.
     * @return 'True' when the key event was processed by this method.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (mCtrlKeyboardMode) {
                mCtrlKeyboardMode = false;
                mSoftKeyboardMode = false;
                setButtonsVisibility(false);
                setupKeyboard();
                return true;
            }
            if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction()) ||
                    ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
                finish();
            } else {
                if (ENABLE_GO_HOME) {
                    goHome();
                }
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void goHome() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PassCodeActivity.KEY_CONFIRMING_PASSCODE, mConfirmingPassCodeFlag);
        outState.putString(PassCodeActivity.KEY_PASSCODE, mConfirmingPassCode);
    }

    private void fillPassFields() {
        if (count < 4) {
            int id = passFields.get(count);

            findViewById(id).setBackground (getResources().getDrawable (R.drawable.passcode_circular_fill));
            count++;
        }
    }

    private class ButtonClicked implements OnClickListener {

        private int mIndex;
        private EditText mEditText;

        ButtonClicked(EditText editText, int index) {
            mEditText = editText;
            mIndex = index;
        }

        public void onClick(View v) {
            if (mIndex <= 9) {
                // 0,1,2,...,8,9
                int key = KeyEvent.KEYCODE_0 + mIndex;
                mEditText.dispatchKeyEvent(
                        new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, key, 0));
                mEditText.dispatchKeyEvent(
                        new KeyEvent(0, 0, KeyEvent.ACTION_UP, key, 0));
                fillPassFields();
            } else if (mIndex == 10) {
                // clear
                mEditText.setText("");
            } else {
                // delete
                mEditText.dispatchKeyEvent(
                        new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0));
                mEditText.dispatchKeyEvent(
                        new KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL, 0));
            }
        }
    }

    class KeypadParam {
        public boolean fill;
        public float widthInch;
        public float heightInch;
        public int pos;
        public boolean subtext;
    }

    private KeypadParam getKeypadParam() {
        KeypadParam param = new KeypadParam();
        Configuration config = getResources().getConfiguration();
        String orientation = config.orientation == Configuration.ORIENTATION_PORTRAIT ? "Portrait" : "Landscape";
        param.fill = mPref.getBoolean("Pref" + orientation + "Fill", KEYPAD_SIZE_FILL);
        param.widthInch = mPref.getFloat("Pref" + orientation + "Width", KEYPAD_SIZE_INCH);
        param.heightInch = mPref.getFloat("Pref" + orientation + "Height", KEYPAD_SIZE_INCH);
        param.pos = mPref.getInt("Pref" + orientation + "Pos", KEYPAD_POS);
        param.subtext = mPref.getBoolean("PrefSubtext", KEYPAD_SUBTEXT);

        param.fill = KEYPAD_SIZE_FILL;
        param.widthInch = KEYPAD_SIZE_INCH;
        param.heightInch = KEYPAD_SIZE_INCH;
        param.pos = KEYPAD_POS;
        param.subtext = KEYPAD_SUBTEXT;

        return param;
    }

    private void saveKeypadParam(KeypadParam param) {
        SharedPreferences.Editor editor = mPref.edit();
        Configuration config = getResources().getConfiguration();
        String orientation = config.orientation == Configuration.ORIENTATION_PORTRAIT ? "Portrait" : "Landscape";
        editor.putBoolean("Pref" + orientation + "Fill", param.fill);
        editor.putFloat("Pref" + orientation + "Width", param.widthInch);
        editor.putFloat("Pref" + orientation + "Height", param.heightInch);
        editor.putInt("Pref" + orientation + "Pos", param.pos);
        editor.putBoolean("PrefSubtext", param.subtext);
        editor.apply();
    }

    private void setupKeypadParam() {
        KeypadParam keyParam = getKeypadParam();
        setKeypadParam(keyParam);
    }

    private void setKeypadParam(KeypadParam keyParam) {
        int width;
        int height;
        if (keyParam.fill) {
            width = LinearLayout.LayoutParams.MATCH_PARENT;
            height = LinearLayout.LayoutParams.MATCH_PARENT;
        } else {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int densityDip = metrics.densityDpi;
            width = (int)(keyParam.widthInch * densityDip);
            height = (int)(keyParam.heightInch * densityDip);
        }
        LinearLayout ll = (LinearLayout) findViewById(R.id.LinearLayout);
        ll.getLayoutParams().width = width;
        ll.getLayoutParams().height = height;

        setKeypadPos(keyParam.pos);
    }

    private void setKeypadPos(int pos) {
//        RelativeLayout rl = (RelativeLayout) findViewById(R.id.RelativeLayout);
//        rl.setGravity(pos);
    }

    private class CtrlButtonClicked implements OnClickListener {

        private final static float INC_DEC_INCH_UNIT = 0.1F;
        private int mIndex;
        private LinearLayout mLinearLayout;
        private int mDensityDip;
        private KeypadParam mKeypadParam;

        CtrlButtonClicked(int index, AppCompatActivity activity, KeypadParam keypadParam) {
            mIndex = index;
            mLinearLayout = (LinearLayout) findViewById(R.id.LinearLayout);
            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mDensityDip = metrics.densityDpi;
            mKeypadParam = keypadParam;
        }

        private boolean isMatchParent() {
            return mLinearLayout.getLayoutParams().width == LinearLayout.LayoutParams.MATCH_PARENT;
        }

        private void addWidthInch(float inch) {
            if (!isMatchParent()) {
                mLinearLayout.getLayoutParams().width += (int)(inch * mDensityDip);
                mLinearLayout.requestLayout();
            }
        }

        private void addHeightInch(float inch) {
            if (!isMatchParent()) {
                mLinearLayout.getLayoutParams().height += (int)(inch * mDensityDip);
                mLinearLayout.requestLayout();
            }
        }

        @Override
        public void onClick(View v) {

            switch (mIndex) {
                case 11:
                default:
                    mKeypadParam.fill = isMatchParent();
                    mKeypadParam.widthInch = (float)mLinearLayout.getLayoutParams().width / mDensityDip;
                    mKeypadParam.heightInch = (float)mLinearLayout.getLayoutParams().height / mDensityDip;
                    saveKeypadParam(mKeypadParam);
                    mCtrlKeyboardMode = false;
                    mSoftKeyboardMode = false;
                    setButtonsVisibility(false);
                    setupKeyboard();
                    break;
                case 1:
                    addHeightInch(INC_DEC_INCH_UNIT);
                    break;
                case 4:
                    addHeightInch(-INC_DEC_INCH_UNIT);
                    break;
                case 3:
                    addWidthInch(INC_DEC_INCH_UNIT);
                    break;
                case 6:
                    addWidthInch(-INC_DEC_INCH_UNIT);
                    break;
                case 2:
                    if (!isMatchParent()) {
                        mLinearLayout.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                        mLinearLayout.getLayoutParams().height = LinearLayout.LayoutParams.MATCH_PARENT;
                        mLinearLayout.requestLayout();
                    }
                    break;
                case 5:
                    mKeypadParam.fill = KEYPAD_SIZE_FILL;
                    mKeypadParam.widthInch = KEYPAD_SIZE_INCH;
                    mKeypadParam.heightInch = KEYPAD_SIZE_INCH;
                    mKeypadParam.pos = KEYPAD_POS;
                    mKeypadParam.subtext = KEYPAD_SUBTEXT;
                    setKeypadParam(mKeypadParam);
                    mLinearLayout.requestLayout();
                    break;
                case 7:
                case 8:
                case 9:
                    int gravity =
                        mIndex == 7 ? Gravity.START :
                        mIndex == 8 ? Gravity.CENTER :
                        Gravity.END;
                    gravity = gravity | Gravity.BOTTOM;
                    mKeypadParam.pos = gravity;
                    setKeypadPos(gravity);
                    break;
                case 10:
                    mKeypadParam.subtext = !mKeypadParam.subtext;
                    String text = mKeypadParam.subtext ? "on" : "off";
                    Toast.makeText(PassCodeActivity.this, text, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    // when softKeyboard close
    @Override
    public void onClose()
    {
        if (ENABLE_SWITCH_SOFT_KEYBOARD) {
            if (mShowButtonsWhenSoftKeyboardClose) {
                mSoftKeyboardMode = false;
                setButtonsVisibility(true);
            }
        } else {
            if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction()) ||
                    ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
                // same as cancel button
                finish();
            } else {
                if (ENABLE_GO_HOME) {
                    goHome();
                } else {
                    showErrorMessage(R.string.pass_code_enter_pass_code);
                    startGuard();
                }
            }
        }
    }
}
