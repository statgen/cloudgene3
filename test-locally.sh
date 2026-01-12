#!/bin/bash

set -euxo pipefail
set -m # Sets JOB CONTROL mode, needed for fg

# Rebuild front-end
(cd src/main/html/webapp && rm -rf dist && npm run build)

# Rebuild bundle
rm -rf target && mvn install -DskipTests

# Set Cloudgene up
cd target/cloudgene-*-statgen.*/

./cloudgene install lukfor/cg-fetchngs
# ./cloudgene install statgen/imputationserver2

# Start Cloudgene as a background process (so we can do more stuff)
./cloudgene server &

# Wait for Cloudgene to be up and running, and submit a 'fetchngs' job with user 'foobar'
sleep 2
FOOBAR_TOKEN=$(curl -d 'username=foobar' --data-urlencode 'password=foobarBAZ+42!' 'http://localhost:8082/login' | jq -r '.access_token')
curl -H "X-Auth-Token: ${FOOBAR_TOKEN}" -F 'input=SRR12696236' 'http://localhost:8082/api/v2/jobs/submit/fetchngs'

# Set Cloudgene as the foreground process again (so it dies when we kill the script)
fg %1
