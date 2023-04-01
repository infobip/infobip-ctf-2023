#include <ctype.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define __MAX_VAR_NAME_SIZE 10
#define __MAX_STRING_SIZE 40
#define __MAX_ALLOCA_SIZE 40
#define __MAX_INPUT_SIZE 64
#define __MAX_VARS 16

__attribute__((constructor))
void setup() {
	setvbuf(stdin, NULL, _IONBF, 0);
	setvbuf(stdout, NULL, _IONBF, 0);
	setvbuf(stderr, NULL, _IONBF, 0);
}

enum op {
	UNKW,
	DECL,
	FUNC,
};

enum method {
	NOWRK,
	SPLIT,
	CLEAR,
	ADDCH,
	PRINT,
	PURGE,
	VRDMP,
	_HELP,
};

struct op_info {
	enum op o;
	enum method m;
	int var_idx;
	void* dbgfunc;
	char reserved;
	char cparam[__MAX_STRING_SIZE];
	char vparam[__MAX_VAR_NAME_SIZE];
};

struct var_info {
	int8_t size;
	int8_t cap;
	int16_t reserved;
	char var_name[__MAX_VAR_NAME_SIZE];
	char* var_content;
};

struct var_info vars[__MAX_VARS];
char line[__MAX_INPUT_SIZE];

void op_decl_resolve_val(struct op_info* ret, char* src, int ci) {
	int quotes = 0, li = 0, token;
	
	while (token = *src) {
		++src;
		++ci;

		switch (quotes) {
		case 0:
			if (token != '"') {
				printf("illegal char %c at column %d\n", token, ci);
				ret->o = UNKW;
				ret->m = NOWRK;
				return;
			}
			++quotes;
			break;
		case 1:
			if (token != '"') {
				if (li >= __MAX_STRING_SIZE) {
					printf("illegal string size, max %d\n", __MAX_STRING_SIZE);
					ret->o = UNKW;
					ret->m = NOWRK;
					return;
				}
				ret->cparam[li++] = token;
			} else {
				++quotes;
			}
			break;
		}
		if (token == '"' && quotes >= 2) {
			if (*src != '\0') {
				printf("illegal char %c found at the end of the string\n", *src);
				ret->o = UNKW;
				ret->m = NOWRK;
				ret->var_idx = -1;
				return;
			}
			break;
		}
	}
}

void op_decl(struct op_info* ret) {
	if (ret->var_idx != -1) {
		vars[ret->var_idx].cap = __MAX_STRING_SIZE;
		vars[ret->var_idx].size = strlen(ret->cparam);
		strcpy(vars[ret->var_idx].var_name, ret->vparam);
		strcpy(vars[ret->var_idx].var_content, ret->cparam);
	}
}

void op_func_resolve_m(struct op_info* ret, char* src, int ci) {
	int mi = 0, token;
	
	while (token = *src) {
		++src;
		++ci;

		if ((token >= 'a' && token <= 'z')
		 || (token >= 'A' && token <= 'Z')
		 || (token >= '0' && token <= '9')
		 || (token == '_')) {
		 	if (mi >= __MAX_STRING_SIZE) {
		 		printf("illegal method size, max %d\n", __MAX_STRING_SIZE);
				ret->m = NOWRK;
				return;
		 	}
			ret->cparam[mi++] = token;
			continue;
		}
		
		if (token == '(') {
			break;
		}
		
		printf("unknown token %c found at column %d\n", token, ci);
		ret->m = NOWRK;
		return;
	}
		
	if (strcmp(ret->cparam, "split") == 0) {
		ret->m = SPLIT;
	} else if (strcmp(ret->cparam, "clear") == 0) {
		ret->m = CLEAR;
	} else if (strcmp(ret->cparam, "addch") == 0) {
		ret->m = ADDCH;
	} else if (strcmp(ret->cparam, "print") == 0) {
		ret->m = PRINT;
	} else if (strcmp(ret->cparam, "purge") == 0) {
		ret->m = PURGE;
	} else {
		printf("cannot find method %s\n", ret->cparam);
		ret->m = NOWRK;
		return;
	}

	if (ret->m == SPLIT || ret->m == ADDCH) {
		token = *src;
		++src;
		if (token == '\0' || token == ')' || *src == '\0') {
			printf("expected parameter but got none\n");
			ret->m = NOWRK;
			return;
		}
		if (*src != ')') {
			printf("expected char but got extra %s\n", src);
			ret->m = NOWRK;
			return;
		}
		ret->reserved = (char)token;
	} else {
		if (*src != ')') {
			printf("passed parameters to method expecting 0 parameters\n");
			ret->m = NOWRK;
		}
	}

	return;
}

void op_func_m_print(struct op_info* ret) {
	if (ret->var_idx != -1) {
		int i;
		printf("'");
		for (i = 0; i < vars[ret->var_idx].size; ++i) {
			unsigned char* c = ((unsigned char*)vars[ret->var_idx].var_content)[i];
			printf(isprint(c) ? "%c" : "\\x%02x", c);
		}
		printf("'\n");
	}
}

void op_func_m_clear(struct op_info* ret) {
	if (ret->var_idx != -1) {
		memset(vars[ret->var_idx].var_content, 0, vars[ret->var_idx].size);
		vars[ret->var_idx].size = 0;
	}
}

void op_func_m_addch(struct op_info* ret) {
	if (ret->var_idx != -1) {
		int l = vars[ret->var_idx].size;
		char* w = vars[ret->var_idx].var_content;
		if ((l + 1) < vars[ret->var_idx].cap) {
			memset(w + l, (char)ret->reserved, 1);
			memset(w + l + 1, '\0', 1);
			vars[ret->var_idx].size = l + 1;
		} else {
			printf("cap reached\n");
		}
	}
}

void op_func_m_split(struct op_info* ret) {
	if (ret->var_idx != -1 && ret->reserved) {
		int i, ns = -1;
		for (i = 0; i < vars[ret->var_idx].size; ++i) {
			if (vars[ret->var_idx].var_content[i] == (char)ret->reserved) {
				ns = i;
				break;
			}
		}
		if (ns == -1) {
			printf("no occurence of %c found in %s\n", ret->reserved, vars[ret->var_idx].var_name);
			return;
		}

		for (i = 0; i < __MAX_VARS; ++i) {
			if (vars[i].size == 0) {
				snprintf(vars[i].var_name, __MAX_VAR_NAME_SIZE, "%s0", vars[ret->var_idx].var_name);
				if (strcmp(vars[i].var_name, vars[ret->var_idx].var_name) == 0) {
					printf("unsupported operation: var name size reached\n");
					return;
				}
				vars[i].var_content = (char*)(vars[ret->var_idx].var_content + ns + 1);
				vars[i].cap = __MAX_STRING_SIZE - ns;
				vars[i].size = vars[ret->var_idx].size - ns - 1;
				vars[ret->var_idx].size = ns;
				vars[ret->var_idx].cap = ns;
				memset(vars[ret->var_idx].var_content + ns, '\0', 1);
				return;
			}
		}

		printf("variable memory is full\n");
	}
}

void op_func_m_purge(struct op_info* ret) {
	if (ret->var_idx != -1) {
		memset(vars[ret->var_idx].var_content, 0, vars[ret->var_idx].size);
		vars[ret->var_idx].var_content = NULL;
		memset(vars[ret->var_idx].var_name, 0, __MAX_VAR_NAME_SIZE);
		vars[ret->var_idx].cap = 0;
		vars[ret->var_idx].size = 0;
	}
}

void op_func_vrdmp(struct op_info* ret) {
	int i;
	for (i = 0; i < __MAX_VARS; ++i) {
		printf("[%d] -> (%d/%d) %s = ", i, vars[i].size, vars[i].cap, vars[i].var_name);
		ret->var_idx = i;
		op_func_m_print(ret);
	}
}

void op_func__help(struct op_info* ret) {
	char buffer[1300];
	sprintf(&buffer[0], "Help for CobraLang 0.0.1a:\n"
						"\n"
						"Implemented types:\n"
						"- string\n"
						"\n"
						"Implemented builtin functions:\n"
						"- var_dump - dumps the current state of variables\n"
						"- help     - prints this help output\n"
						"- quit     - quits the interpreter\n"
						"\n"
						"Help for type string:\n"
						"\n"
						"Type string is used to store array of characters. To instantiate a string simply use the already known Pythonic way:\n"
						"  new_string = \"this is my string\"\n"
						"\n"
						"Implemented methods:\n"
						"- print()\n"
						"  - no parameters\n"
						"  - prints the value of the string variable, e.g. new_string.print(), print can also be invoked by simply writing the name of the variable in the interpreter, e.g. new_string\n"
						"- clear()\n"
						"  - no parameters\n"
						"  - clears the value of the string variable, e.g. new_string.clear()\n"
						"- purge()\n"
						"  - no parameters\n"
						"  - deletes the string from memory\n"
						"- addch(c)\n"
						"  - one parameter: char to add to the string\n"
						"  - adds the parameter at the end of the string, e.g. new_string.addch(a) resulting in \"this is my stringa\", the char parameter is written directly without quotes\n"
						"- split(c)\n"
						"  - one parameter: char used as delimiter for splitting\n"
						"  - splits the string on first occurrence of character c, e.g. new_string.split(i) results in new_string with value \"th\" and a new variable stored as new_string0 with value \"s is my string\"");
	printf("%s\n", buffer);
}

void exec_func(struct op_info* ret) {
	switch (ret->m) {
	case SPLIT:
		op_func_m_split(ret);
		break;
	case CLEAR:
		op_func_m_clear(ret);
		break;
	case ADDCH:
		op_func_m_addch(ret);
		break;
	case PRINT:
		op_func_m_print(ret);
		break;
	case PURGE:
		op_func_m_purge(ret);
		break;
	case VRDMP:
		op_func_vrdmp(ret);
		break;
	case _HELP:
		op_func__help(ret);
		break;
	}
}

struct op_info consume(char* src) {
	int token;
	int i, vi = 0, ci = 0;
	int8_t first_idx = -1;
	struct op_info ret = { .m = NOWRK, .o = UNKW, .var_idx = -1, .dbgfunc = (void*)printf };
	
	while (token = *src) {
		++src;
		++ci;

		if (token == ' '
		 || token == '\t'
		 || token == '\f'
		 || token == '\v'
		 || token == '\r') {
			continue;
		}

		if (ret.var_idx == -1) {
			if ((token >= 'a' && token <= 'z')
			 || (token >= 'A' && token <= 'Z')
			 || (token >= '0' && token <= '9')
			 || (token == '_')) {
			 	if (vi >= __MAX_VAR_NAME_SIZE) {
			 		printf("illegal var size, max %d\n", __MAX_VAR_NAME_SIZE);
					return ret;
			 	}
				ret.vparam[vi++] = token;
				if (*src != '\0') {
					continue;
				}
				token = *src;
			}
			switch (token) {
			case '=':
				ret.o = DECL;
				break;
			case '.':
				ret.o = FUNC;
				break;
			case '\0':
				if (strcmp(ret.vparam, "var_dump") == 0) {
					ret.o = FUNC;
					ret.m = VRDMP;
				} else if (strcmp(ret.vparam, "help") == 0) {
					ret.o = FUNC;
					ret.m = _HELP;
				} else {
					ret.o = FUNC;
					ret.m = PRINT;
				}
				break;
			default:
				ret.o = UNKW;
			}
			
			if (ret.o == UNKW) {
				printf("illegal char %c found at column %d\n", token, ci);
				return ret;
			}
			
			for (i = 0; i < __MAX_VARS; ++i) {
				if (vars[i].size == 0 && first_idx == -1) {
					first_idx = i;
				}
				if (strcmp(vars[i].var_name, ret.vparam) == 0) {
					ret.var_idx = i;
					break;
				}
			}
			
			if (ret.var_idx == -1) {
				if (first_idx == -1) {
					printf("var limit reached\n");
					return ret;
				}
				if (ret.m != PRINT) {
					ret.var_idx = first_idx;
				}
			}
			continue;
		}
		
		if (ret.o == DECL) {
			op_decl_resolve_val(&ret, src - 1, ci);
			return ret;
		} else if (ret.o == FUNC) {
			op_func_resolve_m(&ret, src - 1, ci);
			return ret;
		} else {
			printf("error in lex\n");
			return ret;
		}
	}
	
	return ret;
}

int read_input() {
	char newline;
	int ret = scanf("%60[^\n]%c", line, &newline);
	if (strcmp(line, "quit") == 0) {
		return EOF;
	}
	return ret;
}

int interactive() {
	int i;
	while (1) {
		printf(">>> ");
		if (read_input() == EOF) {
			__asm__("xor %rsi, %rsi\n\t"
					"xor %rdx, %rdx\n\t");
			return EOF;
		}
		struct op_info ret = consume(line);
		
		switch (ret.o) {
		case DECL:
			if (vars[ret.var_idx].size <= 0) {
				vars[ret.var_idx].var_content = alloca(__MAX_ALLOCA_SIZE);
			}
			op_decl(&ret);
			break;
		case FUNC:
			exec_func(&ret);
			break;
		case UNKW:
		default:
			break;
		}
	}
}

int main() {
	printf("CobraLang 0.0.1a (Release 281222-linux) [gcc 11.3.0]\n");
	printf("Use 'help' for more information, 'var_dump' for debug var info, 'quit' or EOF to exit.\n");
	int ri = interactive();
	switch (ri) {
		case 0:
			break;
		case EOF:
			printf("received EOF\n");
			break;
		default:
			printf("unexpected error occured %d\n", ri);
			break;
	}
	return ri;
}
