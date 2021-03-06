package org.jboss.pnc.buildagent.logback.formatter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class CustomKeysJsonLayout extends JsonLayout {

    protected Map<String, String> customAttrNames = new HashMap<>();

    public CustomKeysJsonLayout() {
        super();
    }

    @Override
    public void add(String fieldName, boolean field, String value, Map<String, Object> map) {
        super.add(getCustomOrDefault(fieldName), field, value, map);
    }

    @Override
    public void addTimestamp(String key, boolean field, long timeStamp, Map<String, Object> map) {
        super.addTimestamp(getCustomOrDefault(key), field, timeStamp, map);
    }

    @Override
    public void addMap(String key, boolean field, Map<String, ?> mapValue, Map<String, Object> map) {
        super.addMap(getCustomOrDefault(key), field, mapValue, map);
    }

    @Override
    protected void addThrowableInfo(String fieldName, boolean field, ILoggingEvent value, Map<String, Object> map) {
        super.addThrowableInfo(getCustomOrDefault(fieldName), field, value, map);
    }

    protected String getCustomOrDefault(String key) {
        return customAttrNames.getOrDefault(key, key);
    }

    public String getTimestampAttrName() {
        return customAttrNames.get(TIMESTAMP_ATTR_NAME);
    }

    public void setTimestampAttrName(String timestampAttrName) {
        customAttrNames.put(TIMESTAMP_ATTR_NAME, timestampAttrName);
    }

    public String getLevelAttrName() {
        return customAttrNames.get(LEVEL_ATTR_NAME);
    }

    public void setLevelAttrName(String levelAttrName) {
        customAttrNames.put(LEVEL_ATTR_NAME, levelAttrName);
    }

    public String getThreadAttrName() {
        return customAttrNames.get(THREAD_ATTR_NAME);
    }

    public void setThreadAttrName(String threadAttrName) {
        customAttrNames.put(THREAD_ATTR_NAME, threadAttrName);
    }

    public String getMdcAttrName() {
        return customAttrNames.get(MDC_ATTR_NAME);
    }

    public void setMdcAttrName(String mdcAttrName) {
        customAttrNames.put(MDC_ATTR_NAME, mdcAttrName);
    }

    public String getLoggerAttrName() {
        return customAttrNames.get(LOGGER_ATTR_NAME);
    }

    public void setLoggerAttrName(String loggerAttrName) {
        customAttrNames.put(LOGGER_ATTR_NAME, loggerAttrName);
    }

    public String getFormattedMessageAttrName() {
        return customAttrNames.get(FORMATTED_MESSAGE_ATTR_NAME);
    }

    public void setFormattedMessageAttrName(String formattedMessageAttrName) {
        customAttrNames.put(FORMATTED_MESSAGE_ATTR_NAME, formattedMessageAttrName);
    }

    public String getMessageAttrName() {
        return customAttrNames.get(MESSAGE_ATTR_NAME);
    }

    public void setMessageAttrName(String messageAttrName) {
        customAttrNames.put(MESSAGE_ATTR_NAME, messageAttrName);
    }

    public String getContextAttrName() {
        return customAttrNames.get(CONTEXT_ATTR_NAME);
    }

    public void setContextAttrName(String contextAttrName) {
        customAttrNames.put(CONTEXT_ATTR_NAME, contextAttrName);
    }

    public String getExceptionAttrName() {
        return customAttrNames.get(EXCEPTION_ATTR_NAME);
    }

    public void setExceptionAttrName(String exceptionAttrName) {
        customAttrNames.put(EXCEPTION_ATTR_NAME, exceptionAttrName);
    }
}
