package org.jboss.pnc.buildagent.common;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class StringLiner {

    private final StringBuilder stringBuilder = new StringBuilder();

    public void append(String string) {
        stringBuilder.append(string);
    }

    public String nextLine() {
        int nlLength = 1;
        int nlPosition = stringBuilder.indexOf("\n");
        if (nlPosition == -1) {
            nlPosition = stringBuilder.indexOf("\r");
        }
        if (nlPosition == -1) {
            nlPosition = stringBuilder.indexOf("\r\n");
            nlLength = 2;
        }
        if (nlPosition > -1) {
            String line = stringBuilder.substring(0, nlPosition);
            stringBuilder.delete(0, nlPosition + nlLength);
            return line;
        }
        return null;
    }
}
