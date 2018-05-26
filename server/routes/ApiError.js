
function ApiError(err, message, data) {
	var self = this;
	this.status = err.status;
	this.stack = err.stack;
	this.errorMessage = err.message;
	this.message = message;
	this.data = data;
	if(this.errorMessage === undefined) {
		this.errorMessage = err;
	}

	this.getDevResponse = function() {
		var res = self.data || {};
		res.error = {
            msg: self.message,
            eMsg: self.errorMessage,
            stack: self.stack
        };
        return res;
	};

	this.getResponse = function() {
		var res = self.data || {};
		res.error = {
			msg: self.message
		}
		return res;
	};
}

module.exports = ApiError;
