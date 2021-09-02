#!/bin/bash

EXECUTOR_ENDPOINT1=https://localhost:8743/executor/v1
EXECUTOR_ENDPOINT2=https://localhost:8843/executor/v1

MAX_PAGES=100

# use the test user pkcs12 cert
P12_KEYSTORE=../pki/testUser.p12
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
    -out ${TMP_PEM} -nodes
opensslexit=$?
umask $OLD_UMASK
[ $opensslexit = 0 ] || errormsg "Error creating temporary certificate file"

FOLDER="executor_$(date +%Y%m%d_%I%M%S.%3N)"

mkdir $FOLDER
cd $FOLDER

SYSTEM_FROM=$(hostname)

echo "$(date): polling connection factory for pool1"
curl -s -D headers_0.txt -k -E ${TMP_PEM} \
    -H "Accept: application/xml" \
    ${EXECUTOR_ENDPOINT1}/Common/AccumuloConnectionFactory/stats -o connectionFactory1Response.txt
echo "$(date): polling connection factory for pool2"
curl -s -D headers_0.txt -k -E ${TMP_PEM} \
    -H "Accept: application/xml" \
    ${EXECUTOR_ENDPOINT2}/Common/AccumuloConnectionFactory/stats -o connectionFactory2Response.txt

cd ../
