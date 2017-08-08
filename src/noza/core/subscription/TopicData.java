package noza.core.subscription;

import noza.base.common.Util;
import noza.base.exception.MqttException;
import noza.core.msg.Topic;

import java.util.Arrays;
import java.util.EnumSet;

public class TopicData
{
    enum Flag
    {
        SHARED,
        PERMANENT,
        FILTER,
        REGULAR,
        SINGLE_LEVEL_WC_OWNER,
        MULTI_LEVEL_WC_OWNER,
    }


    protected String topic;
    protected String[] topicLevels;

    protected String filter;
    protected String[] filterLevels;
    protected EnumSet<Flag> flags;

    public TopicData(String topic,
                     boolean isFilter, boolean permanent)
    {
        this.flags       = EnumSet.noneOf(Flag.class);

        this.topic       = topic;
        this.topicLevels = Util.splitStr(topic, '/');

        if (topic.startsWith("$shared")) {
            if (topic.charAt("$shared".length()) != '/') {
                throw new MqttException("Malformed topic :" + topic);
            }

            int index = topic.indexOf('/', "$shared".length());
            if (index == -1) {
                throw new MqttException("Malformed topic : " + topic);
            }

            flags.add(Flag.SHARED);
            filter = topic.substring(index + 1);
            filterLevels = Arrays.copyOfRange(topicLevels, 2, topicLevels.length);
        }
        else {
            filter       = topic;
            filterLevels = topicLevels;
        }

        if (permanent) {
            flags.add(Flag.PERMANENT);
        }

        if (isFilter) {
            flags.add(Flag.FILTER);

            if (filter.contains("+")) {
                flags.add(Flag.SINGLE_LEVEL_WC_OWNER);
            }

            if (filter.contains("#")) {
                flags.add(Flag.MULTI_LEVEL_WC_OWNER);
            }

            if (!isSingleLevelWcOwner() && !isMultiLevelWcOwner()) {
                flags.add(Flag.REGULAR);
            }
        }
        else {
            flags.add(Flag.REGULAR);
        }

        verifyFilterLevels();
    }

    protected TopicData(TopicData data)
    {
        this.topic        = data.topic;
        this.topicLevels  = data.topicLevels;
        this.filter       = data.filter;
        this.filterLevels = data.filterLevels;
        this.flags        = data.flags;
    }

    public String getFilter()
    {
        return filter;
    }

    public String getTopic()
    {
        return topic;
    }

    public boolean isShared()
    {
        return flags.contains(Flag.SHARED);
    }

    public boolean isPermanent()
    {
        return flags.contains(Flag.PERMANENT);
    }

    public boolean isFilter()
    {
        return flags.contains(Flag.FILTER);
    }

    public boolean isRegular()
    {
        return flags.contains(Flag.REGULAR);
    }

    public boolean isSingleLevelWcOwner()
    {
        return flags.contains(Flag.SINGLE_LEVEL_WC_OWNER);
    }

    public boolean isMultiLevelWcOwner()
    {
        return flags.contains(Flag.MULTI_LEVEL_WC_OWNER);
    }

    public void verifyFilterLevels()
    {

        for (String level : filterLevels) {
            if (level.contains("+")) {
                if (!level.equals("+") || !isFilter()) {
                    throw new MqttException("Malformed topic " + topic);
                }
            }

            if (level.contains("#")) {
                if (!level.equals("#") || !isFilter()) {
                    throw new MqttException("Malformed topic " + topic);
                }
            }

            if (level.contains("$")) {
                if (!level.equals("$SYS")) {
                    throw new MqttException("Malformed topic " + topic);
                }
                else if (!isFilter()) {
                    throw new MqttException("Malformed topic " + topic);
                }
            }
        }
    }
}
