# Build

There are two components:
- `cubes011` - Go binary that draws a rectangle PNG and uses LSB to store key press details
- `injector` - C binary that performs HRK to EUR conversion but also searches for running `vim` instance, injects into it, executes shellcode and runs `cubes011`

First build the `cubes011` binary per its README.md and move it into the `injector` folder in order to build the full flow `vim` injection binary.
