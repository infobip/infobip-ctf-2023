FROM ubuntu:22.04
EXPOSE 9000

RUN apt-get update && apt-get install -y socat
RUN groupadd -r ctf && useradd --no-log-init -r -g ctf ctf
RUN mkdir chall/
WORKDIR chall/

ENV FLAG="ibctf{0ne_c4nn0t_b3-a-h4ck3r-1f_1t-iZ-H0L3_1n-1}"
COPY glossary .
RUN chmod +x ./glossary
CMD ["/usr/bin/socat", "TCP-LISTEN:9000,reuseaddr,fork", "EXEC:\"./glossary\",pty,rawer,stderr,echo=0,su=ctf"]
