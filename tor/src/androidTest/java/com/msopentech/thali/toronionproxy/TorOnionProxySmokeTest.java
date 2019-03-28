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

/*
This code took the Socks4a logic from SocksProxyClientConnOperator in NetCipher which we then modified
to meet our needs. That original code was licensed as:

This file contains the license for Orlib, a free software project to
provide anonymity on the Internet from a Google Android smartphone.

For more information about Orlib, see https://guardianproject.info/

If you got this file as a part of a larger bundle, there may be other
license terms that you should be aware of.
===============================================================================
Orlib is distributed under this license (aka the 3-clause BSD license)

Copyright (c) 2009-2010, Nathan Freitas, The Guardian Project

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.

    * Neither the names of the copyright owners nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*****
Orlib contains a binary distribution of the JSocks library:
http://code.google.com/p/jsocks-mirror/
which is licensed under the GNU Lesser General Public License:
http://www.gnu.org/licenses/lgpl.html

*****

 */

package com.msopentech.thali.toronionproxy;

import com.msopentech.thali.local.toronionproxy.TorOnionProxyTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.Calendar;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class TorOnionProxySmokeTest extends TorOnionProxyTestCase {
    private static final int TOTAL_SECONDS_PER_TOR_STARTUP = 4 * 60;
    private static final int TOTAL_TRIES_PER_TOR_STARTUP = 5;
    private static final int WAIT_FOR_HIDDEN_SERVICE_MINUTES = 3;
    private static final Logger LOG = LoggerFactory.getLogger(TorOnionProxySmokeTest.class);

    /**
     * Start two TorOPs, one for a hidden service and one for a client. Have the hidden service op stop and start
     * and see if the client can get connected again.
     * @throws IOException
     * @throws InterruptedException
     */
    public void testHiddenServiceRecycleTime() throws IOException, InterruptedException {
        String hiddenServiceManagerDirectoryName = "hiddenservicemanager";
        String clientManagerDirectoryName = "clientmanager";
        OnionProxyManager hiddenServiceManager = null, clientManager = null;
        try {
            hiddenServiceManager = getOnionProxyManager(hiddenServiceManagerDirectoryName);
            // Note: Normally you never want to call anything like deleteTorWorkingDirectory since this
            // is where all the cached data about the Tor network is kept and it makes connectivity
            // must faster. We are deleting it here just to make sure we are running clean tests.
            deleteTorWorkingDirectory(hiddenServiceManager.getWorkingDirectory());
            assertTrue(hiddenServiceManager.startWithRepeat(TOTAL_SECONDS_PER_TOR_STARTUP, TOTAL_TRIES_PER_TOR_STARTUP));

            LOG.warn("Hidden Service Manager is running.");

            clientManager = getOnionProxyManager(clientManagerDirectoryName);
            deleteTorWorkingDirectory(clientManager.getWorkingDirectory());
            assertTrue(clientManager.startWithRepeat(TOTAL_SECONDS_PER_TOR_STARTUP, TOTAL_TRIES_PER_TOR_STARTUP));

            LOG.warn("Client Manager is running.");

            String onionAddress = runHiddenServiceTest(hiddenServiceManager, clientManager);

            LOG.warn("We successfully sent a message from client manager to hidden service manager!");

            // Now take down the hidden service manager and bring it back up with a new descriptor but the
            // same address
            hiddenServiceManager.stop();
            hiddenServiceManager.startWithRepeat(TOTAL_SECONDS_PER_TOR_STARTUP, TOTAL_TRIES_PER_TOR_STARTUP);
            // It's possible that one of our deletes could have nuked the hidden service directory
            // in which case we would actually be testing against a new hidden service which would
            // remove the point of this test. So we check that they are the same.
            assertEquals(runHiddenServiceTest(hiddenServiceManager, clientManager), onionAddress);
        } finally {
            if (hiddenServiceManager != null) {
                hiddenServiceManager.stop();
            }
            if (clientManager != null) {
                clientManager.stop();
            }
        }
    }

    public enum ServerState {SUCCESS, TIMEDOUT, OTHERERROR}

    private String runHiddenServiceTest(OnionProxyManager hiddenServiceManager, OnionProxyManager clientManager)
            throws IOException, InterruptedException {
        int localPort = 9343;
        int hiddenServicePort = 9344;
        String onionAddress = hiddenServiceManager.publishHiddenService(hiddenServicePort, localPort);
        LOG.info("onionAddress for test hidden service is: " + onionAddress);

        byte[] testBytes = new byte[] { 0x01, 0x02, 0x03, 0x05};

        long timeToExit = Calendar.getInstance().getTimeInMillis() + WAIT_FOR_HIDDEN_SERVICE_MINUTES * 60 * 1000;
        while(Calendar.getInstance().getTimeInMillis() < timeToExit) {
            SynchronousQueue<ServerState> serverQueue = new SynchronousQueue<ServerState>();
            Thread serverThread = receiveExpectedBytes(testBytes, localPort, serverQueue);

            Socket clientSocket =
                    getClientSocket(onionAddress, hiddenServicePort, clientManager.getIPv4LocalHostSocksPort());

            DataOutputStream clientOutputStream = new DataOutputStream(clientSocket.getOutputStream());
            clientOutputStream.write(testBytes);
            clientOutputStream.flush();
            ServerState serverState = serverQueue.poll(WAIT_FOR_HIDDEN_SERVICE_MINUTES, TimeUnit.MINUTES);
            if (serverState == ServerState.SUCCESS) {
                return onionAddress;
            } else {
                long timeForThreadToExit = Calendar.getInstance().getTimeInMillis() + 1000;
                while(Calendar.getInstance().getTimeInMillis() < timeForThreadToExit &&
                        serverThread.getState() != Thread.State.TERMINATED) {
                    Thread.sleep(1000,0);
                }
                if (serverThread.getState() != Thread.State.TERMINATED) {
                    throw new RuntimeException("Server thread doesn't want to terminate and free up our port!");
                }
            }
        }
        throw new RuntimeException("Test timed out!");
    }

    /**
     * It can take awhile for a new hidden service to get registered
     * @param onionAddress DNS style hidden service address of from x.onion
     * @param hiddenServicePort Hidden service's port
     * @param socksPort Socks port that Tor OP is listening on
     * @return A socket via Tor that is connected to the hidden service
     */
    private Socket getClientSocket(String onionAddress, int hiddenServicePort, int socksPort)
            throws InterruptedException {
        long timeToExit = Calendar.getInstance().getTimeInMillis() + WAIT_FOR_HIDDEN_SERVICE_MINUTES *60*1000;
        Socket clientSocket = null;
        while (Calendar.getInstance().getTimeInMillis() < timeToExit && clientSocket == null) {
            try {
                clientSocket = Utilities.socks4aSocketConnection(onionAddress, hiddenServicePort, "127.0.0.1", socksPort);
                clientSocket.setTcpNoDelay(true);
                LOG.info("We connected via the clientSocket to try and talk to the hidden service.");
            } catch (IOException e) {
                LOG.error("attempt to set clientSocket failed, will retry", e);
                    Thread.sleep(5000, 0);
            }
        }

        if (clientSocket == null) {
            throw new RuntimeException("Could not set clientSocket");
        }

        return clientSocket;
    }

    private Thread receiveExpectedBytes(final byte[] expectedBytes, int localPort,
                                                final SynchronousQueue<ServerState> serverQueue) throws IOException {
        final ServerSocket serverSocket = new ServerSocket(localPort);

        Thread thread = new Thread(new Runnable() {
            public void run() {
                Socket receivedSocket = null;
                try {
                    receivedSocket = serverSocket.accept();
                    // Yes, setTcpNoDelay is useless because we are just reading but I'm being paranoid
                    receivedSocket.setTcpNoDelay(true);
                    receivedSocket.setSoTimeout(10*1000);
                    LOG.info("Received incoming connection");
                    DataInputStream dataInputStream = new DataInputStream(receivedSocket.getInputStream());
                    for(byte nextByte : expectedBytes) {
                        byte receivedByte = dataInputStream.readByte();
                        if (nextByte != receivedByte) {
                            LOG.error("Received " + receivedByte + ", but expected " + nextByte);
                            serverQueue.put(ServerState.OTHERERROR);
                            return;
                        } else {
                            LOG.info("Received " + receivedByte);
                        }
                    }
                    LOG.info("All Bytes Successfully Received!");
                    serverQueue.put(ServerState.SUCCESS);
                } catch(IOException e) {
                    LOG.warn("We got an io exception waiting for the server bytes, this really shouldn't happen, but does.", e);
                    try {
                        serverQueue.put(ServerState.TIMEDOUT);
                    } catch (InterruptedException e1) {
                        LOG.error("We couldn't send notice that we had a server time out! EEEK!");
                    }
                } catch (InterruptedException e) {
                    LOG.error("Test Failed", e);
                    try {
                        serverQueue.put(ServerState.OTHERERROR);
                    } catch (InterruptedException e1) {
                        LOG.error("We got an InterruptedException and couldn't tell the server queue about it!", e1);
                    }
                } finally {
                    // I suddenly am getting IncompatibleClassChangeError: interface no implemented when
                    // calling these functions. I saw a random Internet claim (therefore it must be true!)
                    // that closeable is only supported on sockets in API 19 but I'm compiling with 19 (although
                    // targeting 18). To make things even weirder, this test passed before!!! I am soooo confused.
                    try {
                        if (receivedSocket != null) {
                            receivedSocket.close();
                            LOG.info("Close of receiveExpectedBytes worked");
                        }
                        serverSocket.close();
                        LOG.info("Server socket is closed");
                    } catch (IOException e) {
                        LOG.error("Close failed!", e);
                    }
                }
            }
        });
        thread.start();
        return thread;
    }

    private void deleteTorWorkingDirectory(File torWorkingDirectory) {
        FileUtilities.recursiveFileDelete(torWorkingDirectory);
        if (!torWorkingDirectory.mkdirs()) {
            throw new RuntimeException("couldn't create Tor Working Directory after deleting it.");
        }
    }
}
