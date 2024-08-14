FROM alpine

WORKDIR /usr/app
COPY build/native/nativeCompile/openid-helper openid-helper

LABEL org.opencontainers.image.source="https://github.com/DRSchlaubi/openid-helper"

ENTRYPOINT ["/usr/app/openid-helper"]
