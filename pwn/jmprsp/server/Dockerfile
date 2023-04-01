FROM ubuntu:22.04
EXPOSE 9000

RUN apt-get update -y && apt-get install -y \
    lib32z1 \
    xinetd \
    && rm -rf /var/lib/apt/lists/*

RUN useradd -m ctf
WORKDIR /ctf

RUN echo "Connection blocked" > /etc/banner_fail
COPY ctf.xinetd /etc/xinetd.d/ctf
COPY flag.txt .
COPY jmprsp .
COPY start.sh .

RUN chmod -R 755 /ctf

CMD ["/usr/sbin/xinetd", "-dontfork"]
