# Solution

A non-PIE binary that stores the flag in the libc tcache. The best solution is to call malloc and read the return address. Shellcode can be significantly shorter by omiting the rdx parameter which results in program returning large amount of memory, but simple regex search can extract the flag from the dump.

Solution is in `solve.py`:
```
$ python3 solve.py 
23
[+] Opening connection to 172.17.0.2 on port 9000: Done
b'Your shellcode can be at most 99 bytes, go:'
[+] Receiving all data: Done (130.00KB)
[*] Closed connection to 172.17.0.2 port 9000
b'ibctf{0ne_c4nn0t_b3-a-h4ck3r-1f_1t-iZ-H0L3_1n-1}'
```
