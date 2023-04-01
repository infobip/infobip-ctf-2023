#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <seccomp.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/types.h>

#define _SC_MAX_SIZE 25

__attribute__((constructor))
void setup() {
	setvbuf(stdin, NULL, _IONBF, 0);
	setvbuf(stdout, NULL, _IONBF, 0);
	setvbuf(stderr, NULL, _IONBF, 0);
	alarm(10);
}

int seccomp_rules() {
	int syscall_denylist[] = {
		SCMP_SYS(fork),
		SCMP_SYS(kill),
		SCMP_SYS(execve),
		SCMP_SYS(execveat),
		SCMP_SYS(connect),
		SCMP_SYS(accept),
		SCMP_SYS(sendto),
		SCMP_SYS(recvfrom),
		SCMP_SYS(sendmsg),
		SCMP_SYS(recvmsg),
		SCMP_SYS(bind),
		SCMP_SYS(listen)
	};
	int syscall_denylist_len = sizeof(syscall_denylist) / sizeof(int);
	
	scmp_filter_ctx ctx = seccomp_init(SCMP_ACT_ALLOW);
	if (!ctx) {
		return -1;
	}
	
	for (int i = 0; i < syscall_denylist_len; i++) {
		if (seccomp_rule_add(ctx, SCMP_ACT_ERRNO(EACCES), syscall_denylist[i], 0) != 0) {
			return -1;
		}
	}
	
	if (seccomp_load(ctx) != 0) {
		return -1;
	}
	
	seccomp_release(ctx);
	return 0;
}

int main() {
	void* mem;
	ssize_t length;
	char* flag = getenv("FLAG");
	if (flag == NULL) {
		printf("FLAG env not set\n");
		return 1;
	}
	size_t flag_len = strlen(flag);
	
	int seccomp_init_ret = seccomp_rules();
	if (seccomp_init_ret != 0) {
		printf("failed to load seccomp\n");
		return 1;
	}

	char* flag_mem = malloc(75);
	memcpy(flag_mem + 16, flag, flag_len);
	memset(flag, 0, flag_len);
	flag = NULL;
	free(flag_mem);
	mem = mmap(0, _SC_MAX_SIZE, PROT_EXEC | PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_SHARED, -1, 0);
	printf("Your shellcode can be at most %d bytes, go:", _SC_MAX_SIZE);
	length = read(0, mem, _SC_MAX_SIZE);

	if(length > _SC_MAX_SIZE) {
		printf("payload too big!\n");
		return 1;
	}

	asm("jmp *%0"
		:
		: "r"(mem));
	return 0;
}
