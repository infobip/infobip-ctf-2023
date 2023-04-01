#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/mman.h>

unsigned char code[] = "SHELLCODE";

int main() {
	int l = strlen(code);
	void* mem = mmap(0, l, PROT_EXEC | PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0);
	memcpy(mem, code, l);
	asm("jmp *%0"
		:
		: "r"(mem));
	getchar();
	return 0;
}
