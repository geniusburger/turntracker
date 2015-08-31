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
	    		if(data.tasks.length) {
	    			self.task = self.tasks[0];
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

	    		 // descending by number of diff turns, tie-breaker is ascending most recent turn
				data.users.sort(function(a,b) {
					var diff = b.diff - a.diff;
					if(diff === 0) {
						for(var i = 0; i < self.turns.length; i++) {
							if(self.turns[i].id == a.id) {
								return 1;
							}
							if(self.turns[i].id == b.id) {
								return -1;
							}
						}
					}
					return diff;
				});
				self.worst = data.users[0].diff;
	    		self.users = data.users;
	    		self.statusError = null;
	    	} else {
	    		self.statusError = "Unknown status error";
	    		self.users = [];
	    	}
    	};

	    self.getTurns = function() {
	    	return $http.get('/api/turns', {params:{id:self.task.id}}).success(processTurnsData).error(function(err){console.error('getTurns failed', err); throw err;});
	    };

	    self.getStatus = function() {
	    	return $http.get('/api/status', {params:{id:self.task.id}}).success(processStatusData).error(function(err){console.error('getStatus failed', err); throw err;});
	    };

	    self.getTurnsAndStatus = function() {
	    	return $http.get('/api/turns-status', {params:{id:self.task.id}})
	    		.success(function(data){
	    			processTurnsData(data);
	    			processStatusData(data);
	    		}).error(function(err){
	    			console.error('getTurnsAndStatus failed', err);
	    			throw err;
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
	    			throw err;
	    		});
	    };

	    self.getTasks = function() {
	    	return $http.get('/api/tasks', {params:{userid:self.me.id}})
	    		.success(function(data){
	    			processTasksData(data);
	    		}).error(function(err){
	    			console.error('getTasks failed', err);
	    			throw err;
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
    			throw err;
    		});
	    };

	    self.getAll();
	}]);

	// $(document).ready(function(){
	// 	$('#testButton').click();
	// });

})();

