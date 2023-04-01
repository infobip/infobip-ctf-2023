import random
import select
import string
import sys
import time

HIT_COUNT = 400
EMPTY = ' '

def cls():
    print("\033c")

def intro():
    cls()
    for i in range(3, -1, -1):
        print("Hacker Keyboard - Balaclava")
        print(f"{i}...")
        time.sleep(1)
        cls()
        
def print_state(q):
    c = q.pop(0)
    for i in q[::-1]:
        print(f"  {i}")
    print(f"- {c} -")
    return c

def hit_q(c):
    t = random.randint(1, 9)/3
    i, o, e = select.select([sys.stdin], [], [], t)

    if i:
        r = sys.stdin.readline().strip()
        if r == c:
            return 1
    else:
        if c == ' ':
            return 0

    return -1

if __name__ == "__main__":
    total = 0
    score = 0

    try:
        intro()
        queue = [EMPTY, EMPTY, EMPTY]

        for i in range(HIT_COUNT):
            print(f"Score: {score}")
            c = print_state(queue)
            off = hit_q(c)

            prob = random.randint(1, 4) if i < HIT_COUNT - 3 else 1
            if prob == 1:
                a = EMPTY
            else:
                a = random.choice(string.ascii_letters + string.digits)
                total += 1

            queue.append(a)
            score += off
            cls()
    except KeyboardInterrupt:
        print("Aborting...")
    except Exception as e:
        print(f"exception: {e}")

    print(f"Score: {score}/{total}")
    if score != total:
        print("Not perfect :(")
    else:
        print("ibctf{1f-th3-PC_1z_n0t-c0mpl41n1ng-y0ur3_n0t-h1tt1ng_l0ud-en0ugh}")
