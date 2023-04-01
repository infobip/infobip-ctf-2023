package main

import (
	"bufio"
	"crypto/aes"
	"crypto/cipher"
	"fmt"
	"image/png"
	"os"
	
	"github.com/auyer/steganography"
)

// Source: https://github.com/MarinX/keylogger/blob/a54d7834cc1a2d37536127ab438e6084887bc55a/keymapper.go#L4
var keyCodeMap = map[uint16]string{
	1:   "ESC",
	2:   "1",
	3:   "2",
	4:   "3",
	5:   "4",
	6:   "5",
	7:   "6",
	8:   "7",
	9:   "8",
	10:  "9",
	11:  "0",
	12:  "-",
	13:  "=",
	14:  "BS",
	15:  "TAB",
	16:  "Q",
	17:  "W",
	18:  "E",
	19:  "R",
	20:  "T",
	21:  "Y",
	22:  "U",
	23:  "I",
	24:  "O",
	25:  "P",
	26:  "[",
	27:  "]",
	28:  "ENTER",
	29:  "L_CTRL",
	30:  "A",
	31:  "S",
	32:  "D",
	33:  "F",
	34:  "G",
	35:  "H",
	36:  "J",
	37:  "K",
	38:  "L",
	39:  ";",
	40:  "'",
	41:  "`",
	42:  "L_SHIFT",
	43:  "\\",
	44:  "Z",
	45:  "X",
	46:  "C",
	47:  "V",
	48:  "B",
	49:  "N",
	50:  "M",
	51:  ",",
	52:  ".",
	53:  "/",
	54:  "R_SHIFT",
	55:  "*",
	56:  "L_ALT",
	57:  "SPACE",
	58:  "CAPS_LOCK",
	59:  "F1",
	60:  "F2",
	61:  "F3",
	62:  "F4",
	63:  "F5",
	64:  "F6",
	65:  "F7",
	66:  "F8",
	67:  "F9",
	68:  "F10",
	69:  "NUM_LOCK",
	70:  "SCROLL_LOCK",
	71:  "HOME",
	72:  "UP_8",
	73:  "PGUP_9",
	74:  "-",
	75:  "LEFT_4",
	76:  "5",
	77:  "RT_ARROW_6",
	78:  "+",
	79:  "END_1",
	80:  "DOWN",
	81:  "PGDN_3",
	82:  "INS",
	83:  "DEL",
	84:  "",
	85:  "",
	86:  "",
	87:  "F11",
	88:  "F12",
	89:  "",
	90:  "",
	91:  "",
	92:  "",
	93:  "",
	94:  "",
	95:  "",
	96:  "R_ENTER",
	97:  "R_CTRL",
	98:  "/",
	99:  "PRT_SCR",
	100: "R_ALT",
	101: "",
	102: "Home",
	103: "Up",
	104: "PgUp",
	105: "Left",
	106: "Right",
	107: "End",
	108: "Down",
	109: "PgDn",
	110: "Insert",
	111: "Del",
	112: "",
	113: "",
	114: "",
	115: "",
	116: "",
	117: "",
	118: "",
	119: "Pause",
}

func main() {
	in, err := os.Open("../../attachments/cube.png")
	if err != nil {
		panic(err)
	}
	defer in.Close()
	
	reader := bufio.NewReader(in)
	img, err := png.Decode(reader)
	if err != nil {
		panic(err)
	}

	msgSize := steganography.GetMessageSizeFromImage(img)
	msgEncrypted := steganography.Decode(msgSize, img)

	block, err := aes.NewCipher([]byte("mce3Ej10xk3Aqw19"))
	if err != nil {
		panic(err)
	}
	msg := make([]byte, msgSize)
	mode := cipher.NewCTR(block, []byte("kdE39vn1S0EE3kcm"))
	mode.XORKeyStream(msg, msgEncrypted)
	for i := 0; i < len(msg); i += 3 {
		pos := uint16(msg[i+1]<<8+msg[i+2])
		fmt.Printf("[%c]: %s\n", msg[i], keyCodeMap[pos])
	}
}
