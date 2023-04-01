#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

__attribute__((constructor))
void setup() {
	setvbuf(stdin, NULL, _IONBF, 0);
	setvbuf(stdout, NULL, _IONBF, 0);
	setvbuf(stderr, NULL, _IONBF, 0);
	alarm(60);
}

void __reg_clr_stdio() {
	__asm__("jmp %rsp\n\t");
}

void read_input() {
	char data[32];
	gets(data);
}

int main() {
	printf("Come on folks, you know what to do: ");
	read_input();
	return 0;
}
