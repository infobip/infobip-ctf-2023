# Build

```
echo "134791ef44316edc32617532d23d48c4b85e3ba62e4c1a35397ccaf1fde86804" | nc -nlvp 4444
# Ctrl + Z
docker build -t flagpt .
# Ctrl + Z
fg 1
# Ctrl + C
fg 2
docker save flagpt -o dmp
7z a file.7z dmp -mx9
```
