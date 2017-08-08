package noza.base.config;

import noza.base.common.Util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

public class Configs
{
    private static final String defaultFileName = "data/config/config.properties";

    private EnumMap<Config, Object> configs;
    private String fileName;


    public Configs(String fileName)
    {
        this.configs  = new EnumMap<>(Config.class);
        this.fileName = (fileName != null) ? fileName : defaultFileName;
        this.fileName = System.getProperty("user.dir") + "/" + this.fileName;

        for (Config config : Config.array) {
            if (config.getGroup() == ConfigGroup.UNIQUE ||
                config.getGroup() == ConfigGroup.INTERNAL) {
                configs.put(config, config.getDefaultValue());
            }
        }

        try {
            read();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void set(Config config, Object o)
    {
        configs.put(config, o);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Config config)
    {
        return (T) configs.get(config);
    }

    @SuppressWarnings("unchecked")
    public boolean getBoolean(Config config)
    {
        return ((Boolean) configs.get(config));
    }

    @SuppressWarnings("unchecked")
    public int getInt(Config config)
    {
        return (Integer) configs.get(config);
    }

    @SuppressWarnings("unchecked")
    public long getLong(Config config)
    {
        return (Long) configs.get(config);
    }

    @SuppressWarnings("unchecked")
    public String getString(Config config)
    {
        return (String) configs.get(config);
    }

    @SuppressWarnings("unchecked")
    private void parse(Config config, String value, Map map)
    {
        if (value != null) {
            switch (config.getType()) {
                case BOOLEAN:
                    map.put(config, new Boolean(value));
                    break;
                case INTEGER:
                    map.put(config, new Integer(value));
                    break;
                case LONG:
                    map.put(config, new Long(value));
                    break;
                case STRING:
                    map.put(config, value);
                    break;
            }
        }
    }

    private void read() throws IOException
    {
        Properties prop = new Properties();

        prop.load(new FileInputStream(fileName));

        for (Config config : Config.array) {
            if (config.getGroup() != ConfigGroup.UNIQUE) {
                continue;
            }

            if (config.getType() == Config.ValueType.LIST) {
                List<Map<Config, Object>> list = new ArrayList<>();
                for (int i = 0; ; i++) {
                    Map<Config, Object> map = new LinkedHashMap<>();
                    for (Config subItem : config.getSubItems()) {
                        String value = prop.getProperty(subItem.getName() + "." + i);
                        if (value != null) {
                            parse(subItem, value, map);
                        }
                    }

                    if (map.size() == 0) {
                        break;
                    }

                    list.add(map);
                }

                configs.put(config, list);
            }
            else {
                parse(config, prop.getProperty(config.getName()), configs);
            }
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(2048);
        int width = 0;
        for (Config config : Config.array) {
            int configLen = config.getName().length();
            width = (configLen > width) ? configLen : width;
        }

        builder.append("Configs ").append(Util.newLine());
        for (Config config : configs.keySet()) {
            if (config.getGroup() == ConfigGroup.UNIQUE ||
                config.getGroup() == ConfigGroup.INTERNAL) {

                if (config.getType() != Config.ValueType.LIST) {
                    builder.append(Util.pad(config.getName(), width));
                    builder.append(" : ");
                    builder.append(configs.get(config));
                    builder.append(Util.newLine());
                }
                else {
                    List<Map<Config, Object>> list = get(config);
                    for (Map<Config, Object> subGroup : list) {
                        builder.append(Util.pad(config.getName(), width));
                        builder.append(" : ");
                        builder.append(Util.newLine());

                        for (Config subItem : subGroup.keySet()) {
                            builder.append(Util.pad(subItem.getName(), width));
                            builder.append(" : ");
                            builder.append(subGroup.get(subItem));
                            builder.append(Util.newLine());
                        }
                    }
                }
            }
        }

        return builder.toString();
    }
}
