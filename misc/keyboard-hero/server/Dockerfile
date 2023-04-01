FROM python:3-alpine
EXPOSE 9000

RUN apk add socat
RUN addgroup -S ctf && adduser -S ctf -G ctf
RUN mkdir hero/
WORKDIR hero/

COPY hero.py .

CMD ["/usr/bin/socat", "TCP-LISTEN:9000,reuseaddr,fork", "EXEC:\"python3 hero.py\",pty,rawer,stderr,echo=0,su=ctf"]
