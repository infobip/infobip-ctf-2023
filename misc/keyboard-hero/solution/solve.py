from pwn import *

def recv_score(r, end=False):
    r.recvuntil(b"Score:")
    score = r.readline()
    if b"/" in score:
        return True

    for _ in range(2):
        r.readline()

    return False

r = remote("172.17.0.2", 9000)
while not recv_score(r):
    raw = r.readline()
    c = raw.lstrip(b"- ").rstrip(b" -\n")
    if c:
        r.sendline(c)
print(r.readline().decode().strip())
