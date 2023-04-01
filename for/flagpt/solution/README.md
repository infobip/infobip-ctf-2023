# Solution

We extract the 7zip file and consequently tar archive:
```
$ 7z x file.7z

7-Zip [64] 16.02 : Copyright (c) 1999-2016 Igor Pavlov : 2016-05-21
p7zip Version 16.02 (locale=en_US.UTF-8,Utf16=on,HugeFiles=on,64 bits,4 CPUs Intel(R) Core(TM) i5-8265U CPU @ 1.60GHz (806EC),ASM,AES-NI)

Scanning the drive for archives:
1 file, 36024338 bytes (35 MiB)

Extracting archive: file.7z
--
Path = file.7z
Type = 7z
Physical Size = 36024338
Headers Size = 171
Method = LZMA2:26
Solid = +
Blocks = 1

Everything is Ok

Files: 2
Size:       120953728
Compressed: 36024338
$ file dmp*
dmp1: POSIX tar archive
dmp2: pcapng capture file - version 1.0
$ tar xvf dmp1
399bd52ce2dd2f44a8178794804114900e1427d2c2d8dd8dd5f16ff838ddd205/
399bd52ce2dd2f44a8178794804114900e1427d2c2d8dd8dd5f16ff838ddd205/VERSION
399bd52ce2dd2f44a8178794804114900e1427d2c2d8dd8dd5f16ff838ddd205/json
399bd52ce2dd2f44a8178794804114900e1427d2c2d8dd8dd5f16ff838ddd205/layer.tar
59ee9261afaa742223f8e524d535164665906afee0482b5e61211c5de3d948ed/
59ee9261afaa742223f8e524d535164665906afee0482b5e61211c5de3d948ed/VERSION
59ee9261afaa742223f8e524d535164665906afee0482b5e61211c5de3d948ed/json
59ee9261afaa742223f8e524d535164665906afee0482b5e61211c5de3d948ed/layer.tar
79ed4e0a33bbfd4a4be581a076a0bb226fdd5d16bc8940fc780c8681f4ad0a7d/
79ed4e0a33bbfd4a4be581a076a0bb226fdd5d16bc8940fc780c8681f4ad0a7d/VERSION
79ed4e0a33bbfd4a4be581a076a0bb226fdd5d16bc8940fc780c8681f4ad0a7d/json
79ed4e0a33bbfd4a4be581a076a0bb226fdd5d16bc8940fc780c8681f4ad0a7d/layer.tar
a3e6831dcc051bac15a13f9da00a647b85b054c7d620dc4b5c6dff5e6d505910/
a3e6831dcc051bac15a13f9da00a647b85b054c7d620dc4b5c6dff5e6d505910/VERSION
a3e6831dcc051bac15a13f9da00a647b85b054c7d620dc4b5c6dff5e6d505910/json
a3e6831dcc051bac15a13f9da00a647b85b054c7d620dc4b5c6dff5e6d505910/layer.tar
bfce347c125c4c1cdefc0351ab09069c65e14e91462b7b3e06bc1d578dd7b185.json
cdb570617f9225e0f45068cfaa95cf701bab6455105a2b387957b3ceef2139f4/
cdb570617f9225e0f45068cfaa95cf701bab6455105a2b387957b3ceef2139f4/VERSION
cdb570617f9225e0f45068cfaa95cf701bab6455105a2b387957b3ceef2139f4/json
cdb570617f9225e0f45068cfaa95cf701bab6455105a2b387957b3ceef2139f4/layer.tar
dd8165684bdc6bc0387154f31ee05bc285e991b63b8d20715cdc2ae0dc41d0c6/
dd8165684bdc6bc0387154f31ee05bc285e991b63b8d20715cdc2ae0dc41d0c6/VERSION
dd8165684bdc6bc0387154f31ee05bc285e991b63b8d20715cdc2ae0dc41d0c6/json
dd8165684bdc6bc0387154f31ee05bc285e991b63b8d20715cdc2ae0dc41d0c6/layer.tar
manifest.json
repositories
```

A trained eye will almost certainly detect Docker layers. In order to inspect Docker container we can just load the tar file with `docker load`:
```
$ sudo docker load < dmp1
2bf2410e14f7: Loading layer   53.9MB/53.9MB
decd7344974f: Loading layer  2.048kB/2.048kB
ad3a83b99179: Loading layer  3.072kB/3.072kB
6c4ed0e65551: Loading layer  3.072kB/3.072kB
cf702d849f37: Loading layer  2.048kB/2.048kB
Loaded image: flagpt:latest
```

There is nothing in the container that would be considered of value, so one has to inspect other layers. For that there is a good tool called dive which you can find on Github: https://github.com/wagoodman/dive. When using dive these should be the noticed changes:
- a3e6831dcc051bac15a13f9da00a647b85b054c7d620dc4b5c6dff5e6d505910 is the base layer
- cdb570617f9225e0f45068cfaa95cf701bab6455105a2b387957b3ceef2139f4 adds python3
- 79ed4e0a33bbfd4a4be581a076a0bb226fdd5d16bc8940fc780c8681f4ad0a7d creates directory /opt/app
- 59ee9261afaa742223f8e524d535164665906afee0482b5e61211c5de3d948ed copies flag.txt
- 399bd52ce2dd2f44a8178794804114900e1427d2c2d8dd8dd5f16ff838ddd205 copies flagpt.py
- dd8165684bdc6bc0387154f31ee05bc285e991b63b8d20715cdc2ae0dc41d0c6 deletes directory /opt/app

We can extract the files directly from the layer .tar files:
```
$ tar xvf 59ee9261afaa742223f8e524d535164665906afee0482b5e61211c5de3d948ed/layer.tar 
opt/
opt/app/
opt/app/flag.txt
$ tar xvf 399bd52ce2dd2f44a8178794804114900e1427d2c2d8dd8dd5f16ff838ddd205/layer.tar 
opt/
opt/app/
opt/app/flagpt.py
$ xxd opt/app/flag.txt 
00000000: 5851 5743 5f4a 3039 4607 5e00 5801 490e  XQWC_J09F.^.X.I.
00000010: 006d 0657 1a01 1e55 1446 6c55 1908 0d57  .m.W...U.FlU...W
00000020: 5115 5e0b 0015 3e07 4616 1910 013e 465b  Q.^...>.F....>F[
00000030: 550d 0611 3c15 561c 1f54 1045            U...<.V..T.E
$ cat opt/app/flagpt.py 
import socket
import os

for file in os.listdir(os.curdir):
    if file == "flag.txt":
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect(("172.17.0.1", 4444))
        key = s.recv(60)
        s.close()

        with open(file, "rb") as fp:
            flag = fp.read()

        print(bytes([flag[i]^key[i] for i in range(len(flag))]))
```

It is obvious that `flag.txt` is somehow encrypted and that the script decrypts `flag.txt` content by receiving the key via network socket. The key that we need is stored in the .pcap file extracted from the 7zip:
```
$ tcpdump -r dmp2 -A host 172.17.0.2 and port 4444
reading from file dmp2, link-type LINUX_SLL (Linux cooked v1), snapshot length 262144
09:42:52.106002 IP 172.17.0.2.52014 > vm.4444: Flags [S], seq 127596930, win 64240, options [mss 1460,sackOK,TS val 101771133 ecr 0,nop,wscale 7], length 0
E..<.l@.@..*...........\............XT.........
...}........
09:42:52.106002 IP 172.17.0.2.52014 > vm.4444: Flags [S], seq 127596930, win 64240, options [mss 1460,sackOK,TS val 101771133 ecr 0,nop,wscale 7], length 0
E..<.l@.@..*...........\............XT.........
...}........
09:42:52.106041 IP vm.4444 > 172.17.0.2.52014: Flags [S.], seq 2467010490, ack 127596931, win 65160, options [mss 1460,sackOK,TS val 2143860322 ecr 101771133,nop,wscale 7], length 0
E..<..@.@............\..............XT.........
...b...}....
09:42:52.106043 IP vm.4444 > 172.17.0.2.52014: Flags [S.], seq 2467010490, ack 127596931, win 65160, options [mss 1460,sackOK,TS val 2143860322 ecr 101771133,nop,wscale 7], length 0
E..<..@.@............\..............XT.........
...b...}....
09:42:52.106060 IP 172.17.0.2.52014 > vm.4444: Flags [.], ack 1, win 502, options [nop,nop,TS val 101771133 ecr 2143860322], length 0
E..4.m@.@..1...........\............XL.....
...}...b
09:42:52.106060 IP 172.17.0.2.52014 > vm.4444: Flags [.], ack 1, win 502, options [nop,nop,TS val 101771133 ecr 2143860322], length 0
E..4.m@.@..1...........\............XL.....
...}...b
09:42:52.106210 IP vm.4444 > 172.17.0.2.52014: Flags [P.], seq 1:66, ack 1, win 510, options [nop,nop,TS val 2143860322 ecr 101771133], length 65
E..u./@.@............\..............X......
...b...}134791ef44316edc32617532d23d48c4b85e3ba62e4c1a35397ccaf1fde86804

09:42:52.106214 IP vm.4444 > 172.17.0.2.52014: Flags [P.], seq 1:66, ack 1, win 510, options [nop,nop,TS val 2143860322 ecr 101771133], length 65
E..u./@.@............\..............X......
...b...}134791ef44316edc32617532d23d48c4b85e3ba62e4c1a35397ccaf1fde86804

09:42:52.106243 IP 172.17.0.2.52014 > vm.4444: Flags [.], ack 66, win 502, options [nop,nop,TS val 101771133 ecr 2143860322], length 0
E..4.n@.@..0...........\............XL.....
...}...b
09:42:52.106243 IP 172.17.0.2.52014 > vm.4444: Flags [.], ack 66, win 502, options [nop,nop,TS val 101771133 ecr 2143860322], length 0
E..4.n@.@..0...........\............XL.....
...}...b
09:42:52.106308 IP 172.17.0.2.52014 > vm.4444: Flags [R.], seq 1, ack 66, win 502, options [nop,nop,TS val 101771133 ecr 2143860322], length 0
E..4.o@.@../...........\............XL.....
...}...b
09:42:52.106308 IP 172.17.0.2.52014 > vm.4444: Flags [R.], seq 1, ack 66, win 502, options [nop,nop,TS val 101771133 ecr 2143860322], length 0
E..4.o@.@../...........\............XL.....
...}...b
```

When we know the decryption key, we can decrypt the flag.txt file with simple python:
```
>>> FLAGPT_KEY="134791ef44316edc32617532d23d48c4b85e3ba62e4c1a35397ccaf1fde86804"
>>> with open("opt/app/flag.txt", "rb") as fp:
...     flag = fp.read()
... 
>>> KEY=FLAGPT_KEY.encode()
>>> bytes([flag[i]^KEY[i] for i in range(len(flag))])
b'ibctf{U_r3m1nd-m3_0f-4-gpt_1-0nc3-kn3w_1ts-s0_unf41r_t0-y0u}'
```
