package org.jboss.pnc.buildagent.common;

import java.nio.ByteBuffer;

public class Arrays {

    public static byte[] toBytes(int[] ints) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(ints.length);
        for (int i : ints) {
            byteBuffer.put((byte) i);
        }
        return byteBuffer.array();
    }

}
