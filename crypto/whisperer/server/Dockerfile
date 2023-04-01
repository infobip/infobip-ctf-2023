FROM python:3-slim-bullseye
EXPOSE 8080

RUN apt-get update && apt-get upgrade -y
RUN apt-get install -y sqlite3
RUN pip3 install Flask gunicorn cryptography APScheduler requests
RUN groupadd -r ctf && useradd --no-log-init -r -g ctf ctf

RUN mkdir app && chown ctf:ctf app/
WORKDIR app
COPY static ./static
COPY templates ./templates
COPY app.py ./app.py
COPY whisperdb.sql ./whisperdb.sql
RUN sqlite3 whisper.db < whisperdb.sql
RUN chown ctf:ctf whisper.db

USER ctf
ENV FLASK_SECRET_KEY="e44a95fd3b82b420cdfd5c83be54e222"
ENV FLASK_SVC_PASSWD="BGtRl46socAY3hzm8N2VwDOd0L1FJyZM"
ENV FLASK_PASSWD_SALT="VQhDrslRmvM9GOayz7C38FIjxP4LW5Nd"
CMD ["gunicorn", "-w", "1", "-t", "4", "-b", "0.0.0.0:8080", "app:app"]
