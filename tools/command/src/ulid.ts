// ULIDs: lexicographically sortable, client-generated ids. Offline creation
// never needs the server, and every mutation stays idempotent by construction.

const ENCODING = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"; // Crockford base32

let lastTime = 0;
let lastRandom: number[] = [];

export function ulid(now = Date.now()): string {
  let time = now;
  let random: number[];
  if (time === lastTime) {
    // Same millisecond: increment the random part so ids stay monotonic.
    random = [...lastRandom];
    for (let i = random.length - 1; i >= 0; i--) {
      if (random[i] < 31) {
        random[i]++;
        break;
      }
      random[i] = 0;
    }
  } else {
    const bytes = new Uint8Array(16);
    crypto.getRandomValues(bytes);
    random = Array.from(bytes, (b) => b & 31);
  }
  lastTime = time;
  lastRandom = random;

  let timePart = "";
  for (let i = 0; i < 10; i++) {
    timePart = ENCODING[time % 32] + timePart;
    time = Math.floor(time / 32);
  }
  return timePart + random.map((r) => ENCODING[r]).join("");
}
