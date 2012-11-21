package org.mozilla.gecko.fxaccount.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.fxaccount.FxAccountConstants;
import org.mozilla.gecko.fxaccount.FxAccountCreationException;
import org.mozilla.gecko.sync.Logger;

import android.accounts.Account;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public abstract class FxAccountAbstractSetupAccountActivity extends Activity {
  public static final String LOG_TAG = FxAccountSetupExistingAccountActivity.class.getSimpleName();

  protected final int contentViewId;

  protected enum State { READY, WORKING, ERROR };
  protected State state;

  protected TextWatcher textChangedListener;

  protected EditText emailEdit;
  protected EditText passwordEdit;
  protected EditText password2Edit;

  protected ViewGroup disabledLayer;

  protected Button nextButton;
  protected ProgressBar progressBar;
  protected TextView errorTextView;

  public FxAccountAbstractSetupAccountActivity(int contentViewId) {
    super();
    this.contentViewId = contentViewId;
    this.state = State.READY;
  }

  /**
   * Helper to set enabled state of all input controls.
   *
   * @param enabled whether to enable or disable.
   */
  private void setControlsEnabled(boolean enabled) {
    // Hide/show a light box over everything...
    if (enabled) {
      fadeOut(disabledLayer, 500);
    } else {
      fadeIn(disabledLayer, 0);
    }

    // ... because disabling doesn't look significantly different.
    emailEdit.setEnabled(enabled);
    passwordEdit.setEnabled(enabled);
    if (password2Edit != null) {
      password2Edit.setEnabled(enabled);
    }
  }

  private void fadeIn(View view, long durationInMilliseconds) {
    if (view.getVisibility() == View.VISIBLE) {
      return;
    }

    Animation inAnimation = AnimationUtils.loadAnimation(this, R.anim.fadein);
    inAnimation.setDuration(durationInMilliseconds);

    view.setVisibility(View.VISIBLE);
    view.startAnimation(inAnimation);
  }

  private void fadeOut(final View view, long durationInMilliseconds) {
    if (view.getVisibility() == View.INVISIBLE) {
      return;
    }

    Animation outAnimation = AnimationUtils.loadAnimation(this, R.anim.fadeout);
    outAnimation.setDuration(durationInMilliseconds);

    outAnimation.setAnimationListener(new AnimationListener() {
      @Override
      public void onAnimationStart(Animation animation) {
        // Do nothing.
      }

      @Override
      public void onAnimationRepeat(Animation animation) {
        // Do nothing.
      }

      @Override
      public void onAnimationEnd(Animation animation) {
        view.setVisibility(View.INVISIBLE);
      }
    });

    view.startAnimation(outAnimation);
  }

  protected void setState(State newState, String errorMessage) {
    Logger.debug(LOG_TAG, "Setting state to " + newState + " (" + errorMessage + ").");

    this.state = newState;

    if (newState == State.READY) {
      nextButton.setVisibility(View.VISIBLE);
      progressBar.setVisibility(View.INVISIBLE);
      errorTextView.setVisibility(View.INVISIBLE);

      this.setControlsEnabled(true);
      this.updateValidation();
    } else if (newState == State.WORKING) {
      nextButton.setVisibility(View.INVISIBLE);
      progressBar.setVisibility(View.VISIBLE);
      errorTextView.setVisibility(View.INVISIBLE);

      this.setControlsEnabled(false);
      this.updateValidation();
    } else if (newState == State.ERROR) {
      nextButton.setVisibility(View.INVISIBLE);
      progressBar.setVisibility(View.INVISIBLE);
      errorTextView.setVisibility(View.VISIBLE);

      errorTextView.setText(errorMessage);

      this.setControlsEnabled(true);
      this.updateValidation();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle icicle) {
    Logger.debug(LOG_TAG, "onCreate(" + icicle + ")");

    super.onCreate(icicle);
    setContentView(contentViewId);

    emailEdit = (EditText) ensureFindViewById(R.id.email, "email textedit");
    passwordEdit = (EditText) ensureFindViewById(R.id.password, "password textedit");
    // password2Edit is allowed to be null.
    password2Edit = (EditText) findViewById(R.id.password2);

    nextButton = (Button) ensureFindViewById(R.id.next, "next button");
    progressBar = (ProgressBar) ensureFindViewById(R.id.progress, "progress bar");
    errorTextView = (TextView) ensureFindViewById(R.id.error, "error textview");

    disabledLayer = (ViewGroup) ensureFindViewById(R.id.disabledLayer, "disabled layer");

    // Need controls initialized in order to set default values.
    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      setDefaultValues(extras);
    }

    textChangedListener = createTextChangedListener();

    if (textChangedListener != null) {
      emailEdit.addTextChangedListener(textChangedListener);
      passwordEdit.addTextChangedListener(textChangedListener);
      if (password2Edit != null) {
        password2Edit.addTextChangedListener(textChangedListener);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDestroy() {
    Logger.debug(LOG_TAG, "onDestroy");

    if (textChangedListener != null) {
      emailEdit.removeTextChangedListener(textChangedListener);
      passwordEdit.removeTextChangedListener(textChangedListener);
      if (password2Edit != null) {
        password2Edit.removeTextChangedListener(textChangedListener);
      }

      textChangedListener = null;
    }

    super.onDestroy();
  }

  protected void setDefaultValues(Bundle icicle) {
    if (icicle == null) {
      return;
    }

    // Set default values if they are specified.
    String email = icicle.getString(FxAccountConstants.PARAM_EMAIL);
    if (email != null) {
      emailEdit.setText(email);
    }

    String password = icicle.getString(FxAccountConstants.PARAM_PASSWORD);
    if (password != null) {
      passwordEdit.setText(password);

      if (password2Edit != null) {
        password2Edit.setText(password);
      }
    }
  }

  /**
   * Helper to find view or error if it is missing.
   *
   * @param id of view to find.
   * @param description to print in error.
   * @return non-null <code>View</code> instance.
   */
  protected View ensureFindViewById(int id, String description) {
    View view = findViewById(id);

    if (view == null) {
      String message = "Could not find view " + description + ".";
      Logger.error(LOG_TAG, message);
      throw new RuntimeException(message);
    }

    return view;
  }

  private void updateButtonEnabled() {
    boolean enabled = true;

    enabled = enabled && (emailEdit.getError() == null);
    enabled = enabled && (passwordEdit.getError() == null);
    if (password2Edit != null) {
      enabled = enabled && (password2Edit.getError() == null);
    }

    if (enabled == nextButton.isEnabled()) {
      return;
    }

    Logger.debug(LOG_TAG, (enabled ? "En" : "Dis") + "abling next button.");

    nextButton.setEnabled(enabled);
  }

  protected void displaySuccess(Account account) {
    setState(State.READY, null);

    String message = "Created account with email address " + account.name;
    Logger.info(LOG_TAG, message);

    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
  }

  protected void displayException(FxAccountCreationException e) {
    Logger.warn(LOG_TAG, "Got exception.", e);

    setState(State.ERROR, e.getMessage());
  }

  protected TextWatcher createTextChangedListener() {
    return new TextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        updateValidation();

        setState(State.READY, null);
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Do nothing.
      }

      @Override
      public void afterTextChanged(Editable s) {
        // Do nothing.
      }
    };
  }

  protected void updateValidation() {
    String email = emailEdit.getText().toString();
    String password = passwordEdit.getText().toString();

    if (email.length() == 0) {
      emailEdit.setError("You must enter your email address.");
    } else {
      emailEdit.setError(null);
    }

    if (password.length() == 0) {
      passwordEdit.setError("You must enter your password.");
    } else {
      passwordEdit.setError(null);
    }

    // We might not have a second password input box.
    if (password2Edit != null) {
      String password2 = password2Edit.getText().toString();

      if (password2.length() == 0) {
        password2Edit.setError("You must re-enter your password.");
      } else if (!password.equals(password2)) {
        password2Edit.setError("Your passwords must match.");
      } else {
        password2Edit.setError(null);
      }
    }

    // This uses the error validation states, so do it last.
    updateButtonEnabled();
  }

  /**
   * Helper to create a receiver that will receive the result of a
   * "create Android account" request from a
   * </code>FxAccountIntentService</code> instance.
   *
   * @return a <code>ResultReceiver</code> instance.
   */
  protected ResultReceiver createResultReceiver() {
    Handler handler = new Handler(); // Ensure we get our result on the UI thread.

    return new ResultReceiver(handler) {
      @Override
      protected void onReceiveResult(int resultCode, Bundle resultData) {
        Logger.debug(LOG_TAG, "onReceiveResult " + resultCode + " " + resultData.keySet());

        if (resultCode == RESULT_OK) {
          Account account = resultData.getParcelable(FxAccountConstants.PARAM_ACCOUNT);

          displaySuccess(account);

          Intent result = new Intent();
          result.putExtra(FxAccountConstants.PARAM_ACCOUNT, account);

          setResult(RESULT_OK, result);
          finish();

          return;
        }

        String error = resultData.getString(FxAccountConstants.PARAM_ERROR);
        displayException(new FxAccountCreationException(error));

      }
    };
  }
}
