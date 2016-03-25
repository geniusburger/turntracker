var Curl = require('node-libcurl').Curl;
var curl = new Curl();

curl.setOpt('URL', 'http://localhost:3000/api/remind');
curl.setOpt('CURLOPT_PUT', 1);

curl.on('end', function( statusCode, body, headers ) {
	console.info(statusCode);
	console.info('---');
	console.info(body.length);
	console.info('---'); 
	this.close();
});
 
curl.on('error', curl.close.bind(curl));
curl.perform();
