var extend = require('util')._extend;
var config = require('./config.js');
var express = require('express');
var mysql = require('mysql');
var minimist = require('minimist');
var bodyParser = require('body-parser');

var options = minimist(process.argv.slice(2), {
  boolean: 'live',
  default: { live: false }
});

const PORT = 3000;

var db = mysql.createConnection( extend( config.database.test, {} ) );

db.connect(function(err){
	if(!err) {
		console.log("Database is connected ... \n\n");  
	} else {
		console.error("Error connecting database ... \n\n");  
	}
});

var app = express();

if(options.live)
{
	app.use(require('connect-livereload')({
		port: 35729
	}));
}

app.use(express.static('build'));
app.use(bodyParser.json());       // to support JSON-encoded bodies
//app.use(bodyParser.urlencoded({ extended: true }));	// to support URL-encoded bodies

var getTurns = function(taskId) {
	return new Promise(function(resolve, reject){
		db.query(
	    	'SELECT users.displayname AS name, turns.inserted AS date, users.id ' +
	    	'FROM tasks INNER JOIN turns on turns.task_id = tasks.id INNER JOIN USERS ON turns.user_id = users.id ' +
			'WHERE tasks.id = ? ORDER BY turns.inserted DESC',
			taskId, function(err, rows, fields){
				if(err) {
					reject(err);
				} else {
					resolve(rows);
				}
			});
	});
};

var getTasks = function(userId) {
	return new Promise(function(resolve, reject){
		db.query(
			'SELECT tasks.id,  tasks.name, tasks.periodic_hours, tasks.creator_user_id ' +
			'FROM participants JOIN tasks ON participants.task_id = tasks.id ' +
			'WHERE participants.user_id = ?', 
			userId, function(err, rows, fields){
				if(err) {
					reject(err);
				} else {
					resolve(rows);
				}
			});
	});
};

var getStatus = function(taskId, callback) {
	return new Promise(function(resolve, reject){
		db.query(
			'SELECT users.id AS id, users.displayname AS name, IFNULL(counts.turns, 0) AS turns ' + 
			'FROM participants JOIN users on participants.user_id = users.id LEFT JOIN ( ' +
				'SELECT turns.user_id, count(*) as turns, turns.inserted ' +
				'FROM turns WHERE turns.task_id = ? ' +
				'GROUP BY turns.user_id ORDER BY turns.inserted ASC ' +
			') counts on participants.user_id = counts.user_id ' +
			'WHERE  participants.task_id = ? ' +
			'ORDER by turns ASC, counts.inserted ASC',
			[taskId,taskId], function(err, rows, fields){
				if(err) {
					reject(err);
				} else {
					resolve(rows);
				}
			});
	});
};

var saveAddress = function(userId, ip, callback) {
	return new Promise(function(resolve, reject){
	    db.query( 'INSERT INTO addresses SET ? ON DUPLICATE KEY UPDATE user_id = ?',
	    	[{user_id: userId, ip: ip}, userId], function(err, rows, fields) {
			if (err) {
				if(err.code === 'ER_DUP_ENTRY') {
					console.log('Already saved user %d address %s', userId, ip);
					resolve(userId);
				} else {
					console.error('Error while performing addresses query', err);
					reject(err);
				}
			} else {
				console.log('Saved user %d address %s', userId, ip);
				resolve(userId);
			}
		})
	});
};

var getUser = function(ip, callback) {
	return new Promise(function(resolve, reject){
		db.query('SELECT users.id, users.displayname as name FROM addresses JOIN users ON users.id = addresses.user_id WHERE ip = ?',
			ip, function(err, rows, fields) {
				if(err) {
					console.error('Error while getting address', err);
					reject(err);
				} else {
					if(rows[0]) {
						console.log('Got user from address %s', ip, rows[0]);
						resolve(rows[0]);
					} else {
						console.warn("Didn't find user for address %s", ip);
						reject();
					}
				}
		});
	});
};

var takeTurn = function(taskId, userid) {
	return new Promise(function(resolve, reject){
	    db.query( 'INSERT INTO turns SET ?', {user_id: userid, task_id: taskId}, function(err, rows, fields) {
			if (err) {
				console.error('Error while performing turn query', err);
				reject(err);
			} else {
				resolve();
			}
	    });
	});
};

app.get('/api/tasks-turns-status', function(req,res){
	var results = {};
	getTasks(req.query.userid).then(function(tasks){
		results.tasks = tasks;
		if(req.query.taskid) {
			for(var i = 0; i < tasks.length; i++) {
				if(tasks[i].id === req.query.taskid) {
					results.taskid = parseInt(req.query.taskid);
					return Promise.all([getTurns(req.query.taskid), getStatus(req.query.taskid)]);
				}
			}
			log.warn('tasks-turns-status invalid task id', req.query.taskid);
		}
		if(tasks.length) {
			results.taskid = tasks[0].id;
			return Promise.all([getTurns(tasks[0].id), getStatus(tasks[0].id)]);
		} else {
			results.taskId = 0;
			return [[],[]];
		}
	}).then(function(data){
		results.turns = data[0];
		results.users = data[1];
		res.json(results);
	}).catch(function(err){
		console.error('tasks-turns-status error', err);
		res.json({error: 'query error'});
	});
});

app.get('/api/tasks', function(req,res){
	getTasks(req.query.userid).then(function(tasks){
		res.json({tasks: tasks});
	}).catch(function(err){
		console.error('tasks error', err);
		res.json({error: 'tasks error'});
	});
});

app.get('/api/turns', function(req,res) {
	var userPromise = getUser(req.ip);
	var turnsPromise = userPromise.then(function(){
		return getTurns(req.query.id);
	});

	Promise.all([userPromise, turnsPromise]).then(function(results){
		res.json({user: results[0], turns: results[1]});
	}).catch(function(err) {
		console.error('turns error', err);
		res.json({user: {id: -1, name: ''}, error: 'query error'});
	});
});

app.get('/api/status', function(req,res) {
	getStatus(req.query.id).then(function(rows){
		res.json({users: rows});
	}).catch(function(err){
		console.error('Error while performing status query', err);
		res.json({error: 'Query Error'});
	});
});

app.get('/api/turns-status', function(req,res) {
	var userPromise = getUser(req.ip);
	var listPromise = userPromise.then(function(){
		return getTurns(req.query.id);
	});
	Promise.all([userPromise, listPromise, getStatus(req.query.id)]).then(function(results){
		res.json({user: results[0], turns: results[1], users: results[2], taskid: parseInt(req.query.id)});
	}).catch(function(err){
		console.error('turns-status error', err);
		res.json({error: 'query error'});
	});
});

app.post('/api/turn', function(req,res) {
	(req.body.user_id ? saveAddress(req.body.user_id, req.ip) : getUser(req.ip)).then(function(user){
		return takeTurn(req.body.task_id, typeof user === 'object' ? user.id : user);
	}).then(function(){
		return Promise.all([getTurns(req.body.task_id), getStatus(req.body.task_id)]);
	}, function(){
		console.error('failed to take turn', err);
		return Promise.all([getTurns(req.body.task_id), getStatus(req.body.task_id)]);
	}).then(function(results){
		res.json({turns: results[0], users: results[1]});
	}).catch(function(err){
		console.error('turn error', err);
		res.json({error: 'query error'});
	});
});

var server = app.listen(PORT, function() {
	var host = server.address().address;
	var port = server.address().port;
	console.log('TurnTracker listening at http://%s:%s', host, port);
});