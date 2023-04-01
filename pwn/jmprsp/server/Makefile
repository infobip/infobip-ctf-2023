.PHONY: local

BIN=jmprsp
CFLAGS=-O0 -fno-stack-protector -no-pie -zexecstack
CC=gcc

local:
	$(CC) -o $(BIN) $(CFLAGS) $(BIN).c -lgcc -lc

