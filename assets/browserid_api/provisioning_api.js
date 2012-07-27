(function() {

  /**
   * Message to Java
   * @param {Object} method
   * @param {Object} params
   */
  function rpc(method, params) {
  	
    var response = prompt('__channel__:' + JSON.stringify({
      method: method,
      params: params || null
    }));

    if (!response) {
      return null;
    }
    
    console.log('rpc ' + response);

    return JSON.parse(response);
  };

  navigator.id = {

    beginProvisioning: function(callback) {
      console.log('navigator.id.beginProvisioning …');

      var response = rpc('beginProvisioning');

      if (!response || response.error) {
        callback(null, null);
        return;
      }
      var result = response.result;

      callback(result.identity, result.certValidityDuration);
    },

    genKeyPair: function(callback) {
    	console.log('navigator.id.genKeyPair …');
    	
    	var response = rpc('genKeyPair');
    	
    	if (!response || response.error) {
        callback(null);
        return;
      }
      var result = response.result;
      
      callback(result.publicKey);
    },

    registerCertificate: function(certificate) {
    	console.log('navigator.id.registerCertificate … ' + certificate);
    	
			rpc('registerCertificate', {certificate: certificate});
    },

    raiseProvisioningFailure: function(reason) {
			console.log('navigator.id.raiseProvisioningFailure … ' + reason);
			
			rpc('raiseProvisioningFailure', {reason: reason});
    }
    
  };

}).call(this);

