FROM jboss/keycloak

EXPOSE 8080
EXPOSE 8443

COPY template-realm.json /tmp/template-realm.json
COPY tls.crt /etc/x509/https/tls.crt
COPY tls.key /etc/x509/https/tls.key

ENV DB_VENDOR=h2
ENV KEYCLOAK_USER=admin
ENV KEYCLOAK_PASSWORD=admin
ENV KEYCLOAK_IMPORT=/tmp/template-realm.json
