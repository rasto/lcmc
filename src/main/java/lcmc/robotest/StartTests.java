/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2014, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.robotest;

import java.io.IOException;

import javax.inject.Named;
import javax.inject.Singleton;

import lcmc.cluster.domain.Cluster;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.MainPanel;
import lcmc.common.ui.main.MainData;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

@Named
@Singleton
public class StartTests {
    private static final Logger LOG = LoggerFactory.getLogger(RoboTest.class);

    private final PcmkTest1 pcmkTest1;
    private final PcmkTest2 pcmkTest2;
    private final PcmkTest3 pcmkTest3;
    private final PcmkTest4 pcmkTest4;
    private final PcmkTest5 pcmkTest5;
    private final PcmkTest6 pcmkTest6;
    private final PcmkTest7 pcmkTest7;
    private final PcmkTest8 pcmkTest8;
    private final PcmkTestA pcmkTestA;
    private final PcmkTestB pcmkTestB;
    private final PcmkTestC pcmkTestC;
    private final PcmkTestD pcmkTestD;
    private final PcmkTestE pcmkTestE;
    private final PcmkTestF pcmkTestF;
    private final PcmkTestG pcmkTestG;
    private final PcmkTestH pcmkTestH;
    private final RoboTest roboTest;
    private final DrbdTest1 drbdTest1;
    private final DrbdTest2 drbdTest2;
    private final DrbdTest3 drbdTest3;
    private final DrbdTest4 drbdTest4;
    private final DrbdTest5 drbdTest5;
    private final DrbdTest8 drbdTest8;
    private final GUITest1 guiTest1;
    private final GUITest2 guiTest2;
    private final VMTest1 vmTest1;
    private final VMTest4 vmTest4;
    private final VMTest5 vmTest5;
    private final MainData mainData;
    private final MainPanel mainPanel;
    private final Application application;

    private Cluster cluster;

    public StartTests(PcmkTest8 pcmkTest8, PcmkTest1 pcmkTest1, PcmkTest2 pcmkTest2, Application application, RoboTest roboTest,
            VMTest1 vmTest1, PcmkTest3 pcmkTest3, DrbdTest5 drbdTest5, DrbdTest2 drbdTest2, DrbdTest8 drbdTest8,
            PcmkTest4 pcmkTest4, GUITest2 guiTest2, PcmkTestE pcmkTestE, DrbdTest3 drbdTest3, GUITest1 guiTest1, VMTest5 vmTest5,
            PcmkTest5 pcmkTest5, PcmkTest6 pcmkTest6, DrbdTest4 drbdTest4, PcmkTestG pcmkTestG, PcmkTest7 pcmkTest7,
            MainPanel mainPanel, PcmkTestD pcmkTestD, DrbdTest1 drbdTest1, MainData mainData, PcmkTestF pcmkTestF,
            PcmkTestA pcmkTestA, PcmkTestB pcmkTestB, VMTest4 vmTest4, PcmkTestC pcmkTestC, PcmkTestH pcmkTestH) {
        this.pcmkTest8 = pcmkTest8;
        this.pcmkTest1 = pcmkTest1;
        this.pcmkTest2 = pcmkTest2;
        this.application = application;
        this.roboTest = roboTest;
        this.vmTest1 = vmTest1;
        this.pcmkTest3 = pcmkTest3;
        this.drbdTest5 = drbdTest5;
        this.drbdTest2 = drbdTest2;
        this.drbdTest8 = drbdTest8;
        this.pcmkTest4 = pcmkTest4;
        this.guiTest2 = guiTest2;
        this.pcmkTestE = pcmkTestE;
        this.drbdTest3 = drbdTest3;
        this.guiTest1 = guiTest1;
        this.vmTest5 = vmTest5;
        this.pcmkTest5 = pcmkTest5;
        this.pcmkTest6 = pcmkTest6;
        this.drbdTest4 = drbdTest4;
        this.pcmkTestG = pcmkTestG;
        this.pcmkTest7 = pcmkTest7;
        this.mainPanel = mainPanel;
        this.pcmkTestD = pcmkTestD;
        this.drbdTest1 = drbdTest1;
        this.mainData = mainData;
        this.pcmkTestF = pcmkTestF;
        this.pcmkTestA = pcmkTestA;
        this.pcmkTestB = pcmkTestB;
        this.vmTest4 = vmTest4;
        this.pcmkTestC = pcmkTestC;
        this.pcmkTestH = pcmkTestH;
    }

    public void startTest(final Test autoTest, final Cluster c) {
        final Type type = autoTest.getType();
        final char index = autoTest.getIndex();
        mainData.getMainFrame()
                .setSize(Tools.getDefaultInt("DrbdMC.width"), Tools.getDefaultInt("DrbdMC.height") + 50);
        cluster = c;
        roboTest.initRobot(cluster);
        roboTest.setAborted(false);
        roboTest.info("start test " + index + " in 3 seconds");
        if (cluster != null) {
            for (final Host host : roboTest.getClusterHosts()) {
                try {
                    host.getSSH().installTestFiles();
                } catch (IOException e) {
                    LOG.appError("startTest: could not install filed");
                    return;
                }
            }
            final Host firstHost = cluster.getHostsArray()[0];
            mainPanel.setTerminalPanel(firstHost.getTerminalPanel());
        }
        final Thread thread = new Thread(() -> {
            roboTest.sleepNoFactor(3000);
            if (type == Type.GUI) {
                roboTest.moveTo(30, 20);
                roboTest.leftClick();
                final int count = 200;
                if (index == '1') {
                    /* cluster wizard deadlock */
                    final long startTime = System.currentTimeMillis();
                    roboTest.info("test" + index);
                    guiTest1.start(count);
                    final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                    roboTest.info("test" + index + ", secs: " + secs);
                } else if (index == '2') {
                    /* cluster wizard deadlock */
                    final long startTime = System.currentTimeMillis();
                    roboTest.info("test" + index);
                    guiTest2.start(count);
                    final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                    roboTest.info("test" + index + ", secs: " + secs);
                }
            } else if (type == Type.PCMK) {
                startPcmkTests(index, cluster);
            } else if (type == Type.DRBD) {
                roboTest.moveToMenu(Tools.getString("Dialog.vm.Storage.Title"));
                roboTest.leftClick();
                mainPanel.expandTerminalSplitPane(MainPanel.TerminalSize.COLLAPSE);
                if (index == '0') {
                    /* all DRBD tests */
                    int i = 1;
                    final int blockDevY = roboTest.getBlockDevY();
                    while (!roboTest.isAborted()) {
                        final long startTime = System.currentTimeMillis();
                        roboTest.info("test" + index + " no " + i);
                        drbdTest1.start(cluster, blockDevY);
                        if (roboTest.isAborted()) {
                            break;
                        }
                        drbdTest2.start(cluster, blockDevY);
                        if (roboTest.isAborted()) {
                            break;
                        }
                        drbdTest3.start(cluster, blockDevY);
                        if (roboTest.isAborted()) {
                            break;
                        }
                        if (cluster.getHostsArray()[0].hasVolumes()) {
                            drbdTest4.start(cluster, blockDevY);
                            if (roboTest.isAborted()) {
                                break;
                            }
                        }
                        final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                        roboTest.info("test" + index + " no " + i + ", secs: " + secs);
                        roboTest.resetTerminalAreas();
                        i++;
                        if (cluster.getHostsArray()[0].hasVolumes()) {
                            application.setBigDRBDConf(!application.getBigDRBDConf());
                        }
                    }
                } else if (index == '1') {
                    /* DRBD 1 link */
                    int i = 1;
                    final int blockDevY = roboTest.getBlockDevY();
                    while (!roboTest.isAborted()) {
                        final long startTime = System.currentTimeMillis();
                        roboTest.info("test" + index + " no " + i);
                        drbdTest1.start(cluster, blockDevY);
                        final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                        roboTest.info("test" + index + " no " + i + ", secs: " + secs);
                        roboTest.resetTerminalAreas();
                        i++;
                        if (cluster.getHostsArray()[0].hasVolumes()) {
                            application.setBigDRBDConf(!application.getBigDRBDConf());
                        }
                    }
                } else if (index == '2') {
                    /* DRBD cancel */
                    int i = 1;
                    final int blockDevY = roboTest.getBlockDevY();
                    while (!roboTest.isAborted()) {
                        final long startTime = System.currentTimeMillis();
                        roboTest.info("test" + index + " no " + i);
                        drbdTest2.start(cluster, blockDevY);
                        final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                        roboTest.info("test" + index + " no " + i + ", secs: " + secs);
                        roboTest.resetTerminalAreas();
                        i++;
                    }
                } else if (index == '3') {
                    /* DRBD 2 resoruces */
                    int i = 1;
                    final int blockDevY = roboTest.getBlockDevY();
                    while (!roboTest.isAborted()) {
                        final long startTime = System.currentTimeMillis();
                        roboTest.info("test" + index + " no " + i);
                        drbdTest3.start(cluster, blockDevY);
                        final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                        roboTest.info("test" + index + " no " + i + ", secs: " + secs);
                        roboTest.resetTerminalAreas();
                        i++;
                        if (cluster.getHostsArray()[0].hasVolumes()) {
                            application.setBigDRBDConf(!application.getBigDRBDConf());
                        }
                    }
                } else if (index == '4') {
                    /* DRBD 2 volumes */
                    int i = 1;
                    final int blockDevY = roboTest.getBlockDevY();
                    while (!roboTest.isAborted()) {
                        final long startTime = System.currentTimeMillis();
                        roboTest.info("test" + index + " no " + i);
                        drbdTest4.start(cluster, blockDevY);
                        final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                        roboTest.info("test" + index + " no " + i + ", secs: " + secs);
                        roboTest.resetTerminalAreas();
                        i++;
                        if (cluster.getHostsArray()[0].hasVolumes()) {
                            application.setBigDRBDConf(!application.getBigDRBDConf());
                        }
                    }
                } else if (index == '5') {
                    int i = 1;
                    final int blockDevY = roboTest.getBlockDevY();
                    while (!roboTest.isAborted()) {
                        final long startTime = System.currentTimeMillis();
                        roboTest.info("test" + index + " no " + i);
                        drbdTest5.start(cluster, blockDevY);
                        if (roboTest.isAborted()) {
                            break;
                        }
                        final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                        roboTest.info("test" + index + " no " + i + ", secs: " + secs);
                        roboTest.resetTerminalAreas();
                        i++;
                    }
                } else if (index == '8') {
                    /* proxy */
                    int i = 1;
                    final int blockDevY = roboTest.getBlockDevY();
                    while (!roboTest.isAborted()) {
                        final long startTime = System.currentTimeMillis();
                        roboTest.info("test" + index + " no " + i);
                        drbdTest8.start(cluster, blockDevY);
                        final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                        roboTest.info("test" + index + " no " + i + ", secs: " + secs);
                        roboTest.resetTerminalAreas();
                        i++;
                        if (cluster.getHostsArray()[0].hasVolumes()) {
                            application.setBigDRBDConf(!application.getBigDRBDConf());
                        }
                    }
                }
            } else if (type == Type.VM) {
                roboTest.moveToMenu("VMs ");
                roboTest.leftClick();
                if (index == '1') {
                    /* VMs */
                    int i = 1;
                    while (!roboTest.isAborted()) {
                        final long startTime = System.currentTimeMillis();
                        roboTest.info("test" + index + " no " + i);
                        vmTest1.start(cluster, "vm-test" + index, 2);
                        final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                        roboTest.info("test" + index + " no " + i + ", secs: " + secs);
                        roboTest.resetTerminalAreas();
                        i++;
                    }
                } else if (index == '2') {
                    /* VMs */
                    int i = 1;
                    final String testIndex = "1";
                    while (!roboTest.isAborted()) {
                        final long startTime = System.currentTimeMillis();
                        roboTest.info("test" + index + " no " + i);
                        vmTest1.start(cluster, "vm-test" + testIndex, 10);
                        final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                        roboTest.info("test" + index + " no " + i + ", secs: " + secs);
                        roboTest.resetTerminalAreas();
                        i++;
                    }
                } else if (index == '3') {
                    /* VMs */
                    int i = 1;
                    final String testIndex = "1";
                    while (!roboTest.isAborted()) {
                        final long startTime = System.currentTimeMillis();
                        roboTest.info("test" + index + " no " + i);
                        vmTest1.start(cluster, "vm-test" + testIndex, 30);
                        final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                        roboTest.info("test" + index + " no " + i + ", secs: " + secs);
                        roboTest.resetTerminalAreas();
                        i++;
                    }
                } else if (index == '4') {
                    /* VMs dialog disabled textfields check. */
                    final long startTime = System.currentTimeMillis();
                    roboTest.info("test" + index);
                    vmTest4.start("vm-test" + index, 100);
                    final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                    roboTest.info("test" + index + ", secs: " + secs);
                    roboTest.resetTerminalAreas();
                } else if (index == '5') {
                    /* VMs */
                    int i = 1;
                    while (!roboTest.isAborted()) {
                        final long startTime = System.currentTimeMillis();
                        roboTest.info("test" + index + " no " + i);
                        vmTest5.start(cluster, "vm-test" + index, 2);
                        final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                        roboTest.info("test" + index + " no " + i + ", secs: " + secs);
                        roboTest.resetTerminalAreas();
                        i++;
                    }
                }
            }
            roboTest.info(type + " test " + index + " done");
        });
        thread.start();
    }

    private void startPcmkTests(char index, final Cluster cluster) {
        roboTest.moveToMenu(Tools.getString("ClusterBrowser.ClusterManager"));
        roboTest.leftClick();
        final int count = 200;
        if (index == '0') {
            /* all pacemaker tests */
            int i = 1;
            while (true) {
                final long startTime = System.currentTimeMillis();
                roboTest.info("test" + index + " no " + i);
                pcmkTest1.start(cluster);
                if (roboTest.isAborted()) {
                    break;
                }
                pcmkTest2.start();
                if (roboTest.isAborted()) {
                    break;
                }
                pcmkTest3.start(4);
                if (roboTest.isAborted()) {
                    break;
                }
                pcmkTest4.start();
                if (roboTest.isAborted()) {
                    break;
                }
                pcmkTest5.start(5);
                if (roboTest.isAborted()) {
                    break;
                }
                pcmkTest6.start(5);
                if (roboTest.isAborted()) {
                    break;
                }
                pcmkTest7.start(5);
                if (roboTest.isAborted()) {
                    break;
                }
                pcmkTest8.start(10);
                if (roboTest.isAborted()) {
                    break;
                }
                pcmkTestA.start(5);
                if (roboTest.isAborted()) {
                    break;
                }
                pcmkTestB.start(5);
                if (roboTest.isAborted()) {
                    break;
                }
                pcmkTestC.start(5);
                if (roboTest.isAborted()) {
                    break;
                }
                pcmkTestD.start(5);
                if (roboTest.isAborted()) {
                    break;
                }
                pcmkTestE.start(5);
                if (roboTest.isAborted()) {
                    break;
                }
                pcmkTestF.start(cluster, 2);
                if (roboTest.isAborted()) {
                    break;
                }
                pcmkTestG.start(5);
                if (roboTest.isAborted()) {
                    break;
                }
                pcmkTestH.start(5);
                if (roboTest.isAborted()) {
                    break;
                }
                final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                roboTest.info("test" + index + " no " + i + ", secs: " + secs);
                i++;
            }
        } else if (index == '1') {
            /* pacemaker */
            int i = 1;
            while (!roboTest.isAborted()) {
                final long startTime = System.currentTimeMillis();
                roboTest.info("test" + index + " no " + i);
                pcmkTest1.start(cluster);
                final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                roboTest.info("test" + index + " no " + i + ", secs: " + secs);
                roboTest.resetTerminalAreas();
                i++;
            }
        } else if (index == '2') {
            /* resource sets */
            int i = 1;
            while (!roboTest.isAborted()) {
                final long startTime = System.currentTimeMillis();
                roboTest.info("test" + index + " no " + i);
                pcmkTest2.start();
                final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                roboTest.info("test" + index + " no " + i + ", secs: " + secs);
                roboTest.resetTerminalAreas();
                i++;
            }
        } else if (index == '3') {
            /* pacemaker drbd */
            final int i = 1;
            final long startTime = System.currentTimeMillis();
            roboTest.info("test" + index + " no " + i);
            pcmkTest3.start(200);
            final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
            roboTest.info("test" + index + " no " + i + ", secs: " + secs);
        } else if (index == '4') {
            /* placeholders 6 dummies */
            int i = 1;
            while (!roboTest.isAborted()) {
                final long startTime = System.currentTimeMillis();
                roboTest.info("test" + index + " no " + i);
                pcmkTest4.start();
                final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                roboTest.info("test" + index + " no " + i + ", secs: " + secs);
                i++;
            }
        } else if (index == '5') {
            int i = 1;
            while (!roboTest.isAborted()) {
                /* pacemaker */
                final long startTime = System.currentTimeMillis();
                roboTest.info("test" + index + " no " + i);
                pcmkTest5.start(10);
                final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                roboTest.info("test" + index + " no " + i + ", secs: " + secs);
                i++;
            }
        } else if (index == '6') {
            int i = 1;
            while (!roboTest.isAborted()) {
                /* pacemaker */
                final long startTime = System.currentTimeMillis();
                roboTest.info("test" + index + " no " + i);
                pcmkTest6.start(10);
                final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                roboTest.info("test" + index + " no " + i + ", secs: " + secs);
                i++;
            }
        } else if (index == '7') {
            /* pacemaker leak test */
            final long startTime = System.currentTimeMillis();
            roboTest.info("test" + index);
            pcmkTest7.start(100);
            final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
            roboTest.info("test" + index + ", secs: " + secs);
        } else if (index == '8') {
            /* pacemaker leak test */
            final long startTime = System.currentTimeMillis();
            roboTest.info("test" + index);
            pcmkTest8.start(30);
            final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
            roboTest.info("test" + index + ", secs: " + secs);
        } else if (index == 'a') {
            /* pacemaker leak test group */
            final long startTime = System.currentTimeMillis();
            roboTest.info("test" + index);
            pcmkTestA.start(200);
            final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
            roboTest.info("test" + index + ", secs: " + secs);
        } else if (index == 'b') {
            /* pacemaker leak test clone */
            final long startTime = System.currentTimeMillis();
            roboTest.info("test" + index);
            pcmkTestB.start(200);
            final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
            roboTest.info("test" + index + ", secs: " + secs);
        } else if (index == 'c') {
            /* pacemaker master/slave test */
            final long startTime = System.currentTimeMillis();
            roboTest.info("test" + index);
            pcmkTestC.start(200);
            final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
            roboTest.info("test" + index + ", secs: " + secs);
        } else if (index == 'd') {
            /* pacemaker leak test */
            final long startTime = System.currentTimeMillis();
            roboTest.info("test" + index);
            pcmkTestD.start(count);
            final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
            roboTest.info("test" + index + ", secs: " + secs);
        } else if (index == 'e') {
            /* host wizard deadlock */
            final long startTime = System.currentTimeMillis();
            roboTest.info("test" + index);
            pcmkTestE.start(count);
            final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
            roboTest.info("test" + index + ", secs: " + secs);
        } else if (index == 'f') {
            int i = 1;
            while (!roboTest.isAborted()) {
                /* cloned group */
                final long startTime = System.currentTimeMillis();
                roboTest.info("test" + index + " no " + i);
                pcmkTestF.start(cluster, 2);
                final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
                roboTest.info("test" + index + " no " + i + ", secs: " + secs);
                i++;
            }
        } else if (index == 'g') {
            /* big group */
            final long startTime = System.currentTimeMillis();
            roboTest.info("test" + index);
            pcmkTestG.start(5);
            final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
            roboTest.info("test" + index + ", secs: " + secs);
        } else if (index == 'h') {
            /* ipmi */
            final long startTime = System.currentTimeMillis();
            roboTest.info("test" + index);
            pcmkTestH.start(15);
            final int secs = (int) (System.currentTimeMillis() - startTime) / 1000;
            roboTest.info("test" + index + ", secs: " + secs);
        }
    }

    public enum Type {
        PCMK("pcmk"), DRBD("drbd"), VM("vm"), GUI("gui");

        private final String testName;

        Type(final String name) {
            testName = "start" + name + "test";
        }

        public String getTestName() {
            return testName;
        }
    }
}
