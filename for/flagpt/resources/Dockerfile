FROM alpine:3.17

RUN apk add python3
RUN mkdir -p /opt/app
WORKDIR /opt/app
COPY flag.txt .
COPY flagpt.py .
RUN python3 ./flagpt.py
WORKDIR /
RUN rm -rf /opt/app
