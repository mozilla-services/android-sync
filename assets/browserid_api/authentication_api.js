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

    return JSON.parse(response);
  };

  navigator.id = {
    
    beginAuthentication: function(callback) {
    	console.log('navigator.id.beginAuthentication …');
			
      var response = rpc('beginAuthentication');

      if (!response || response.error) {
        callback(null);
        return;
      }
      var result = response.result;

      callback(result.identity);
    },

    completeAuthentication: function() {
    	console.log('navigator.id.completeAuthentication …');
    	
			rpc('completeAuthentication');
    },

    raiseAuthenticationFailure: function(reason) {
			console.log('navigator.id.raiseAuthenticationFailure …' + reason);
			
			rpc('raiseAuthenticationFailure', {reason: reason});
    }
    
  };

}).call(this);

