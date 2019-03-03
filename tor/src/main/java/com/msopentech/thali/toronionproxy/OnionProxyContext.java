/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED,
INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
MERCHANTABLITY OR NON-INFRINGEMENT.

See the Apache 2 License for the specific language governing permissions and limitations under the License.
*/

package com.msopentech.thali.toronionproxy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class encapsulates data that is handled differently in Java and Android as well
 * as managing file locations.
 */
abstract public class OnionProxyContext {
    protected final static String HIDDENSERVICE_DIRECTORY_NAME = "hiddenservice";
    protected final static String GEO_IP_NAME = "geoip";
    protected final static String GEO_IPV_6_NAME = "geoip6";
    protected final static String TORRC_NAME = "torrc";
    protected final static String PID_NAME = "pid";
    protected final File workingDirectory;
    protected final File geoIpFile;
    protected final File geoIpv6File;
    protected final File torrcFile;
    protected final File torExecutableFile;
    protected final File cookieFile;
    protected final File hostnameFile;
    protected final File pidFile;

    public OnionProxyContext(File workingDirectory) {
        this.workingDirectory = workingDirectory;
        geoIpFile = new File(getWorkingDirectory(), GEO_IP_NAME);
        geoIpv6File = new File(getWorkingDirectory(), GEO_IPV_6_NAME);
        torrcFile = new File(getWorkingDirectory(), TORRC_NAME);
        torExecutableFile = new File(getWorkingDirectory(), getTorExecutableFileName());
        cookieFile = new File(getWorkingDirectory(), ".tor/control_auth_cookie");
        hostnameFile = new File(getWorkingDirectory(), "/" + HIDDENSERVICE_DIRECTORY_NAME + "/hostname");
        pidFile = new File(getWorkingDirectory(), PID_NAME);
    }

    public void installFiles() throws IOException, InterruptedException {
        // This is sleezy but we have cases where an old instance of the Tor OP needs an extra second to
        // clean itself up. Without that time we can't do things like delete its binary (which we currently
        // do by default, something we hope to fix with https://github.com/thaliproject/Tor_Onion_Proxy_Library/issues/13
        Thread.sleep(1000,0);

        if (!workingDirectory.exists() && !workingDirectory.mkdirs()) {
            throw new RuntimeException("Could not create root directory!");
        }

        FileUtilities.cleanInstallOneFile(getAssetOrResourceByName(GEO_IP_NAME), geoIpFile);
        FileUtilities.cleanInstallOneFile(getAssetOrResourceByName(GEO_IPV_6_NAME), geoIpv6File);
        FileUtilities.cleanInstallOneFile(getAssetOrResourceByName(TORRC_NAME), torrcFile);

        switch(OsData.getOsType()) {
            case ANDROID:
                FileUtilities.cleanInstallOneFile(
                        getAssetOrResourceByName(getPathToTorExecutable() + getTorExecutableFileName()),
                        torExecutableFile);
                break;
            case WINDOWS:
            case LINUX_32:
            case LINUX_64:
            case MAC:
                FileUtilities.extractContentFromZip(getWorkingDirectory(),
                        getAssetOrResourceByName(getPathToTorExecutable() + "tor.zip"));
                break;
            default:
                throw new RuntimeException("We don't support Tor on this OS yet");
        }
    }

    /**
     * Sets environment variables and working directory needed for Tor
     * @param processBuilder we will call start on this to run Tor
     */
    public void setEnvironmentArgsAndWorkingDirectoryForStart(ProcessBuilder processBuilder) {
        processBuilder.directory(getWorkingDirectory());
        Map<String, String> environment = processBuilder.environment();
        environment.put("HOME", getWorkingDirectory().getAbsolutePath());
        switch (OsData.getOsType()) {
            case LINUX_32:
            case LINUX_64:
                // We have to provide the LD_LIBRARY_PATH because when looking for dynamic libraries
                // Linux apparently will not look in the current directory by default. By setting this
                // environment variable we fix that.
                environment.put("LD_LIBRARY_PATH", getWorkingDirectory().getAbsolutePath());
                break;
            default:
                break;
        }
    }

    public String[] getEnvironmentArgsForExec() {
        List<String> envArgs = new ArrayList<String>();
        envArgs.add("HOME=" + getWorkingDirectory().getAbsolutePath() );
        switch(OsData.getOsType()) {
            case LINUX_32:
            case LINUX_64:
                // We have to provide the LD_LIBRARY_PATH because when looking for dynamic libraries
                // Linux apparently will not look in the current directory by default. By setting this
                // environment variable we fix that.
                envArgs.add("LD_LIBRARY_PATH=" + getWorkingDirectory().getAbsolutePath());
                break;
            default:
                break;
        }
        return envArgs.toArray(new String[envArgs.size()]);
    }

    public File getGeoIpFile() {
        return geoIpFile;
    }

    public File getGeoIpv6File() {
        return geoIpv6File;
    }

    public File getTorrcFile() {
        return torrcFile;
    }

    public File getCookieFile() {
        return cookieFile;
    }

    public File getHostNameFile() {
        return hostnameFile;
    }

    public File getTorExecutableFile() {
        return torExecutableFile;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public File getPidFile() { return pidFile; }

    public void deleteAllFilesButHiddenServices() throws InterruptedException {
        // It can take a little bit for the Tor OP to detect the connection is dead and kill itself
        Thread.sleep(1000,0);
        for(File file : getWorkingDirectory().listFiles()) {
            if (file.isDirectory()) {
                if (file.getName().compareTo(HIDDENSERVICE_DIRECTORY_NAME) != 0) {
                    FileUtilities.recursiveFileDelete(file);
                }
            } else {
                if (!file.delete()) {
                    throw new RuntimeException("Could not delete file " + file.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Files we pull out of the AAR or JAR are typically at the root but for executables outside
     * of Android the executable for a particular platform is in a specific sub-directory.
     * @return Path to executable in JAR Resources
     */
    protected String getPathToTorExecutable() {
        String path = "native/";
        switch (OsData.getOsType()) {
            case ANDROID:
                return "";
            case WINDOWS:
                return path + "windows/x86/"; // We currently only support the x86 build but that should work everywhere
            case MAC:
                return path +  "osx/x64/"; // I don't think there even is a x32 build of Tor for Mac, but could be wrong.
            case LINUX_32:
                return path + "linux/x86/";
            case LINUX_64:
                return path + "linux/x64/";
            default:
                throw new RuntimeException("We don't support Tor on this OS");
        }
    }

    protected String getTorExecutableFileName() {
        switch(OsData.getOsType()) {
            case ANDROID:
            case LINUX_32:
            case LINUX_64:
                return "tor";
            case WINDOWS:
                return "tor.exe";
            case MAC:
                return "tor.real";
            default:
                throw new RuntimeException("We don't support Tor on this OS");
        }
    }

    abstract public String getProcessId();
    abstract public WriteObserver generateWriteObserver(File file);
    abstract protected InputStream getAssetOrResourceByName(String fileName) throws IOException;
}
