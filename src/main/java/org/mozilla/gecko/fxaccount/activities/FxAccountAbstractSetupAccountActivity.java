package org.mozilla.gecko.fxaccount.activities;

import org.mozilla.gecko.R;
import org.mozilla.gecko.fxaccount.FxAccountConstants;
import org.mozilla.gecko.fxaccount.FxAccountCreationException;
import org.mozilla.gecko.sync.Logger;

import android.accounts.Account;
import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public abstract class FxAccountAbstractSetupAccountActivity extends Activity {
  protected static final String LOG_TAG = FxAccountSetupExistingAccountActivity.class.getSimpleName();

  protected final int contentViewId;

  protected TextWatcher textChangedListener;

  protected Button nextButton;
  protected EditText emailEdit;
  protected EditText passwordEdit;
  protected EditText password2Edit;

  public FxAccountAbstractSetupAccountActivity(int contentViewId) {
    super();
    this.contentViewId = contentViewId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle icicle) {
    Logger.debug(LOG_TAG, "onCreate(" + icicle + ")");

    super.onCreate(icicle);
    setContentView(contentViewId);

    nextButton = (Button) ensureFindViewById(R.id.next, "next button");
    emailEdit = (EditText) ensureFindViewById(R.id.email, "email textedit");
    passwordEdit = (EditText) ensureFindViewById(R.id.password, "password textedit");
    // password2Edit is allowed to be null.
    password2Edit = (EditText) findViewById(R.id.password2);

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

  protected void updateButtonEnabled() {
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
    String message = "Created account with email address " + account.name;
    Logger.info(LOG_TAG, message);

    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
  }

  protected void displayException(FxAccountCreationException e) {
    Logger.warn(LOG_TAG, "Got exception.", e);

    Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
  }

  protected TextWatcher createTextChangedListener() {
    return new TextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        updateErrors();
        updateButtonEnabled(); // Do this second, since it uses edit text error states.
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

  protected void updateErrors() {
    String email = emailEdit.getText().toString();
    String password = passwordEdit.getText().toString();

    if (email.trim().length() == 0) {
      emailEdit.setError("You must enter your email address.");
    }

    if (password.length() == 0) {
      passwordEdit.setError("You must enter your password.");
    }

    // We might not have a second password input box.
    if (password2Edit == null) {
      return;
    }

    String password2 = password2Edit.getText().toString();

    if (password2.length() == 0) {
      password2Edit.setError("You must re-enter your password.");
    } else if (!password.equals(password2)) {
      password2Edit.setError("Your passwords must match.");
    } else {
      password2Edit.setError(null);
    }
  }
}
