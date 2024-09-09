FROM --platform=$TARGETOS/$TARGETARCH eclipse-temurin:22-jre-alpine

WORKDIR /usr/app
COPY build/install/openid-helper .

ENTRYPOINT ["/usr/app/bin/openid-helper"]