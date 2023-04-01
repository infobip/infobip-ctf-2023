# Build

```
apt install maven openjdk-17-jdk -y
cd liberty
mvn package -Dpackaging=jar
mv target/liberty-*.jar ../liberty.jar
cd ..
docker build -t liberty .
docker run -d -p 8080:8080 -it liberty
```
