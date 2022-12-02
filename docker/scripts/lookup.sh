#!/bin/bash

DATAWAVE_ENDPOINT=https://localhost:8443/query/v1/query
METRICS_ENDPOINT=https://localhost:8543/querymetric/v1

PAUSE='false'

POOL="${POOL:-pool1}"

MAX_PAGES=100

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# use the test user pkcs12 cert
P12_KEYSTORE=${SCRIPT_DIR}/../pki/testUser.p12
P12_KEYSTORE_PASS=ChangeIt

TMP_DIR=/dev/shm
TMP_PEM="$TMP_DIR/testUser-$$-pem"

sh -c "while kill -0 $$ 2>/dev/null; do sleep 1; done; rm -f '${TMP_P12}' '${TMP_PEM}'" &

function needsPassphrase() {
    [ -z "${P12_KEYSTORE_PASS}" ]
}

function getFromCliPrompt() {
    read -s -p "Passphrase for ${P12_KEYSTORE}: " P12_KEYSTORE_PASS && echo 1>&2
}

needsPassphrase && getFromCliPrompt

# Create one-time passphrase and certificate
OLD_UMASK=$(umask)
umask 0277
export P12_KEYSTORE_PASS
openssl pkcs12 \
    -in ${P12_KEYSTORE} -passin env:P12_KEYSTORE_PASS \
    -out ${TMP_PEM} -nodes 2>/dev/null
opensslexit=$?
umask $OLD_UMASK
[ $opensslexit = 0 ] || errormsg "Error creating temporary certificate file"

read_dom () {
    local IFS=\>
    read -d \< ENTITY CONTENT
}

get_query_id () {
    while read_dom; do
        if [[ $ENTITY =~ 'QueryId' ]]; then
            echo $CONTENT
            break
        fi
    done
}

get_num_events () {
    while read_dom; do
        if [[ $ENTITY = 'ReturnedEvents' ]]; then
            echo $CONTENT
            break
        fi
    done
}

FOLDER="lookup_$(date +%Y%m%d_%I%M%S.%N)"

mkdir $FOLDER
cd $FOLDER

SYSTEM_FROM=$(hostname)

echo "$(date): Running LookupUUID query"
echo "$(date): Running LookupUUID query" > querySummary.txt
curl -s -D headers_0.txt -X GET -k -E ${TMP_PEM} \
    -H "Accept: application/xml" \
    --data-urlencode "begin=19660908 000000.000" \
    --data-urlencode "end=20161002 235959.999" \
    --data-urlencode "columnVisibility=PUBLIC" \
    --data-urlencode "auths=PUBLIC,PRIVATE,BAR,FOO" \
    --data-urlencode "systemFrom=$SYSTEM_FROM" \
    --data-urlencode "queryName=Developer Test Lookup UUID Query" \
    --data-urlencode "pool=$POOL" \
    ${DATAWAVE_ENDPOINT}/lookupUUID/PAGE_TITLE/anarchism -o lookupResponse.xml -w '%{http_code}\n' >> querySummary.txt

QUERY_ID=$(get_query_id < lookupResponse.xml)
NUM_EVENTS=$(get_num_events < lookupResponse.xml)
echo "$(date): Returned $NUM_EVENTS events"
echo "$(date): Returned $NUM_EVENTS events" >> querySummary.txt

echo "$(date): Finished running $QUERY_ID"
echo "$(date): Finished running $QUERY_ID" >> querySummary.txt

cd ../

if [ ! -z "$QUERY_ID" ]; then
    mv $FOLDER lookup_$QUERY_ID

    echo "$(date): Getting metrics for $QUERY_ID"
    echo "$(date): Getting metrics for $QUERY_ID" >> lookup_$QUERY_ID/querySummary.txt

    echo "$(date): Metrics available at: ${METRICS_ENDPOINT}/id/$QUERY_ID"
    echo "$(date): Metrics available at: ${METRICS_ENDPOINT}/id/$QUERY_ID" >> lookup_$QUERY_ID/querySummary.txt
fi
