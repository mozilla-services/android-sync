package org.mozilla.persona.client;

import java.util.HashMap;

import android.os.Bundle;

public class PersonaUserAgent {

  protected String                     identity   = null;

  protected String                     domain     = null;

  protected HashMap<String, Indentity> identities = new HashMap<String, Indentity>();

  protected OnCompleteCallback         callback;

  public PersonaUserAgent(HashMap<String, Indentity> identities) {
    // TODO: Better reference to storage
    this.identities = identities;
  }

  /**
   * Add a new identity to the storage
   * 
   * @param String
   *          The email chosen for login
   */
  protected void addIdentity(final String identity) {
    if (identities != null && identities.containsKey(identity)) {
      return;
    }

    identities.put(identity, new Indentity());
  }

  /**
   * The user has picked an email address for login
   * 
   * @param String
   *          The email chosen for login
   */
  public void selectIdentity(final String identity,
      final OnCompleteCallback callback) {
    
    addIdentity(identity);

    this.callback = callback;

    discoverProvider(identity, new OnCompleteCallback() {

      @Override
      public void onSuccess(Bundle idpParams) {
        // TODO Auto-generated method stub

      }

      @Override
      public void onError(String message, Exception e) {
        // TODO Auto-generated method stub

      }
    });
  }

  public void discoverProvider(final String identity, final OnCompleteCallback callback) {
    final String domain = parseEmail(identity);
    
    fetchWellKnown(domain, new OnCompleteCallback() {
      
      @Override
      public void onSuccess(Bundle idpParams) {
        callback.onSuccess(idpParams);
      }
      
      @Override
      public void onError(String message, Exception e) {
        // TODO Auto-generated method stub
        
      }
    });
  }

  protected String parseEmail(String email) {
    // FIXME: Actually parse domain from email.

    return "eyedee.me";
  }

  protected class Indentity {
    public String cert;
    public String keyPair;
  }

  protected void fetchWellKnown(String domain, OnCompleteCallback callback) {
    // XXX: Make an actual HTTP request here using a tested client
    // XXX: Validate wellknown file
    // XXX: Fallback provider

    Bundle result = new Bundle();

    Bundle params = new Bundle();

    Bundle publicKey = new Bundle();

    // Sample data from https://eyedee.me/.well-known/browserid
    publicKey.putString("algorithm", "RS");
    publicKey
        .putString(
            "n",
            "82818905405105134410187227495885391609221288015566078542117409373192106382993306537273677557482085204736975067567111831005921322991127165013340443563713385983456311886801211241492470711576322130577278575529202840052753612576061450560588102139907846854501252327551303482213505265853706269864950437458242988327");
    publicKey.putString("e", "65537");
    params.putBundle("public-key", publicKey);

    params.putString("authentication", "/browserid/sign_in.html");
    params.putString("provisioning", "/browserid/provision.html");

    result.putString("domain", domain);
    result.putBundle("params", params);

    callback.onSuccess(result);
  }

  public interface OnCompleteCallback {
    void onSuccess(Bundle result);

    void onError(String message, Exception e);
  }

}
