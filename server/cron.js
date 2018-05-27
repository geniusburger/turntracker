// Schedule task in windows
// SCHTASKS /CREATE /SC DAILY /TN TURNTRACKER_CRON /TR "node C:\turntracker\server\cron.js" /ST 18:00

var http = require('http');

var req = http.request({
	hostname: 'localhost',
	port: 3000,
	path: '/api/remind',
	method: 'PUT',
	headers: {
		'Content-Length': 0,
	}
}, function(res) {
	res.setEncoding('utf8');
	console.log('response code: ' + res.statusCode);
	console.log('response headers', res.headers);
	res.on('data', function (chunk) {
		console.log(chunk);
	});
});

req.on('error', function(err){
	console.log('ERROR failed to put', err);
	reject(err);
});
req.end();
