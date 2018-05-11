package com.researchworx.cresco.dashboard;

import com.google.gson.Gson;
import com.researchworx.cresco.library.messaging.MsgEvent;
import com.researchworx.cresco.library.plugin.core.CExecutor;
import com.researchworx.cresco.library.utilities.CLogger;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class Executor extends CExecutor {
    private final CLogger logger;
    private Plugin mainPlugin;
    private Gson gson;

    Executor(Plugin plugin) {
        super(plugin);
        mainPlugin = plugin;
        this.logger = new CLogger(this.plugin.getMsgOutQueue(), this.plugin.getRegion(), this.plugin.getAgent(), this.plugin.getPluginID(), CLogger.Level.Trace);
        gson = new Gson();
    }

    @Override
    public MsgEvent processExec(MsgEvent ce) {
        logger.trace("Processing Exec message");

        switch (ce.getParam("action")) {

            case "repolist":
                return repoList(ce);

            default:
                logger.error("Unknown configtype found {} for {}:", ce.getParam("action"), ce.getMsgType().toString());

        }

        return null;
    }

    private MsgEvent repoList(MsgEvent msg) {

        Map<String,List<Map<String,String>>> repoMap = new HashMap<>();
        repoMap.put("plugins",getPluginInventory(mainPlugin.repoPath));
        msg.setCompressedParam("repolist",gson.toJson(repoMap));
        return msg;

    }


    private List<Map<String,String>> getPluginInventory(String repoPath) {
        List<Map<String,String>> pluginFiles = null;
        try
        {
            File folder = new File(repoPath);
            if(folder.exists())
            {
                pluginFiles = new ArrayList<>();
                File[] listOfFiles = folder.listFiles();

                for (int i = 0; i < listOfFiles.length; i++)
                {
                    if (listOfFiles[i].isFile())
                    {
                        try{
                            String jarPath = listOfFiles[i].getAbsolutePath();
                            String jarFileName = listOfFiles[i].getName();
                            String pluginName = getPluginName(jarPath);
                            String pluginMD5 = getJarMD5(jarPath);
                            String pluginVersion = getPluginVersion(jarPath);
                            //System.out.println(pluginName + " " + jarFileName + " " + pluginVersion + " " + pluginMD5);
                            //pluginFiles.add(listOfFiles[i].getAbsolutePath());
                            Map<String,String> pluginMap = new HashMap<>();
                            pluginMap.put("name",getPluginName(jarPath));
                            pluginMap.put("jarfile",listOfFiles[i].getName());
                            pluginMap.put("md5",getJarMD5(jarPath));
                            pluginMap.put("version",getPluginVersion(jarPath));
                            pluginFiles.add(pluginMap);
                        } catch(Exception ex) {

                        }

                    }

                }
                if(pluginFiles.isEmpty())
                {
                    pluginFiles = null;
                }
            }
        }
        catch(Exception ex)
        {
            pluginFiles = null;
        }
        return pluginFiles;
    }

    private String getPluginVersion(String jarFile) {
        String version = null;
        try{
            //String jarFile = AgentEngine.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            //logger.debug("JARFILE:" + jarFile);
            //File file = new File(jarFile.substring(5, (jarFile.length() )));
            File file = new File(jarFile);

            boolean calcHash = true;
            BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            long fileTime = attr.creationTime().toMillis();

            FileInputStream fis = new FileInputStream(file);
            @SuppressWarnings("resource")
            JarInputStream jarStream = new JarInputStream(fis);
            Manifest mf = jarStream.getManifest();

            Attributes mainAttribs = mf.getMainAttributes();
            version = mainAttribs.getValue("Implementation-Version");
        }
        catch(Exception ex)
        {
            ex.printStackTrace();

        }
        return version;
    }

    private String getPluginName(String jarFile) {
        String version = null;
        try{
            //String jarFile = AgentEngine.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            //logger.debug("JARFILE:" + jarFile);
            //File file = new File(jarFile.substring(5, (jarFile.length() )));
            File file = new File(jarFile);

            boolean calcHash = true;
            BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            long fileTime = attr.creationTime().toMillis();

            FileInputStream fis = new FileInputStream(file);
            @SuppressWarnings("resource")
            JarInputStream jarStream = new JarInputStream(fis);
            Manifest mf = jarStream.getManifest();

            Attributes mainAttribs = mf.getMainAttributes();
            version = mainAttribs.getValue("artifactId");
        }
        catch(Exception ex)
        {
            ex.printStackTrace();

        }
        return version;
    }

    private String getJarMD5(String pluginFile) {
        String jarString = null;
        try
        {
            Path path = Paths.get(pluginFile);
            byte[] data = Files.readAllBytes(path);

            MessageDigest m= MessageDigest.getInstance("MD5");
            m.update(data);
            jarString = new BigInteger(1,m.digest()).toString(16);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return jarString;
    }




}
