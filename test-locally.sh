#!/bin/bash

set -euxo pipefail
set -m # Sets JOB CONTROL mode, needed for fg

# Rebuild
mvn clean install -DskipTests

# Set Cloudgene up
cd target/cloudgene-*-statgen.*/

./cloudgene install MarcFraile/dummy-cloudgene-app

# Start Cloudgene as a background process (so we can do more stuff)
./cloudgene server &

# Wait for Cloudgene to be up and running, and submit a 'fetchngs' job with user 'foobar'
sleep 2
FOOBAR_TOKEN=$(curl -d 'username=foobar' --data-urlencode 'password=foobarBAZ+42!' 'http://localhost:8082/login' | jq -r '.access_token')
curl -H "X-Auth-Token: ${FOOBAR_TOKEN}" -X POST 'http://localhost:8082/api/v2/jobs/submit/dummy-cloudgene-app'

# Set Cloudgene as the foreground process again (so it dies when we kill the script)
fg %1
