package org.jboss.pnc.buildagent.common;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class StringLiner {

    private final StringBuffer stringBuffer = new StringBuffer();

    public void append(String string) {
        stringBuffer.append(string);
    }

    public synchronized String nextLine() {
        int nlLength = 1;
        int nlPosition = stringBuffer.indexOf("\n");
        if (nlPosition == -1) {
            nlPosition = stringBuffer.indexOf("\r");
        }
        if (nlPosition == -1) {
            nlPosition = stringBuffer.indexOf("\r\n");
            nlLength = 2;
        }
        if (nlPosition > -1) {
            String line = stringBuffer.substring(0, nlPosition);
            stringBuffer.delete(0, nlPosition + nlLength);
            return line;
        }
        return null;
    }
}
