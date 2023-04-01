FROM alpine:latest

RUN apk update && apk add --no-cache \
    mysql \
    mysql-client \
    supervisor \
    php \
    php-pdo_mysql
RUN rm -rf /var/cache/apk/*
RUN mysql_install_db --user=mysql --datadir=/var/lib/mysql --skip-test-db
COPY supervisord.conf /etc/supervisord.conf

WORKDIR /opt
COPY app ./app
RUN addgroup -g 1000 www
RUN adduser -G www -g www -s /bin/sh -D www

COPY myCDN.sql .
COPY start.sh .
RUN chmod +x start.sh
RUN chown -R www:www /opt
ENTRYPOINT ["/bin/sh", "/opt/start.sh"]
