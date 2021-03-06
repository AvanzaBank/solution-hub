package org.openspaces.test.persistency.cassandra.helper.config;

import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.ParameterizedClass;
import org.apache.cassandra.config.YamlConfigurationLoader;
import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CassandraConfigUtils
{

    public static void writeCassandraConfig(
            URL baseConfigurationLocation,
            File configDestination, 
            String cassandraDataRoot) throws IOException
    {

        /*InputStream is =  baseConfigurationLocation.openStream();
        Constructor constructor = new Constructor(Config.class);
        TypeDescription seedDesc = new TypeDescription(ParameterizedClass.class);
        seedDesc.putMapPropertyType("parameters", String.class, String.class);
        constructor.addTypeDescription(seedDesc);
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(FlowStyle.BLOCK);
        dumperOptions.setIndent(4);


        Yaml yaml = new Yaml(new Loader(constructor), new Dumper(dumperOptions));
        Config config;
        try
        {
            config = (Config) yaml.load(is);
        }
        finally
        {
            is.close();
        }*/
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(FlowStyle.BLOCK);
        dumperOptions.setIndent(4);

        Config config = new YamlConfigurationLoader().loadConfig(baseConfigurationLocation);
        Yaml yaml = new Yaml(dumperOptions);
        setCommitLogDirectory(config, cassandraDataRoot + "/commitlog");
        Files.createDirectories(Paths.get(cassandraDataRoot + "/commitlog"));
        setDataFileDirectories(config, cassandraDataRoot + "/data" );
        Files.createDirectories(Paths.get(cassandraDataRoot + "/data"));
        setSavedCachesDirectory(config, cassandraDataRoot + "/saved_caches");
        Files.createDirectories(Paths.get(cassandraDataRoot + "/saved_caches"));
        // unfortunate necessity
        String dump = yaml.dump(config);
        dump = dump.replace("    class_name: org.apache.cassandra.locator.SimpleSeedProvider",
                            "    - class_name: org.apache.cassandra.locator.SimpleSeedProvider");
        dump = dump.replace("    parameters:",
                            "      parameters:");
        dump = dump.replace("        seeds:",
                            "          - seeds:");
        CassandraTestUtils.writeToFile(configDestination, dump);
    }
    
    private static void setCommitLogDirectory(Config config, String dir)
    {
        config.commitlog_directory = dir;
    }
    
    private static void setDataFileDirectories(Config config, String dir)
    {
        config.data_file_directories = new String[] { dir };
    }
    
    private static void setSavedCachesDirectory(Config config, String dir)
    {
        config.saved_caches_directory = dir;
    }
    
}
