.PHONY: local

BIN=cobra
CFLAGS=-O0 -fno-stack-protector -no-pie
CC=gcc

local:
	$(CC) -o $(BIN) $(CFLAGS) $(BIN).c -lgcc -lc

