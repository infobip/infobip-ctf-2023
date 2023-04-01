# Solution

We are provided with a .pcap file. Opening it reveals fossil requests. The objects exchanged can be extracted via Export objects window in wireshark.

## The object

After exporting the second biggest object available we see:
```
$ binwalk -e mycloud 

DECIMAL       HEXADECIMAL     DESCRIPTION
--------------------------------------------------------------------------------
4             0x4             Zlib compressed data, best compression

$ file _mycloud.extracted/4
_mycloud.extracted/4: data
$ cat _mycloud.extracted/4
login vm 2dbe4c15e16aacf554f7d47942d19fd164e99c75 1c49841d6112b5ec5168845dd6ea448305a691fc
pragma client-version 21800 20220223 135402
pull 1baa8decbda3bda42e2ed27e600d1ad10b6d5125 260a775a52f10d515348a2b0c7d7386c69986a5a
push 1baa8decbda3bda42e2ed27e600d1ad10b6d5125 260a775a52f10d515348a2b0c7d7386c69986a5a
file 51c8725faa98617834ea51aa49c8579cc69e4d90c58b5b8b4f027eba0da4f4b3 1829
V��s��㮬K�D��$���7�3 ��'�Q�:�Ҁ6��d��%.H
...
```

The binwalk approach is the easiest one, but one can also note how fossil-scm docs state that the `application/x-fossil` content type compresses the content:
```
The content type is always either "application/x-fossil" or "application/x-fossil-debug". The "x-fossil" content type is the default. The only difference is that "x-fossil" content is compressed using zlib whereas "x-fossil-debug" is sent uncompressed.
```

Inspecting decompressed data there are interesting things to note:
- there are multiple directives here such as `pragma`, `file`, ...
- the `file` directive most certainly denotes the file with
  - a hash
  - file size
- after file content there is also a weird tag (directive?) called `F`
  - this tag references the aformentioned files via the same hash
- we see the file names in the tags:
```
F .seguridad.kdbx 51c8725faa98617834ea51aa49c8579cc69e4d90c58b5b8b4f027eba0da4f4b3
F La\smejor\scanción 8e58479972bd68f801161a2d9bc43da98fdd49ce9884203853d102508720c8cb18
```

### Extracting the files

We can see that there are some files in the data dump, we can split the annotated files with some python to get the raw bytes:
```
>>> with open("4", "rb") as fp:
...     d4 = fp.read()
... 
>>> header, first, second, tail = d4.split(b"file")
>>> with open(".seguridad.kdbx", "wb") as fp:
...     fp.write(first[71:])
... 
1829
>>> with open("La mejor canción", "wb") as fp:
...     fp.write(second[70:])
... 
704
```

Opening the .seguridad.kdbx file with Keepass seems to work fine, but we don't have the password. In order to get the password, one must use the second file.

## Getting the password

To get the password for .kdbx file one should note:
- "La mejor canción" means "the best song" in spanish
- Lyrics are of the song Volver, Volver
- `Volver, Volver` is the password of the Keepass file

The .kdbx file does not hold the flag:
```
$ keepassxc-cli export -f csv .seguridad.kdbx 
Enter password to unlock .seguridad.kdbx: Volver, Volver
"Group","Title","Username","Password","URL","Notes","TOTP","Icon","Last Modified","Created"
"Root","pass","","|Sf@""nvLB&lUraVqkw\-","","Your master key is:

    8831a256-ee9b04bf-204f59b6-1c001188-
    836e71e5-c740821a-5bd4a48c-5de8f83f","","0","2022-12-12T14:01:52Z","2022-12-12T13:22:00Z"
```

## Going back to the wireshark dump

The wireshark dump holds other important objects. Another large one of 16 kB:
```
$ binwalk -e mycloud16 

DECIMAL       HEXADECIMAL     DESCRIPTION
--------------------------------------------------------------------------------
127           0x7F            Zlib compressed data, default compression
362           0x16A           Zlib compressed data, default compression
16087         0x3ED7          Zlib compressed data, default compression

$ file _mycloud16.extracted/*
_mycloud16.extracted/16A:       DOS/MBR boot sector, code offset 0x3c+2, OEM-ID "mkfs.fat", sectors/cluster 4, root entries 512, sectors 2048 (volumes <=32 MB), Media descriptor 0xf8, sectors/FAT 2, sectors/track 16, serial number 0xc4c0f3b7, unlabeled, FAT (12 bit)
_mycloud16.extracted/16A.zlib:  zlib compressed data
_mycloud16.extracted/3ED7:      ASCII text
_mycloud16.extracted/3ED7.zlib: zlib compressed data
_mycloud16.extracted/7F:        ASCII text
_mycloud16.extracted/7F.zlib:   zlib compressed data
```

The ASCII text files don't seem as important on first sight:
```
$ cat _mycloud16.extracted/3ED7
C backup
D 2022-12-12T14:27:37.479
F f eae0dd74faadac925ac3bef1e108ebace6f5f3eb22e2f23932c6e5a2571b3317
P 3c86ca4e64393a2878e18bf09fdd6d53b628e6a070be0714de9a58ca4aa9a156
R 1a66c7f2d85d0d77b52b93e8019dcdae
U vm
Z c953fdc00080fca96a62e10a68993d55
$ cat _mycloud16.extracted/7F
C initial\sempty\scheck-in
D 2022-12-12T14:26:06.355
R d41d8cd98f00b204e9800998ecf8427e
T *branch * trunk
T *sym-trunk *
U vm
Z 62192d60f6421328547803c9ed6e4d1
```

The third file is shown as FAT file system which we can try to mount:
```
$ mkdir -p data
$ sudo mount _mycloud16.extracted/16A ./data
$ ls -alR ./data
./data:
total 22
drwxr-xr-x  3 root root 16384 sij   1  1970 .
drwxrwxrwt 24 root root  4096 pro  12 17:55 ..
drwxr-xr-x  2 root root  2048 pro  12 15:08 e

./data/e:
total 24
drwxr-xr-x 2 root root  2048 pro  12 15:08 .
drwxr-xr-x 3 root root 16384 sij   1  1970 ..
-rwxr-xr-x 1 root root   385 pro  12 15:01 gocryptfs.conf
-rwxr-xr-x 1 root root    16 pro  12 15:01 gocryptfs.diriv
-rwxr-xr-x 1 root root   103 pro  12 15:08 VXo0sYT0wk2I8lydf4LQHQ
```

Cool, a gocryptfs configuration is here. This means that there is another encrypted file system here which we need to mount, but what is the encryption password?

Since there was an unused password in the Keepass file, we can try with that:
```
$ gocryptfs ./data/e gocrypt/
Password: |Sf@"nvLB&lUraVqkw\-
Decrypting master key
Filesystem mounted and ready.
```

This brings us to the flag:
```
$ ls gocrypt/
flag.txt
$ cat gocrypt/flag.txt 
ibctf{n0-t3ng0-Vulnerab1l1d4d3s_n0-t3ngO-S3gur1d4dD}
```
