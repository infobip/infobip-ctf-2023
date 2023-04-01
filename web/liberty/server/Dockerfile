FROM openjdk:17

RUN groupadd -r ctf && useradd --no-log-init -r -g ctf ctf
RUN mkdir web/ && chown ctf:ctf web/
WORKDIR web

COPY liberty.jar ./liberty.jar
COPY flag.txt ./flag.txt

USER ctf
CMD ["java", "-jar", "liberty.jar"]
