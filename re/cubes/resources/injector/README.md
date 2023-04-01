# Build

To create shellcode use:
```
nasm -felf64 inject.s && ld -o inject inject.o
objcopy -O binary inject shellcode
```

Created shellcode can be tested with skeleton program:
```
HEX=$(hexdump -v -e '"\\xx" 1/1 "%02x"' shellcode)
sed -e "s/SHELLCODE/$(echo $HEX)/" ./skeleton.c | gcc -x c -o skeleton -
```

Production level challenge injector is built via script:
```
cp ../cubes011/cubes011 cubes011
./build.sh
```

Most of the execution requires `sudo`. 
