/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.helpers;

import org.mozilla.gecko.sync.repositories.domain.PasswordRecord;

public class PasswordHelpers {
  
  public static PasswordRecord createPassword1() {
    PasswordRecord rec = new PasswordRecord();
    rec.encType = "some type";
    rec.formSubmitURL = "http://submit.html";
    rec.hostname = "http://hostname";
    rec.httpRealm = "httpRealm";
    rec.password ="12345";
    rec.passwordField = "box.pass.field";
    rec.timeLastUsed = 123412352435L;
    rec.timesUsed = 5L;
    rec.username = "jvoll";
    rec.usernameField = "box.user.field";
    return rec;
  }
  
  public static PasswordRecord createPassword2() {
    PasswordRecord rec = new PasswordRecord();
    rec.encType = "some type";
    rec.formSubmitURL = "http://submit2.html";
    rec.hostname = "http://hostname2";
    rec.httpRealm = "httpRealm2";
    rec.password ="54321";
    rec.passwordField = "box.pass.field2";
    rec.timeLastUsed = 123412352213L;
    rec.timesUsed = 2L;
    rec.username = "rnewman";
    rec.usernameField = "box.user.field2";
    return rec;
  }

  public static PasswordRecord createPassword3() {
    PasswordRecord rec = new PasswordRecord();
    rec.encType = "some type3";
    rec.formSubmitURL = "http://submit3.html";
    rec.hostname = "http://hostname3";
    rec.httpRealm = "httpRealm3";
    rec.password ="54321";
    rec.passwordField = "box.pass.field3";
    rec.timeLastUsed = 123412352213L;
    rec.timesUsed = 2L;
    rec.username = "rnewman";
    rec.usernameField = "box.user.field3";
    return rec;
  }
  
  public static PasswordRecord createPassword4() {
    PasswordRecord rec = new PasswordRecord();
    rec.encType = "some type";
    rec.formSubmitURL = "http://submit4.html";
    rec.hostname = "http://hostname4";
    rec.httpRealm = "httpRealm4";
    rec.password ="54324";
    rec.passwordField = "box.pass.field4";
    rec.timeLastUsed = 123412354444L;
    rec.timesUsed = 4L;
    rec.username = "rnewman4";
    rec.usernameField = "box.user.field4";
    return rec;
  }

  public static PasswordRecord createPassword5() {
    PasswordRecord rec = new PasswordRecord();
    rec.encType = "some type5";
    rec.formSubmitURL = "http://submit5.html";
    rec.hostname = "http://hostname5";
    rec.httpRealm = "httpRealm5";
    rec.password ="54325";
    rec.passwordField = "box.pass.field5";
    rec.timeLastUsed = 123412352555L;
    rec.timesUsed = 5L;
    rec.username = "jvoll5";
    rec.usernameField = "box.user.field5";
    return rec;
  }
}
