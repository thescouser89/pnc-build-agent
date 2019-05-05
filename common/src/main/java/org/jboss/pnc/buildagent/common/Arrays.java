package org.jboss.pnc.buildagent.common;

import java.nio.charset.Charset;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Arrays {

    public static byte[] charIntstoBytes(int[] ints, Charset charset) {
        String string = IntStream.of(ints)
                .mapToObj(i -> new String(Character.toChars(i)))
                .collect(Collectors.joining());
        return string.getBytes(charset);
    }

    public static int[] bytesToInts(byte[] bytes) {
        int[] ints = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            ints[i] = bytes[i];
        }
        return ints;
    }
}
