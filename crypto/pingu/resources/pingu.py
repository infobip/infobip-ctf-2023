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
