import random
import pprint

n = 26
c = [i for i in range(256)]
for _ in range(5):
    random.shuffle(c)

s = []
l = []
p = 0
for i in range(len(c)):
    l.append(c[i])
    p += 1
    if p > 9 or i == len(c) - 1:
        p = 0
        s.append(l)
        l = []

with open('matrix', 'w') as fp:
    pprint.pp(s, fp)
