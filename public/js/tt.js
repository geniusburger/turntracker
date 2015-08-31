(function(){

	var app = angular.module('tt', []);

	app.controller('testCtrl', ['$http', function($http) {
		var self = this
	    self.turns = [];
	    self.users = [];
	    self.userMap = {};
	    self.listError = null;
	    self.statusError = null;
	    self.turnError = null;
	    self.worst = 0;
	    self.me = {id: -1, name: ''};
	    self.hoverId = 0;

	    var processTurnsData = function(data) {
    		if(data.error) {
    			self.listError = data.listError;
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
	    	return $http.get('/api/turns', {params:{id:1}}).success(processTurnsData).error(function(err){console.error('getTurns failed', err); throw err;});
	    };

	    self.getStatus = function() {
	    	return $http.get('/api/status', {params:{id:1}}).success(processStatusData).error(function(err){console.error('getStatus failed', err); throw err;});
	    };

	    self.getTurnsAndStatus = function() {
	    	return $http.get('/api/turns-status', {params:{id:1}})
	    		.success(function(data){
	    			processTurnsData(data);
	    			processStatusData(data);
	    		}).error(function(err){
	    			console.error('getTurnsAndStatus failed', err);
	    			throw err;
	    		});
	    };

	    self.takeTurn = function(userId) {
	    	return $http.post('/api/turn', {user_id: userId, task_id: 1}).success(function(data){
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

	    self.getTurnsAndStatus();
	}]);

	// $(document).ready(function(){
	// 	$('#testButton').click();
	// });

})();

