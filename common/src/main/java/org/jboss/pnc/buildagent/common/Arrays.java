package org.jboss.pnc.buildagent.common;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;

public class Arrays {

    public static byte[] charIntstoBytes(int[] ints, CharsetEncoder charsetEncoder) {
        ByteBuffer bb = ByteBuffer.allocate(scale(ints.length, charsetEncoder.maxBytesPerChar()));
        for (int i = 0; i < ints.length; i++) {
            char[] chars = Character.toChars(ints[i]);
            CharBuffer charBuffer = CharBuffer.allocate(chars.length);

            for (int chIndex = 0; chIndex < chars.length; chIndex++) {
                charBuffer.append(chars[chIndex]);
            }

            try {
                charBuffer.flip();
                ByteBuffer encoded = charsetEncoder.encode(charBuffer);
                bb.put(encoded);
            } catch (CharacterCodingException e) {
                e.printStackTrace();
            }
        }
        return trim(bb.array(), bb.position());
    }

    private static byte[] trim(byte[] ba, int len) {
        if (len == ba.length)
            return ba;
        else
            return java.util.Arrays.copyOf(ba, len);
    }

    private static int scale(int len, float expansionFactor) {
        // We need to perform double, not float, arithmetic; otherwise
        // we lose low order bits when len is larger than 2**24.
        return (int)(len * (double)expansionFactor);
    }

}
