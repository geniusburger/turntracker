// Schedule task in windows
// SCHTASKS /CREATE /SC DAILY /TN TURNTRACKER_CRON /TR "node C:\turntracker\server\cron.js" /ST 18:00

var Curl = require('node-libcurl').Curl;
var curl = new Curl();

curl.setOpt('URL', 'http://localhost:3000/api/remind');
curl.setOpt('PUT', 1);

curl.on('end', function( statusCode, body, headers ) {
	console.info(statusCode);
	this.close();
});
 
curl.on('error', curl.close.bind(curl));
curl.perform();
