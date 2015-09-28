(function(){

	var app = angular.module('tt', []);

	app.controller('testCtrl', ['$http', function($http) {
		var self = this;
		self.tasks = [];
		self.task = {};
	    self.turns = [];
	    self.users = [];
	    self.userMap = {};
	    self.error = {
	    	code: undefined,
	    	msg: undefined,
	    	eMsg: undefined,
	    	stack: undefined
	    };
	    self.worst = 0;
	    self.me = {id: 1, name: 'Human'};
	    self.hoverId = 0;

	    var processTasksData = function(data) {
	    	if(data.error) {
	    		self.listError = data.error;
	    		self.tasks = [];
	    	} else if(data.tasks) {
	    		self.tasks = data.tasks;
	    		if(data.tasks.length && data.taskid) {
	    			self.tasks.forEach(function(task){
	    				if(task.id === data.taskid) {
	    					self.task = task;
	    				}
	    			});
	    		} else {
	    			self.listError = 'no tasks available';
	    		}
	    	} else {
	    		self.listError = 'Unknown tasks error';
	    	}
	    };

	    var processTurnsData = function(data) {
    		if(data.error) {
    			self.listError = data.error;
    			self.turns = [];
    			throw data.error;
    		} else if(data.turns) {
    			data.turns.forEach(function(turn){
    				turn.date = new Date(turn.date).toLocaleString();
    			});
    			self.turns = data.turns;
    			self.listError = null;
    		} else {
    			self.listError = 'Unknown turns error';
    			self.turns = [];
    		}
    		if(data.user) {
    			self.me = data.user;
    		}
    		if(data.taskid) {
    			self.tasks.forEach(function(task){
    				if(task.id === data.taskid) {
    					self.task = task;
    				}
    			});
    		}
    	};

    	var processStatusData = function(data) {
    		if(data.error) {
	    		self.statusError = data.error;
	    		self.users = [];
	    		throw data.error;
	    	} else if(data.users) {
	    		var max = data.users.reduce(function(max, user){ 
	    			return Math.max(max, user.turns);
	    		}, 0);
	    		self.userMap = {};
	    		data.users.forEach(function(user){
	    			user.diff = max - user.turns;
	    			self.userMap[user.id] = user.name;
	    		});
				self.worst = data.users[0].diff;
	    		self.users = data.users;
	    		self.statusError = null;
	    	} else {
	    		self.statusError = "Unknown status error";
	    		self.users = [];
	    	}
    	};

    	var handleApiError = function(res, whatFailed) {
			var existing = Array.prototype.slice.call(document.getElementsByTagName('iframe'));
			for(var i = 0; i < existing.length; i++) {
				existing[i].remove();
			}
			res = res || {};
    		self.error.code = res.status;
    		switch(typeof res.data) {
    			case 'string': {
		    		var match = res.data.match(/<html>([\s\S]*?)<\/html>/i);
		    		if(match && match[1]) {
		    			var iframe = document.createElement('iframe');
		    			iframe.style.width = '100%';
		    			document.getElementsByTagName('body')[0].appendChild(iframe);
						var doc = iframe.contentWindow || iframe.contentDocument.document || iframe.contentDocument;
						doc.document.open();
						doc.document.write(res.data);
						doc.document.close();
						iframe.style.height = (iframe.contentWindow.document.body.scrollHeight + 10) + 'px';

						self.error.msg = whatFailed;
					} else {
						self.error.msg = (whatFailed ? whatFailed + ' ' : '') + res.data;
					}
				} break;
				case 'object': {
					if(res.data && res.data.error) {
						self.error.msg = res.data.error.msg;
						self.error.eMsg = res.data.error.eMsg;
						self.error.stack = res.data.error.stack;
						break;
					}
					// fall-through
				}
				default: {
					self.error.msg = whatFailed;
					console.error(whatFailed, res.data);
				} break;
			}
    	};

    	self.clearError = function() {
    		self.error.code = undefined;
    		self.error.msg = undefined;
    		self.error.eMsg = undefined;
    		self.error.stack = undefined;
    	}

	    self.getTurns = function() {
	    	self.clearError();
	    	return $http.get('/api/turns', {params:{id:self.task.id}})
		    	.then(function(res){
		    		processStatusData(res.data);
		    	}, function(res){
		    		handleApiError(res, 'Failed to get turns');
		    	});
	    };

	    self.getStatus = function() {
	    	self.clearError();
	    	return $http.get('/api/status', {params:{id:self.task.id}})
		    	.then(function(res){
		    		processStatusData(res.data);
		    	}, function(res){
		    		handleApiError(res, 'Failed to get status');
		    	});
	    };

	    self.getTurnsAndStatus = function(taskId) {
	    	self.clearError();
	    	return $http.get('/api/turns-status', {params: {task_id: taskId, user_id: self.me.id}})
	    		.then(function(res){
	    			processTurnsData(res.data);
	    			processStatusData(res.data);
	    		}, function(res){
	    			handleApiError(res, 'Failed to turns/status');
	    		});
	    };

	    self.getAll = function() {
	    	self.clearError();
	    	return $http.get('/api/tasks-turns-status', {params: {userid: self.me.id}})
	    		.then(function(res){
	    			processTasksData(res.data);
	    			processTurnsData(res.data);
	    			processStatusData(res.data);
	    		}, function(res){
	    			handleApiError(res, 'Failed to tasks/turns/status');
	    		});
	    };

	    self.getTasks = function() {
	    	self.clearError();
	    	return $http.get('/api/tasks', {params:{userid:self.me.id}})
	    		.then(function(res){
	    			processTasksData(res.data);
	    		}, function(res){
	    			handleApiError(res, 'Failed to get tasks');
	    		});
	    };

	    self.takeTurn = function(userId) {
	    	self.clearError();
	    	return $http.post('/api/turn', {user_id: userId, task_id: self.task.id})
	    		.then(function(res){
	    			processTurnsData(res.data);
	    			processStatusData(res.data);
		    	}, function(res){
		    		handleApiError(err, 'failed to take turn');
	    		});
	    };

	    self.notify = function() {
	    	self.clearError();
	    	return $http.post('/api/notify', {message: 'message from TT'})
		    	.then(function(res){
		    		console.log('notify results', res.data);
		    	}, function(res) {
		    		handleApiError(res, 'Failed to notify');
		    	});
	    };

	    self.getUsers = function() {
	    	self.clearError();
	    	return $http.get('/api/users')
		    	.then(function(res){
		    		console.log('users', res.data);
		    	}, function(res){
		    		handleApiError(res, 'failed to get users');
		    	});
	    }

	    self.deleteTurn = function(turn) {
	    	self.clearError();
	    	return $http.delete('/api/turn', {params: {turn_id: turn.turnid, user_id: self.me.id, task_id: self.task.id}})
		    	.then(function(res){
	    			processTurnsData(res.data);
	    			processStatusData(res.data);
		    	}, function(res){
		    		handleApiError(res, 'failed to delete turn');
		    	});
	    };

	    self.getAll();
	}]);
})();

