package org.jboss.pnc.buildagent.common.function;

import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public interface ThrowingConsumer<T, E extends Exception> {
    void accept(T t) throws E;

    static <T> Consumer<T> wrap(ThrowingConsumer<T, Exception> throwingConsumer) {
        return i -> {
            try {
                throwingConsumer.accept(i);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }
}
