#!/bin/bash

rm -f application.keystore template.p12 truststore.p12 tls.crt tls.key
keytool -genkeypair -dname CN=localhost -alias template -keyalg rsa -keysize 2048 -storetype pkcs12 -keystore template.p12 -validity 3650 -storepass changeit
keytool -importkeystore -srckeystore template.p12 -srcalias template -srcstorepass changeit -destkeystore application.keystore -deststoretype pkcs12 -deststorepass password -destalias server
keytool -export -alias template -keystore template.p12 -storepass changeit -rfc -file tls.crt
openssl pkcs12 -in template.p12 -nodes -nocerts -passin pass:changeit | openssl rsa -out tls.key
keytool -importcert -keystore truststore.p12 -storetype pkcs12 -alias template -storepass changeit -file tls.crt -noprompt
rm -f ../../auth-service/tls.crt ../../auth-service/tls.key
mv tls.crt tls.key ../../auth-service
cp template.p12 truststore.p12 ../../account-service/src/main/resources/keystore/
cp template.p12 truststore.p12 ../../api-service/src/main/resources/keystore/
cp template.p12 truststore.p12 ../../discovery-service/src/main/resources/keystore/
cp template.p12 truststore.p12 ../../gateway-service/src/main/resources/keystore/
