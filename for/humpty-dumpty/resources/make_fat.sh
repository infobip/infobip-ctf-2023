FAT_FILENAME="f"
FAT_DEST="/tmp/${FAT_FILENAME}"
MNT_POINT="/tmp/usb"
ENC_DIR="${MNT_POINT}/e"
DEC_DIR="/tmp/p"
FLAG="flag.txt"
ENC_PASS="|Sf@\"nvLB&lUraVqkw\-"

rm -rf ${FAT_FILENAME}
dd if="/dev/zero" of="${FAT_DEST}" bs=1M count=1
fdisk "${FAT_DEST}" <<EOF
n
p
1

t
0b
w
EOF
mkfs.fat "${FAT_DEST}"
mkdir -p "${MNT_POINT}"
mount "${FAT_DEST}" "${MNT_POINT}"
mkdir -p "${ENC_DIR}" "${DEC_DIR}"
echo "${ENC_PASS}" | gocryptfs -init "${ENC_DIR}"
echo "${ENC_PASS}" | gocryptfs "${ENC_DIR}" "${DEC_DIR}"
cp "${FLAG}" "${DEC_DIR}"
umount -l "${DEC_DIR}"
umount -l "${MNT_POINT}"
cp "${FAT_DEST}" "${FAT_FILENAME}"
rm -rf "${ENC_DIR}" "${DEC_DIR}" "${FAT_DEST}" "${MNT_POINT}" 
