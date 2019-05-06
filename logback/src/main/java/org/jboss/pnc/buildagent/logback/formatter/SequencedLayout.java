package org.jboss.pnc.buildagent.logback.formatter;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class SequencedLayout extends CustomKeysJsonLayout {

    private static final AtomicLong sequence = new AtomicLong();

    protected void addCustomDataToJsonMap(Map<String, Object> map, ILoggingEvent event) {
        map.put("sequence", sequence.getAndIncrement());
    }

}
