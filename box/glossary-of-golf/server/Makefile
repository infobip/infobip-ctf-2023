.PHONY: local

BIN=glossary
CFLAGS=-static -O0 -fno-stack-protector -z execstack -no-pie
CC=gcc

local:
	$(CC) -o $(BIN) $(CFLAGS) $(BIN).c -lseccomp

