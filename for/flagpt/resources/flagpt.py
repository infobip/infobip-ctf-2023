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
