FROM golang:1.19-alpine

EXPOSE 8000

RUN apk add gcc musl-dev
RUN addgroup -S ctf && adduser -s /bin/sh -S ctf -G ctf

WORKDIR /app

COPY go.mod .
COPY go.sum .
COPY index.html .
COPY main.go .
COPY flag.txt .

USER ctf
CMD ["go", "run", "main.go"]
