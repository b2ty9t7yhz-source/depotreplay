package dev.depotreplay.core.replay;

import java.nio.charset.StandardCharsets;

/** Reflection-free SHA-256 implementation shared with the TeaVM browser build. */
final class PortableSha256 {
    private static final int[] ROUND_CONSTANTS = {
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
            0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
            0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
            0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
            0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
            0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
            0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
            0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
            0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private PortableSha256() { }

    static String digestUtf8(String value) {
        return digest(value.getBytes(StandardCharsets.UTF_8));
    }

    static String digest(byte[] input) {
        int paddedLength = ((input.length + 9 + 63) / 64) * 64;
        byte[] message = new byte[paddedLength];
        System.arraycopy(input, 0, message, 0, input.length);
        message[input.length] = (byte) 0x80;
        long bitLength = Math.multiplyExact((long) input.length, 8L);
        for (int index = 0; index < 8; index++) {
            message[paddedLength - 1 - index] = (byte) (bitLength >>> (index * 8));
        }

        int[] hash = {
                0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
                0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
        };
        int[] words = new int[64];
        for (int offset = 0; offset < message.length; offset += 64) {
            prepareWords(message, offset, words);
            compress(hash, words);
        }
        return toHex(hash);
    }

    private static void prepareWords(byte[] message, int offset, int[] words) {
        for (int index = 0; index < 16; index++) {
            int base = offset + index * 4;
            words[index] = (message[base] & 0xff) << 24
                    | (message[base + 1] & 0xff) << 16
                    | (message[base + 2] & 0xff) << 8
                    | message[base + 3] & 0xff;
        }
        for (int index = 16; index < words.length; index++) {
            int first = words[index - 15];
            int second = words[index - 2];
            int sigmaZero = Integer.rotateRight(first, 7)
                    ^ Integer.rotateRight(first, 18)
                    ^ first >>> 3;
            int sigmaOne = Integer.rotateRight(second, 17)
                    ^ Integer.rotateRight(second, 19)
                    ^ second >>> 10;
            words[index] = words[index - 16] + sigmaZero + words[index - 7] + sigmaOne;
        }
    }

    private static void compress(int[] hash, int[] words) {
        int a = hash[0];
        int b = hash[1];
        int c = hash[2];
        int d = hash[3];
        int e = hash[4];
        int f = hash[5];
        int g = hash[6];
        int h = hash[7];

        for (int index = 0; index < words.length; index++) {
            int upperSigmaOne = Integer.rotateRight(e, 6)
                    ^ Integer.rotateRight(e, 11)
                    ^ Integer.rotateRight(e, 25);
            int choice = e & f ^ ~e & g;
            int temporaryOne = h + upperSigmaOne + choice + ROUND_CONSTANTS[index] + words[index];
            int upperSigmaZero = Integer.rotateRight(a, 2)
                    ^ Integer.rotateRight(a, 13)
                    ^ Integer.rotateRight(a, 22);
            int majority = a & b ^ a & c ^ b & c;
            int temporaryTwo = upperSigmaZero + majority;

            h = g;
            g = f;
            f = e;
            e = d + temporaryOne;
            d = c;
            c = b;
            b = a;
            a = temporaryOne + temporaryTwo;
        }

        hash[0] += a;
        hash[1] += b;
        hash[2] += c;
        hash[3] += d;
        hash[4] += e;
        hash[5] += f;
        hash[6] += g;
        hash[7] += h;
    }

    private static String toHex(int[] hash) {
        char[] result = new char[64];
        int resultIndex = 0;
        for (int value : hash) {
            for (int shift = 28; shift >= 0; shift -= 4) {
                result[resultIndex++] = HEX[value >>> shift & 0x0f];
            }
        }
        return new String(result);
    }
}
