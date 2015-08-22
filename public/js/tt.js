(function(){

	var app = angular.module('tt', []);

	app.controller('testCtrl', ['$http', function($http) {
		var self = this
	    self.list = [];
	    self.users = [];
	    self.listError = null;
	    self.statusError = null;
	    self.turnError = null;
	    self.worst = 0;

	    self.getList = function() {
	    	$http.put('/api/list', {id:1}).success(function(data) {
	    		if(data.error) {
	    			self.listError = data.listError;
	    			self.list = [];
	    		} else if(data.list) {
	    			data.list.forEach(function(turn){
	    				turn.date = new Date(turn.date).toLocaleString();
	    			});
	    			self.list = data.list;
	    			self.listError = null;
	    			self.getStatus();
	    		} else {
	    			self.listError = 'Unknown list error';
	    			self.list = [];
	    		}
	    	});
	    };

	    self.getStatus = function() {
	    	$http.put('/api/status', {id:1}).success(function(data) {
	    		if(data.error) {
		    		self.statusError = data.error;
		    		self.users = [];
		    	} else if(data.users) {
		    		var max = data.users.reduce(function(max, user){ 
		    			return Math.max(max, user.turns);
		    		}, 0);
		    		data.users.forEach(function(user){
		    			user.diff = max - user.turns;
		    		});

		    		 // descending by number of diff turns, tie-breaker is ascending most recent turn
					data.users.sort(function(a,b) {
						var diff = b.diff - a.diff;
						if(diff === 0) {
							for(var i = 0; i < self.list.length; i++) {
								if(self.list[i].id == a.id) {
									return 1;
								}
								if(self.list[i].id == b.id) {
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
	    	});
	    };

	    self.takeTurn = function(userId) {
	    	$http.put('/api/turn', {user_id: userId, task_id: 1}).success(function(data){
	    		if(data.error) {
	    			self.turnError = data.error;
	    		} else {
	    			self.turnError = null;
	    			self.getList();
	    		}
	    	});
	    };

	    self.getList();
	}]);

	// $(document).ready(function(){
	// 	$('#testButton').click();
	// });

})();

