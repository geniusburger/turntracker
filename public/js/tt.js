(function(){

	var app = angular.module('tt', []);

	app.controller('testCtrl', ['$http', function($http) {
		var self = this;
		self.tasks = [];
		self.task = {};
	    self.turns = [];
	    self.users = [];
	    self.userMap = {};
	    self.listError = null;
	    self.statusError = null;
	    self.turnError = null;
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

    	var displayErrorPage = function(err) {
    		if(typeof err === 'string') {
	    		var match = err.match(/<html>([\s\S]*?)<\/html>/i);
	    		if(match && match[1]) {
	    			var existing = Array.prototype.slice.call(document.getElementsByTagName('iframe'));
	    			for(var i = 0; i < existing.length; i++) {
	    				existing[i].remove();
	    			}

	    			var iframe = document.createElement('iframe');
	    			iframe.style.width = '100%';
	    			document.getElementsByTagName('body')[0].appendChild(iframe);
					var doc = iframe.contentWindow || iframe.contentDocument.document || iframe.contentDocument;
					doc.document.open();
					doc.document.write(err);
					doc.document.close();
					iframe.style.height = (iframe.contentWindow.document.body.scrollHeight + 10) + 'px';
					return;
				} else {
					console.log('no match');
				}
			} else {
				console.log("type of error is '" + typeof err);
			}
			throw err;
    	};

	    self.getTurns = function() {
	    	return $http.get('/api/turns', {params:{id:self.task.id}}).success(processTurnsData).error(function(err){console.error('getTurns failed', err); throw err;});
	    };

	    self.getStatus = function() {
	    	return $http.get('/api/status', {params:{id:self.task.id}}).success(processStatusData).error(function(err){console.error('getStatus failed', err); throw err;});
	    };

	    self.getTurnsAndStatus = function(taskId) {
	    	return $http.get('/api/turns-status', {params:{id:taskId}})
	    		.success(function(data){
	    			processTurnsData(data);
	    			processStatusData(data);
	    		}).error(function(err){
	    			console.error('getTurnsAndStatus failed', err);
	    			displayErrorPage(err);
	    		});
	    };

	    self.getAll = function() {
	    	return $http.get('/api/tasks-turns-status', {params: {userid:self.me.id}})
	    		.success(function(data){
	    			processTasksData(data);
	    			processTurnsData(data);
	    			processStatusData(data);
	    		}).error(function(err){
	    			console.error('getAll failed', err);
	    			displayErrorPage(err);
	    		});
	    };

	    self.getTasks = function() {
	    	return $http.get('/api/tasks', {params:{userid:self.me.id}})
	    		.success(function(data){
	    			processTasksData(data);
	    		}).error(function(err){
	    			console.error('getTasks failed', err);
	    			displayErrorPage(err);
	    		});
	    };

	    self.takeTurn = function(userId) {
	    	return $http.post('/api/turn', {user_id: userId, task_id: self.task.id}).success(function(data){
	    		if(data.error) {
	    			self.turnError = data.error;
	    		} else {
	    			self.turnError = null;
	    			console.log('take results', data);
	    			processTurnsData(data);
	    			processStatusData(data);
	    		}
	    	}).error(function(err){
    			console.error('takeTurn failed', err);
    			displayErrorPage(err);
    		});
	    };

	    self.notify = function() {
	    	return $http.post('/api/notify', {message: 'message from TT'}).success(function(data){
	    		console.log('notify results', data);
	    	}).error(function(err) {
	    		console.error('notify failed');
	    		displayErrorPage(err);
	    	});
	    };

	    self.getUsers = function() {
	    	return $http.get('/api/users').success(function(data){
	    		console.log('users', data);
	    	}).error(function(err){
	    		console.error('get users failed');
	    		displayErrorPage(err);
	    	});
	    }

	    self.getAll();
	}]);

	// $(document).ready(function(){
	// 	$('#testButton').click();
	// });

})();

