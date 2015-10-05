(function(){

	var app = angular.module('tt', []);

	app.controller('AdminController', ['$http', function($http) {
		var self = this;
		self.tasks = [];
		self.currentTask = {};
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
	    self.reasons = [];
	    self.methods = [];

	    var enumArrayToObject = function(array) {
	    	var obj = {};
	    	array.forEach(function(e){
	    		obj[e.id] = e;
	    	});
	    	return obj;
	    };

	    var processTasksData = function(data) {
	    	if(data.error) {
	    		self.listError = data.error;
	    		self.tasks = [];
	    	} else if(data.tasks) {
	    		self.tasks = data.tasks;
	    		if(data.tasks.length && data.taskid) {
	    			self.tasks.forEach(function(task){
	    				if(task.taskId === data.taskid) {
	    					self.currentTask = task;
	    				}
	    			});
	    		} else {
	    			self.listError = 'no tasks available';
	    		}
	    		if(data.reasons && Array.isArray(data.reasons)) {
	    			self.reasons = data.reasons;
	    		}
	    		if(data.methods && Array.isArray(data.methods)) {
	    			self.methods = data.methods;
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
    				if(task.taskId === data.taskid) {
    					self.currentTask = task;
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

    	self.handleApiError = function(res, whatFailed) {
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
	    	return $http.get('/api/turns', {params:{id:self.currentTask.taskId}})
		    	.then(function(res){
		    		processStatusData(res.data);
		    	}, function(res){
		    		self.handleApiError(res, 'Failed to get turns');
		    	});
	    };

	    self.getStatus = function() {
	    	self.clearError();
	    	return $http.get('/api/status', {params:{id:self.currentTask.taskId}})
		    	.then(function(res){
		    		processStatusData(res.data);
		    	}, function(res){
		    		self.handleApiError(res, 'Failed to get status');
		    	});
	    };

	    self.getTurnsAndStatus = function(taskId) {
	    	self.clearError();
	    	return $http.get('/api/turns-status', {params: {task_id: taskId, user_id: self.me.id}})
	    		.then(function(res){
	    			processTurnsData(res.data);
	    			processStatusData(res.data);
	    		}, function(res){
	    			self.handleApiError(res, 'Failed to turns/status');
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
	    			self.handleApiError(res, 'Failed to tasks/turns/status');
	    		});
	    };

	    self.getTasks = function() {
	    	self.clearError();
	    	return $http.get('/api/tasks', {params:{userid:self.me.id}})
	    		.then(function(res){
	    			processTasksData(res.data);
	    		}, function(res){
	    			self.handleApiError(res, 'Failed to get tasks');
	    		});
	    };

	    self.takeTurn = function(userId) {
	    	self.clearError();
	    	return $http.post('/api/turn', {user_id: userId, task_id: self.currentTask.taskId})
	    		.then(function(res){
	    			processTurnsData(res.data);
	    			processStatusData(res.data);
		    	}, function(res){
		    		self.handleApiError(res, 'failed to take turn');
	    		});
	    };

	    self.notify = function() {
	    	self.clearError();
	    	return $http.post('/api/notify', {message: 'message from TT'})
		    	.then(function(res){
		    		console.log('notify results', res.data);
		    	}, function(res) {
		    		self.handleApiError(res, 'Failed to notify');
		    	});
	    };

	    self.getUsers = function() {
	    	self.clearError();
	    	return $http.get('/api/users')
		    	.then(function(res){
		    		console.log('users', res.data);
		    	}, function(res){
		    		self.handleApiError(res, 'failed to get users');
		    	});
	    }

	    self.deleteTurn = function(turn) {
	    	self.clearError();
	    	return $http.delete('/api/turn', {params: {turn_id: turn.turnid, user_id: self.me.id, task_id: self.currentTask.taskId}})
		    	.then(function(res){
	    			processTurnsData(res.data);
	    			processStatusData(res.data);
		    	}, function(res){
		    		self.handleApiError(res, 'failed to delete turn');
		    	});
	    };

	    self.getSubscriptions = function() {
	    	self.clearError();
	    	return $http.get('/api/subscriptions', {params: {user_id: self.me.id}})
	    		.then(function(res){
	    			console.log(res.data);
	    		}, function(res){
	    			self.handleApiError(res, 'failed to get subscriptions');
	    		});
	    };

	    self.getAll();
	}]);

	app.controller('NotificationController', ['$scope', '$http', function($scope, $http) {
		var vm = this;

		vm.pending = false;
		vm.waiting = false;
		vm.task = {};
		vm.newNote = {};
		vm.userId = 0;
		vm.valid = true;
		vm.invalidNote = '';
		vm.clearError = function(){};
		vm.handleApiError = function(){};

		vm.setTask = function(task) {
			vm.task = task;
			vm.newNote = {
				notification: task.notification,
				reason_id: task.reason_id,
				method_id: task.method_id,
				reminder: task.reminder ? 1 : 0
			};
		};

		vm.setErrorHandlers = function(clearError, handleApiError) {
			vm.clearError = clearError;
			vm.handleApiError = handleApiError;
		};

		vm.setUserId = function(userId) {
			vm.userId = userId;
		}

		// update DB with current values
		vm.update = function(){
			if(vm.pending && vm.valid) {
				vm.pending = false;
				vm.task.notification = vm.newNote.notification;
				vm.task.reason_id = vm.newNote.reason_id;
				vm.task.method_id = vm.newNote.method_id;
				vm.task.reminder = vm.newNote.reminder;

				vm.clearError();
				var httpMethod = $http.put;
				var params = {userId: vm.userId, taskId: vm.task.taskId, note: vm.newNote};
				if(!vm.newNote.notification) {
					httpMethod = $http.delete;
					params = {params: params};
				}
				vm.waiting = true;
		    	return httpMethod('/api/subscription', params)
		    		.then(function(res){
		    			// success
		    			vm.waiting = false;
			    	}, function(res){
			    		vm.handleApiError(res, 'failed to update notification');
		    			vm.waiting = false;
		    		});
			}
		};

		vm.remove = function(){
			vm.newNote.notification = 0;
		};

		// start allowing the form to be edited to add a new notification (will already be enabled if it exists to allow for editing)
		vm.add = function(){
			vm.newNote.notification = 1;
		};

		vm.checkForPendingChanges = function() {
			vm.pending =
				vm.task.notification !== vm.newNote.notification ||
				vm.task.reason_id !== vm.newNote.reason_id ||
				vm.task.method_id !== vm.newNote.method_id ||
				vm.task.reminder !== vm.newNote.reminder;
			if(vm.newNote.notification) {
				if(!vm.newNote.method_id) {
					vm.invalidNote = 'Must choose a method';
				} else if(!vm.newNote.reason_id) {
					vm.invalidNote = 'Must choose a reason';
				} else {
					vm.valid = true;
				}
				vm.valid = false;
			} else {
				vm.valid = true;
			}
			vm.valid =
				!vm.newNote.notification ||
				(vm.newNote.method_id > 0 && vm.newNote.reason_id > 0)
		};

		$scope.$watch(function(){return vm.newNote;}, function(){
			vm.checkForPendingChanges();
		}, true);
	}]);
})();

