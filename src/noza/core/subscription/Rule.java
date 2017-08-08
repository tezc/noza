package noza.core.subscription;

import noza.base.common.Util;

import java.util.EnumSet;

public class Rule
{
    enum Flag
    {
        PUB,
        SUB,
        FOR_ANONYMOUS_USERS,
        FOR_ALL_USERS,
        FOR_ALL_CLIENT_IDS,
        REGULAR,
        SINGLE_LEVEL_WC_OWNER,
        MULTI_LEVEL_WC_OWNER
    }

    private String str;
    private String username;
    private String clientId;
    private String topic;
    private String[] levels;
    private EnumSet<Flag> flags;

    public Rule(String username, String clientId, String access, String topic)
    {
        this.flags    = EnumSet.noneOf(Flag.class);
        this.username = (username == null) ? "" : username;
        this.clientId = (clientId == null) ? "" : clientId;
        this.topic    = topic;

        if (access.contains("pub")) {
            flags.add(Flag.PUB);
        }

        if (access.contains("sub")) {
            flags.add(Flag.SUB);
        }

        if (this.username.equals("#")) {
            flags.add(Flag.FOR_ALL_USERS);
        }
        else if (this.username.equals("")) {
            flags.add(Flag.FOR_ANONYMOUS_USERS);
        }

        if (this.clientId.equals("#")) {
            flags.add(Flag.FOR_ALL_CLIENT_IDS);
        }

        levels = Util.splitStr(topic, '/');

        for (String level : levels) {
            if (level.equals("+")) {
                flags.add(Flag.SINGLE_LEVEL_WC_OWNER);
            }
            else if (level.equals("#")) {
                flags.add(Flag.MULTI_LEVEL_WC_OWNER);
            }
        }

        if (!isMultiLevelWcOwner() && !isSingleLevelWcOwner()) {
            flags.add(Flag.REGULAR);
        }

        str = "Username : " + this.username + Util.newLine() +
              "ClientId : " + this.clientId + Util.newLine() +
              "Access   : " + access        + Util.newLine() +
              "Topic    : " + this.topic    + Util.newLine();
    }

    public boolean isPublishRule()
    {
        return flags.contains(Flag.PUB);
    }

    public boolean isSubscribeRule()
    {
        return flags.contains(Flag.SUB);
    }

    public boolean isForAnonymousUsers()
    {
        return flags.contains(Flag.FOR_ANONYMOUS_USERS);
    }

    public boolean isForAllUsers()
    {
        return flags.contains(Flag.FOR_ALL_USERS);
    }

    public boolean isForAllClientIds()
    {
        return flags.contains(Flag.FOR_ALL_CLIENT_IDS);
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



    public boolean verifyForPublish(String username, String clientId, TopicData topic)
    {
        if (!isPublishRule()) {
            return true;
        }

        if (isForAnonymousUsers()) {
            if (username.length() != 0) {
                return true;
            }
        }

        if (!isForAllUsers()) {
            if (!this.username.equals(username)) {
                return false;
            }
        }

        if (!isForAllClientIds()) {
            if (!this.clientId.equals(clientId)) {
                return false;
            }
        }

        if (isRegular()) {
            if (levels.length != topic.topicLevels.length) {
                return false;
            }

            for (int i = 0; i < levels.length; i++) {
                String elem = toActualStr(levels[i], clientId, username);

                if (!elem.equals(topic.topicLevels[i])) {
                    return false;
                }
            }

            return true;
        }
        else if (!isMultiLevelWcOwner()) {
            if (levels.length != topic.topicLevels.length) {
                return false;
            }

            for (int i = 0; i < levels.length; i++)  {
                String elem = toActualStr(levels[i], clientId, username);

                if (elem.equals("+")) {
                    continue;
                }

                if (!elem.equals(topic.topicLevels[i])) {
                    return false;
                }
            }

            return true;
        }
        else {
            if (levels.length > topic.topicLevels.length + 1) {
                return false;
            }

            for (int i = 0; i < levels.length; i++) {
                String elem = toActualStr(levels[i], clientId, username);

                if (elem.equals("+")) {
                    continue;
                }

                if (elem.equals("#")) {
                    return true;
                }

                if (!elem.equals(topic.topicLevels[i])) {
                    return false;
                }
            }

            return true;
        }
    }

    public boolean verifyForSubscription(String username, String clientId, TopicData topic)
    {
        if (!isSubscribeRule()) {
            return true;
        }

        if (isForAnonymousUsers()) {
            if (username.length() != 0) {
                return true;
            }
        }

        if (!isForAllUsers()) {
            if (!this.username.equals(username)) {
                return false;
            }
        }

        if (!isForAllClientIds()) {
            if (!this.clientId.equals(clientId)) {
                return false;
            }
        }

        if (levels.length > topic.topicLevels.length + 1) {
            return false;
        }

        int len = Math.min(levels.length, topic.topicLevels.length);

        int i;
        for (i = 0; i < len; i++) {
            String elem = toActualStr(levels[i], username, clientId);

            if (elem.equals("+")) {
                continue;
            }

            if (elem.equals("#")) {
                return true;
            }

            if (!elem.equals(topic.topicLevels[i])) {
                return false;
            }
        }


        if (i != levels.length) {
            if (!levels[i + 1].equals("#")) {
                return false;
            }
        }

        return true;
    }

    private String toActualStr(String level, String username, String clientId)
    {
        if (level.equals("$u")) {
            return username;
        }
        else if (level.equals("$c")) {
            return clientId;
        }
        else {
            return level;
        }
    }

    public String toString()
    {
        return str;
    }

}
