# Solution

We observe the NTFS file attached to the challenge:
```
$ file pingu
pingu: DOS/MBR boot sector, code offset 0x52+2, OEM-ID "NTFS    ", sectors/cluster 8, Media descriptor 0xf8, sectors/track 0, dos < 4.0 BootSector (0x80), FAT (1Y bit by descriptor); NTFS, sectors 6143, $MFT start cluster 4, $MFTMirror start cluster 383, bytes/RecordSegment 2^(-1*246), clusters/index block 1, serial number 0792f01bb36b84f3a
```

Testdisk tool can list available files:
```
Directory /

>dr-xr-xr-x     0     0         0 12-Jan-2023 23:25 .
 dr-xr-xr-x     0     0         0 12-Jan-2023 23:25 ..
 -r--r--r--     0     0       729 12-Jan-2023 23:25 pingu.py
```

And deleted files:
```
Deleted files

>./pic1.ppm                                                            12-Jan-2023 23:25       38411
 ./pic2.ppm                                                            12-Jan-2023 23:25      921615
```

After extracting all of the files with testdisk:
```
$ ls
pic1.ppm  pic2.ppm  pingu  pingu.py
```

One has to observe that `pingu.py` performs a secure delete operation with same XOR key (multi-time pad in a way):
```
#!/usr/bin/python3

import os
import sys

HEADER_SIZE = 16

def enc(a, b):
    return [a[i]^b[i] for i in range(min(len(a), len(b)))]

def secure_delete(file, key):
    with open(file, "rb") as fp:
        img = fp.read()

    with open(file, "wb") as fp:
        header, rest = img[:HEADER_SIZE], img[HEADER_SIZE:]
        fp.write(header)
        fp.write(bytes(enc(rest, key)))

    os.remove(file)


if __name__ == "__main__":
    if len(sys.argv) == 1:
        print(f"please specify files to delete: ./{sys.argv[0]} FILE1 FILE2 FILE3")
        sys.exit(1)

    args = sys.argv[1:]
    key_size = max([os.stat(f).st_size for f in args])
    key = os.urandom(key_size)

    for file in args:
        secure_delete(file, key)
```

The `key` is generated once and reused for each file before deletion. This means that participants can simply xor the two files together and they will hopefully get the flag.

Solution is in `solve.py` which writes the `xor.ppm` file where the flag is visible:
```
ibctf{1z-th1s_TUX_-th3-l1nux_pp3nguinN}
```
