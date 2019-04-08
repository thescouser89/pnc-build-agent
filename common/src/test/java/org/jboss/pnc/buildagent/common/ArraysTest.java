package org.jboss.pnc.buildagent.common;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class ArraysTest {

    @Test
    public void charIntstoBytes() {
        String string = "x对于绑定\uFFFD";
        int[] ints = new int[string.length()];

        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            ints[i] = ((int)c);
        }

        CharsetEncoder charsetEncoder = StandardCharsets.UTF_8.newEncoder();
        byte[] bytes = Arrays.charIntstoBytes(ints, charsetEncoder);

        String reEncoded = new String(bytes, StandardCharsets.UTF_8);
        System.out.println(reEncoded);
        Assert.assertEquals(string, reEncoded);

    }
}