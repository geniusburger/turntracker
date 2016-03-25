var curl = require('node-curl');

curl.setopt('CURLOPT_PUT', 1);
curl('http://localhost:3000/api/remind', function(e){
    console.log(e);
    console.log(this.body);
});
