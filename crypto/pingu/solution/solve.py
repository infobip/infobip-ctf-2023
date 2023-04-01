HEADER_SIZE = 16

def enc(a, b):
    return [a[i]^b[i] for i in range(min(len(a), len(b)))]

with open("pic1.ppm", "rb") as fp:
    pic1 = fp.read()

with open("pic2.ppm", "rb") as fp:
    pic2 = fp.read()

with open("xor.ppm", "wb") as fp:
    fp.write(pic2[:HEADER_SIZE])
    fp.write(bytes(enc(pic1[HEADER_SIZE:], pic2[HEADER_SIZE:])))
