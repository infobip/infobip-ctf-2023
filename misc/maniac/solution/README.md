# Solution

Based on the description in the pdf and the given string, one should notice that these are movements and that those movements draw the letters of the flag:
```
smfp = short move forwards pause = i
lmbscfRp = long move backwards short half circle forward right pause = b
scbLp = short half circle backwards left pause = c
lmfsmbrsRsmflmbp = ... = t
lmfrsRsmfsmbrsRsmfrsLsmfp = f
lcfLp = {
lmflcbRp = D
lcfRlcbLp = O
rsLsmfp = -
smfrsLsmfrsRsmfsmbrsRlmfrsLsmfp = Y
lcbLlcfRp = O
smbrsLscfrsRsmfp = U
rsRsmfp = -
lmbrsRlmfp = L
lmfp = I
lmfsmbrsRsmfrsLsmfrsRsmfsmbrsLlmbrsRsmfp = K
lmfrsRsmfsmbrsLsmbrsRsmfsmbrsLsmbrsRsmfp = E
rsLsmfp = -
lmflcbRp = D
lmfrsRsmfrsRlmfsmbrsRsmfp = A
lmfrsRsmfrsRsmfrsLsmfrsRsmfrsLsmfrsLlmfp = N
lcbLp = C
rsLsmfrsRsmfrsRsmfsmbrsLsmfrsRsmf = E
lcfRp = }
```

Idea is to put pen on paper and follow the described movements to draw letters and combine them to get the flag. The `pause/reset` (letter `p`) can be used to split individual letters. After following written movements you should get the flag:
```
ibctf{DO-YOU-LIKE-DANCE}
```
