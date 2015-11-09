(function(){

	var app = angular.module('tt', []);

	app.controller('AdminController', ['$scope', '$http', '$location', function($scope, $http, $location) {
		var self = this;
		self.tasks = [];
		self.currentTask = {};
	    self.turns = [];
	    self.users = [];
	    self.allUsers = [];
	    self.userMap = {};
	    self.error = {
	    	code: undefined,
	    	msg: undefined,
	    	eMsg: undefined,
	    	stack: undefined
	    };
	    self.worst = 0;
	    self.viewingUserId = parseInt($location.search().u) || 1;
	    self.me = {id: self.viewingUserId};
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
    				turn.diff = turn.date !== turn.inserted;
    				turn.date = new Date(turn.date).toLocaleString();
    				turn.inserted = new Date(turn.inserted).toLocaleString();
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
	    			if(user.id === self.me.id) {
	    				self.me = user;
	    			}
	    			user.diff = max - user.turns;
	    			self.userMap[user.id] = user.name;
	    		});
				self.worst = data.users.length ? data.users[0].diff : 0;
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
	    			$location.search('t', taskId);
	    			processTurnsData(res.data);
	    			processStatusData(res.data);
	    		}, function(res){
	    			self.handleApiError(res, 'Failed to turns/status');
	    		});
	    };

	    self.getAll = function(taskid) {
	    	self.clearError();
	    	return $http.get('/api/tasks-turns-status', {params: {userid: self.me.id, taskid: taskid}})
	    		.then(function(res){
	    			$location.search('t', res.data.taskid);
	    			processTasksData(res.data);
	    			processTurnsData(res.data);
	    			processStatusData(res.data);
	    			self.getUsers();
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
		    		self.allUsers = res.data.users;
		    	}, function(res){
		    		self.handleApiError(res, 'failed to get users');
		    	});
	    };

	    self.getUser = function(username) {
	    	self.clearError();
	    	return $http.get('/api/user', {params: {username: username}})
		    	.then(function(res){
		    		console.log('user', res.data);
		    	}, function(res){
		    		self.handleApiError(res, 'failed to get user');
		    	});
	    };

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

	    $scope.$watch(function(){return self.viewingUserId;}, function(newUserId){
	    	if(newUserId !== self.me.id) {
				$location.search('u', newUserId);
				window.location.reload();
			}
		}, true);

	    self.getAll($location.search().t);
	}]);

	app.controller('CreateUserController', ['$http', '$location', function($http, $location){
		var vm = this;

		vm.busy = false;
		vm.username = '';
		vm.displayname = '';
		vm.clearError = function(){};
		vm.handleApiError = function(){};

		vm.setErrorHandlers = function(clearError, handleApiError) {
			vm.clearError = clearError;
			vm.handleApiError = handleApiError;
		};

		vm.save = function() {
			if(vm.busy) {
				return;
			}
			vm.busy = true;
			vm.clearError();
	    	return $http.post('/api/user', {username: vm.username, displayname: vm.displayname})
	    		.then(function(res){
	    			console.log('res', res);
	    			// success
	    			vm.busy = false;
	    			vm.username = '';
	    			vm.displayname = '';
					$location.search('u', res.data.user_id);
					window.location.reload();
		    	}, function(res){
		    		vm.handleApiError(res, 'failed to save new user');
	    			vm.busy = false;
	    		});
		};
	}]);

	app.controller('CreateTaskController', ['$scope', '$http', '$location', function($scope, $http, $location){
		var vm = this;

		vm.busy = false;
		vm.name = '';
		vm.hours = 0;
		vm.clearError = function(){};
		vm.handleApiError = function(){};
		vm.users = [];
		vm.userMap = {};
		vm.selectedUsers = [];
		vm.me = {};

		vm.setErrorHandlers = function(clearError, handleApiError) {
			vm.clearError = clearError;
			vm.handleApiError = handleApiError;
		};

		vm.setUsers = function(me, users) {
			vm.me = me;
			vm.taskUserIds = [vm.me.id];
			vm.taskUsers = [vm.me];
			vm.users = users.filter(function(user){
				vm.userMap[user.id] = user;
				return user.id !== me.id;
			});
		};

		vm.save = function() {
			if(vm.busy) {
				return;
			}
			vm.busy = true;
			vm.clearError();
	    	return $http.post('/api/task', {name: vm.name, hours: vm.hours, creator: vm.me.id, users: vm.taskUserIds.concat(vm.me.id)})
	    		.then(function(res){
	    			console.log('res', res);
	    			// success
	    			vm.busy = false;
	    			vm.name = '';
	    			vm.hours = 0;
	    			vm.selectedUsers = [];
					$location.search('u', res.data.user_id);
					window.location.reload();
		    	}, function(res){
		    		vm.handleApiError(res, 'failed to save new user');
	    			vm.busy = false;
	    		});
		};

		// $scope.$watch(function(){return vm.selectedUsers;}, function(newSelectedUsers){
		// 	vm.taskUserIds = [vm.me.id].concat(newSelectedUsers);
		// 	vm.taskUsers = vm.users.filter(function(user){
		// 		return vm.taskUserIds.contains(user.id);
		// 	});
		// }, true);
	}]);

	app.controller('NotificationController', ['$scope', '$http', function($scope, $http) {
		var vm = this;

		vm.pending = false;
		vm.waiting = false;
		vm.task = {};
		vm.newNote = {};
		vm.user = {};
		vm.valid = true;
		vm.invalidNote = '';
		vm.clearError = function(){};
		vm.handleApiError = function(){};

		vm.setTask = function(task) {
			task.reminder = task.reminder ? 1 : 0;
			vm.task = task;
			vm.newNote = {
				notification: task.notification,
				reason_id: task.reason_id,
				method_id: task.method_id,
				reminder: task.reminder
			};
		};

		vm.setErrorHandlers = function(clearError, handleApiError) {
			vm.clearError = clearError;
			vm.handleApiError = handleApiError;
		};

		vm.setUser = function(user) {
			vm.user = user;
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
				var params = {userId: vm.user.id, taskId: vm.task.taskId, note: vm.newNote};
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
			vm.pending = vm.task.notification !== vm.newNote.notification ||
				(vm.newNote.notification && (
					vm.task.reason_id !== vm.newNote.reason_id ||
					vm.task.method_id !== vm.newNote.method_id ||
					vm.task.reminder !== vm.newNote.reminder
				));
			if(vm.newNote.notification) {
				var valid = false;
				if(!vm.newNote.method_id) {
					vm.invalidNote = 'Must choose a method';
				} else if(!vm.user.mobile && vm.newNote.method_id === 1) {
					vm.invalidNote = "User doesn't have mobile access configured";
					// todo - maybe we should just disable the android option instead of allowing them to select it
				} else if(!vm.newNote.reason_id) {
					vm.invalidNote = 'Must choose a reason';
				} else {
					valid = true;
				}
				vm.valid = valid;
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

