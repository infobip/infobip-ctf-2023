# Build

```sh
apt install -y libseccomp-dev
make
docker build -t glossary .
docker run -d -p 9000:9000 -it glossary
nc localhost 9000
```
