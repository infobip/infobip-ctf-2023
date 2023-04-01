import sys

if len(sys.argv) != 3:
    print(f"{sys.argv[0]} parent_binary embeddable_binary")
    sys.exit(1)

with open(sys.argv[1], 'rb') as fp:
    cube = fp.read()

with open(sys.argv[2], 'rb') as fp:
    cubes011 = fp.read()

cubes011xord = bytes([i^0x41 for i in cubes011])
content = cube + b"\x00"*(0x4000-len(cube)) + cubes011xord
with open(sys.argv[2], 'wb') as fp:
    fp.write(content)
