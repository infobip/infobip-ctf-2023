# Solution

The `ll` file represents an LLVM IR which can be further converted to assembly or even C language via llvm2c.

Converting the `ll` file to C gives:
```
$ ./llvm2c --add-includes -p ll
// includes
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

// struct declarations
struct s_div_t;
struct s__IO_FILE;
struct s__IO_marker;
struct s__IO_codecvt;
struct s__IO_wide_data;

// struct definitions
struct s_div_t {
    unsigned int structVar0;
    unsigned int structVar1;
};
struct s__IO_FILE {
    unsigned int structVar2;
    unsigned char* structVar3;
    unsigned char* structVar4;
    unsigned char* structVar5;
    unsigned char* structVar6;
    unsigned char* structVar7;
    unsigned char* structVar8;
    unsigned char* structVar9;
    unsigned char* structVar10;
    unsigned char* structVar11;
    unsigned char* structVar12;
    unsigned char* structVar13;
    struct s__IO_marker* structVar14;
    struct s__IO_FILE* structVar15;
    unsigned int structVar16;
    unsigned int structVar17;
    unsigned long structVar18;
    unsigned short structVar19;
    unsigned char structVar20;
    unsigned char structVar21[1];
    unsigned char* structVar22;
    unsigned long structVar23;
    struct s__IO_codecvt* structVar24;
    struct s__IO_wide_data* structVar25;
    struct s__IO_FILE* structVar26;
    unsigned char* structVar27;
    unsigned long structVar28;
    unsigned int structVar29;
    unsigned char structVar30[20];
};
struct s__IO_marker {
};
struct s__IO_codecvt {
};
struct s__IO_wide_data {
};

// function declarations
void convert(unsigned char* var0, unsigned char* var1, unsigned int var2);
int main(int argc, char** argv);
unsigned int run(unsigned char* var0);

// global variable definitions
unsigned char matrix[26][10] = {{-63,116,51,-43,-109,-8,6,-75,-106,-71,},{-90,-57,-95,-14,34,73,97,75,-73,-36,},{44,-65,-33,-3,88,-81,10,104,-50,-97,},{14,95,-18,23,38,-17,-62,124,-76,-72,},{27,-39,-32,101,0,-108,-46,55,111,4,},{61,26,21,117,-15,-9,42,19,-40,107,},{72,120,103,46,20,-44,69,-93,-13,53,},{87,100,-41,-88,-70,-42,-125,-98,-48,93,},{7,96,57,45,-77,-66,54,-35,-60,-21,},{-112,-54,24,-114,99,41,-53,98,-89,50,},{66,92,40,-107,-45,37,-86,-102,68,-37,},{17,105,33,-68,-74,-67,127,-83,-113,76,},{-126,70,-105,18,47,-92,-31,5,16,-64,},{108,122,-79,-96,-12,-100,63,121,123,-55,},{11,-26,-99,83,-19,-25,106,-20,-110,67,},{85,-2,15,94,-120,-4,-59,79,-117,49,},{-16,84,-11,114,-38,62,-101,-116,39,1,},{65,-122,-7,-103,-94,48,-34,-78,-80,-6,},{113,-128,30,-69,71,-111,82,-84,-124,74,},{60,-58,78,-127,13,-121,43,80,-115,91,},{-23,-123,22,35,115,109,-49,119,12,-5,},{-29,-22,86,8,-27,56,77,89,32,3,},{-85,28,58,-104,-28,81,-56,-82,126,-10,},{31,-61,-91,59,-1,-24,-118,9,102,36,},{52,-47,110,25,64,-51,-52,112,90,125,},{29,-30,2,-119,-87,118,0,0,0,0,},};
unsigned char _str[2] = {114,0,};
unsigned char _str_1[9] = {114,101,100,46,112,105,108,108,0,};
unsigned char _str_2[2] = {119,0,};

void convert(unsigned char* var0, unsigned char* var1, unsigned int var2){
    unsigned char* var3;
    unsigned char* var4;
    unsigned int var5;
    unsigned int var6;
    struct s_div_t var7;
    struct s_div_t var8;
    block0:
    var3 = var0;
    var4 = var1;
    var5 = var2;
    var6 = 0;
    goto block1;
    block1:
    if (((int)var6) < ((int)var5)) {
        (*((unsigned long*)(&var8))) = div((int)((char)(*(((unsigned char*)(var3)) + ((long)((int)var6))))), 10);
        var7 = var8;
        (*(((unsigned char*)(var4)) + ((long)((int)var6)))) = ((matrix[(long)((int)(var7.structVar0))])[(long)((int)(var7.structVar1))]);
        var6 = (((int)var6) + ((int)1));
        goto block1;
    } else {
        return;
    }
}

int main(int argc, char** argv){
    unsigned int var2;
    unsigned int var3;
    unsigned char** var4;
    block0:
    var2 = 0;
    var3 = argc;
    var4 = argv;
    if (var3 != 2) {
        var2 = 1;
        return var2;
    } else {
        var2 = run(*(((unsigned char**)(var4)) + 1));
        return var2;
    }
}

unsigned int run(unsigned char* var0){
    unsigned int var1;
    unsigned char* var2;
    struct s__IO_FILE* var3;
    unsigned char var4[255];
    unsigned char var5[255];
    unsigned int var6;
    unsigned char var7;
    unsigned int var10_phi;
    unsigned char var8;
    unsigned int var9;
    block0:
    var2 = var0;
    var3 = fopen(var2, &(_str[0]));
    if (var3 == 0) {
        var1 = 1;
        return var1;
    } else {
        var6 = 0;
        goto block3;
    }
    block3:
    var8 = ((unsigned char)getc(var3));
    var7 = var8;
    if (((int)((char)var8)) != -1) {
        var10_phi = 1;
        goto block5;
    } else {
        var9 = (((int)var6) < ((int)255));
        var10_phi = var9;
        goto block5;
    }
    block5:
    if (var10_phi) {
        (var4[(long)((int)var6)]) = var7;
        var6 = (((int)var6) + ((int)1));
        goto block3;
    } else {
        fclose(var3);
        convert(&(var4[0]), &(var5[0]), var6);
        var3 = fopen(&(_str_1[0]), &(_str_2[0]));
        fputs(&(var5[0]), var3);
        fclose(var3);
        var1 = 0;
        return var1;
    }
}
```

Participants have to keep in mind that `llvm2c` is not magical and some things are not as in the original code. For instance there is one very obvious tell regarding the `matrix` variable:
```
unsigned char matrix[26][10] = {{-63,116,51,-43,-109,...
```

The variable is labeled as `unsigned` but contains negative numbers which doesn't make sense, so does values must be converted to a positive number as you would expect a computer would do (two's complement, or more easily just 256 - x = y). Therefore the first few number of the matrix would actually be:
```
193 (256-63), 116, 51, 213 (256-43), 147 (256-109), ...
```

By reviewing the C code it is easily seen that the `red.pill` file was produced by converting each byte of the file into the byte represented by the matrix - where quotient of diving by 10 is the first index and remainder is the second:
```
(*((unsigned long*)(&var8))) = div((int)((char)(*(((unsigned char*)(var3)) + ((long)((int)var6))))), 10);
var7 = var8;
(*(((unsigned char*)(var4)) + ((long)((int)var6)))) = ((matrix[(long)((int)(var7.structVar0))])[(long)((int)(var7.structVar1))]);
```

The quotient and remainder are div_t struct vars got as the result of `div` function.

To return the file to the original state, one must find the byte inside the matrix and calculate the original char by using its position, where its y position is multiplied by 10 and x position is added to the previously multiplied y position:
```
$ python3 solve.py 
You wanted to stay in Wonderland but ibctf{I_sh0Uld-h4v3-t4k3n_th3_BLU-p1ll-103412}
```
