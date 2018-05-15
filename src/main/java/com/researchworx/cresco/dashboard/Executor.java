package com.researchworx.cresco.dashboard;

import com.google.gson.Gson;
import com.researchworx.cresco.library.messaging.MsgEvent;
import com.researchworx.cresco.library.plugin.core.CExecutor;
import com.researchworx.cresco.library.utilities.CLogger;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.*;
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

        List<Map<String,String>> contactMap = getNetworkAddresses();
        repoMap.put("server",contactMap);

        msg.setCompressedParam("repolist",gson.toJson(repoMap));
        return msg;

    }


    private List<Map<String,String>> getNetworkAddresses() {
        List<Map<String,String>> contactMap = null;
        try {
            contactMap = new ArrayList<>();
            String port = plugin.getConfig().getStringParam("port", "3445");
            String protocol = "http";

                List<InterfaceAddress> interfaceAddressList = new ArrayList<>();

                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    if (!networkInterface.getDisplayName().startsWith("veth") && !networkInterface.isLoopback() && networkInterface.supportsMulticast() && !networkInterface.isPointToPoint() && !networkInterface.isVirtual()) {
                        logger.debug("Found Network Interface [" + networkInterface.getDisplayName() + "] initialized");
                        interfaceAddressList.addAll(networkInterface.getInterfaceAddresses());
                    }
                }

                for (InterfaceAddress inaddr : interfaceAddressList) {
                    logger.debug("interface addresses " + inaddr);
                    Map<String, String> serverMap = new HashMap<>();
                    String hostAddress = inaddr.getAddress().getHostAddress();
                    if (hostAddress.contains("%")) {
                        String[] remoteScope = hostAddress.split("%");
                        hostAddress = remoteScope[0];
                    }

                    serverMap.put("protocol", protocol);
                    serverMap.put("ip", hostAddress);
                    serverMap.put("port", port);
                    contactMap.add(serverMap);

                }


            //put hostname at top of list
            InetAddress addr = InetAddress.getLocalHost();
            String hostAddress = addr.getHostAddress();
            if (hostAddress.contains("%")) {
                String[] remoteScope = hostAddress.split("%");
                hostAddress = remoteScope[0];
            }
            Map<String,String> serverMap = new HashMap<>();
            serverMap.put("protocol", protocol);
            serverMap.put("ip", hostAddress);
            serverMap.put("port", port);

            contactMap.remove(contactMap.indexOf(serverMap));
            contactMap.add(0,serverMap);

            //Use env var for host with hidden external addresses
            String externalIp = plugin.getConfig().getStringParam("externalip");
            externalIp = "128.163.202.50";
            if(externalIp != null) {
                Map<String, String> serverMapExternal = new HashMap<>();
                serverMapExternal.put("protocol", protocol);
                serverMapExternal.put("ip", externalIp);
                serverMapExternal.put("port", port);
                contactMap.add(0,serverMapExternal);
            }

        } catch (Exception ex) {
            logger.error("getNetworkAddresses ", ex.getMessage());
        }


        return contactMap;
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
                            pluginMap.put("pluginname",pluginName);
                            pluginMap.put("jarfile",jarFileName);
                            pluginMap.put("md5",pluginMD5);
                            pluginMap.put("version",pluginVersion);
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
