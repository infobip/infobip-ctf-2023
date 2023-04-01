#!/bin/sh

MEDIA_DIR="/media/fuse"
CUR_DIR=$(pwd)

dd if=/dev/zero of=pingu count=4096
fdisk pingu <<EOF
n
p
1

t
07
w
EOF
mkfs.ntfs -F pingu
mkdir "${MEDIA_DIR}"
mount pingu "${MEDIA_DIR}"
cp pingu.py *.ppm "${MEDIA_DIR}"
cd "${MEDIA_DIR}"
python3 pingu.py pic1.ppm pic2.ppm
cd "${CUR_DIR}"
umount "${MEDIA_DIR}"
rm -rf "${MEDIA_DIR}"
