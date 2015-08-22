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

var db = mysql.createConnection(config.database.test);

db.connect(function(err){
	if(!err) {
		console.log("Database is connected ... \n\n");  
	} else {
		console.log("Error connecting database ... \n\n");  
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

app.put('/api/list', function(req,res) {
    res.setHeader('Content-Type', 'application/json');
    db.query(
    	'SELECT users.displayname AS name, turns.inserted AS date, users.id ' +
    	'FROM tasks INNER JOIN turns on turns.task_id = tasks.id INNER JOIN USERS ON turns.user_id = users.id ' +
		'WHERE tasks.id = ? ORDER BY turns.inserted DESC', req.body.id, function(err, rows, fields) {
		if (err) {
			console.log('Error while performing list query');
			res.send({error: 'Query Error'});
		} else {
			res.send({list: rows});
		}
	});
});

app.put('/api/status', function(req,res) {
    res.setHeader('Content-Type', 'application/json');
    db.query(
    	'SELECT users.id AS id, users.displayname AS name, COUNT(turns.user_id) AS turns ' +
		'FROM turns ' +
		'RIGHT JOIN participants ON participants.task_id = turns.task_id and participants.user_id = turns.user_id ' +
		'INNER JOIN users ON participants.user_id = users.id ' +
		'WHERE participants.task_id = ? GROUP BY turns.user_id', req.body.id, function(err, rows, fields) {
		if (err) {
			console.log('Error while performing status query');
			res.send({error: 'Query Error'});
		} else {
			res.send({ users: rows });
		}
    });
});

app.put('/api/turn', function(req,res) {
    res.setHeader('Content-Type', 'application/json');
    db.query(
    	'INSERT INTO turns SET ?', req.body, function(err, rows, fields) {
		if (err) {
			console.log('Error while performing turn query');
			res.send({error: 'Query Error'});
		} else {
			res.send({error: false});
		}
    });
});

var server = app.listen(PORT, function() {
	var host = server.address().address;
	var port = server.address().port;
	console.log('TurnTracker listening at http://%s:%s', host, port);
});