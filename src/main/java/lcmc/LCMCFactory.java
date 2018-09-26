package lcmc;

import lcmc.cluster.domain.Cluster;
import lcmc.cluster.domain.Clusters;
import lcmc.cluster.service.NetworkService;
import lcmc.cluster.service.ssh.Authentication;
import lcmc.cluster.service.ssh.ConnectionThread;
import lcmc.cluster.service.ssh.PopupHostKeyVerifier;
import lcmc.cluster.service.ssh.Ssh;
import lcmc.cluster.service.storage.BlockDeviceService;
import lcmc.cluster.service.storage.FileSystemService;
import lcmc.cluster.service.storage.MountPointService;
import lcmc.cluster.ui.*;
import lcmc.cluster.ui.resource.ClusterViewFactory;
import lcmc.cluster.ui.resource.CommonBlockDevInfo;
import lcmc.cluster.ui.resource.FSInfo;
import lcmc.cluster.ui.resource.NetInfo;
import lcmc.cluster.ui.widget.*;
import lcmc.cluster.ui.wizard.*;
import lcmc.common.domain.Application;
import lcmc.common.domain.UserConfig;
import lcmc.common.ui.*;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.main.MainPresenter;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.common.ui.main.ProgressIndicatorPanel;
import lcmc.common.ui.treemenu.ClusterTreeMenu;
import lcmc.common.ui.treemenu.TreeMenuController;
import lcmc.common.ui.utils.*;
import lcmc.crm.ui.resource.HostInfo;
import lcmc.crm.ui.resource.HostMenu;
import lcmc.drbd.domain.DrbdXml;
import lcmc.drbd.ui.DrbdLog;
import lcmc.drbd.ui.DrbdsLog;
import lcmc.drbd.ui.ProxyHostWizard;
import lcmc.drbd.ui.configdialog.*;
import lcmc.drbd.ui.resource.*;
import lcmc.host.domain.HostFactory;
import lcmc.host.domain.Hosts;
import lcmc.host.ui.*;
import lcmc.logger.LoggerFactory;
import lcmc.lvm.ui.*;
import lcmc.robotest.*;
import lcmc.vm.domain.NetworkParser;
import lcmc.vm.domain.VMCreator;
import lcmc.vm.domain.VMParser;
import lcmc.vm.domain.VmsXml;
import lombok.Getter;
import lombok.val;

import java.util.function.Supplier;

@Getter
public class LCMCFactory {
    private Application application;
    private CheckInstallation checkInstallation;
    private TerminalPanel terminalPanel;
    private MainMenu mainMenu;
    private ClustersPanel clustersPanel;
    private HostFactory hostFactory;
    private UserConfig userConfig;
    private ClusterTabFactory clusterTabFactory;
    private MainPanel mainPanel;
    private Supplier<Cluster> clusterProvider;
    private Supplier<EmptyViewPanel> emptyViewPanelProvider;
    private ProxyCheckInstallation proxyCheckInstallationDialog;

    public LCMC createLCMC() {
        val allHosts = new Hosts();
        val allClusters = new Clusters();
        val swingUtils = new SwingUtils();
        val access = new Access(allClusters);

        val widgetFactory = createWidgetFactory(swingUtils, access);
        val mainData = new MainData(swingUtils);
        final Supplier<ConfirmDialog> confirmDialogProvider = () -> new ConfirmDialog(application, swingUtils , widgetFactory, mainData);
        application = new Application(allHosts, allClusters, confirmDialogProvider);
        val roboTest = createRoboTest(mainData);
        val hwEventBus = new HwEventBus();
        val clusterEventBus = new ClusterEventBus();
        final Supplier<NetInfo> netInfoProvider = NetInfo::new;
        final Supplier<FSInfo> fsInfoProvider = FSInfo::new;
        final Supplier<CommonBlockDevInfo> commonBlockDevInfoProvider = CommonBlockDevInfo::new;
        val clusterViewFactory = new ClusterViewFactory(netInfoProvider, fsInfoProvider , commonBlockDevInfoProvider);
        val blockDeviceService = new BlockDeviceService(hwEventBus, clusterEventBus , clusterViewFactory);

        val mountPointService = new MountPointService(hwEventBus, clusterEventBus);
        val fileSystemService = new FileSystemService(hwEventBus, clusterEventBus);
        val networkService = new NetworkService(hwEventBus, clusterEventBus);

        val networkParser = new NetworkParser();
        val vmParser = new VMParser();
        final Supplier<VMCreator> vmCreatorProvider = VMCreator::new;
        final Supplier<VmsXml> vmsXmlProvider = () -> new VmsXml(networkParser, vmParser, vmCreatorProvider);

        val emptyBrowser = new EmptyBrowser();
        val clusterPresenter = new ClusterPresenter(emptyBrowser);
        val clusterViewPanel = new ClusterViewPanel(swingUtils, clusterBrowser, mainData);
        final Supplier<ClusterTab> clusterTabProvider = () -> new ClusterTab(emptyViewPanelProvider, clusterPresenter , clusterViewPanel , widgetFactory);
        clusterProvider = () -> new Cluster(mainData, swingUtils , blockDeviceService);

        Supplier<ClustersPanel> clustersPanelProvider = () -> clustersPanel;
        clusterTabFactory = new ClusterTabFactory(clustersPanelProvider, clusterTabProvider);
        final Supplier<HostFactory> hostFactoryProvider = () -> hostFactory;
        mainPanel = new MainPanel(clustersPanelProvider, hostFactoryProvider, swingUtils);
        val progressIndicatorPanel = new ProgressIndicatorPanel(widgetFactory, mainPanel, mainData);
        val progressIndicator = new ProgressIndicator(progressIndicatorPanel, () -> mainMenu);
        userConfig = new UserConfig(clusterTabFactory,
                hostFactoryProvider,
                clustersPanelProvider,
                clusterProvider,
                application,
                swingUtils,
                allHosts,
                allClusters);
        val mainPresenter = new MainPresenter(
                swingUtils,
                application,
                progressIndicator,
                userConfig,
                mainData);
        clustersPanel = new ClustersPanel(
                clusterTabFactory,
                userConfig,
                mainData,
                mainPresenter,
                application);
        final Supplier<DrbdXml> drbdXmlProvider = () -> new DrbdXml(access, progressIndicator);
        final DrbdTest1 drbdTest1 = new DrbdTest1(roboTest);
        final VMTest1 vmTest1 = new VMTest1(roboTest, mainPanel);
        final StartTests startTests = new StartTests(roboTest, mainData, mainPanel,
                application,
                new PcmkTest1(roboTest),
                new PcmkTest2(roboTest),
                new PcmkTest3(roboTest),
                new PcmkTest4(roboTest),
                new PcmkTest5(roboTest),
                new PcmkTest6(roboTest),
                new PcmkTest7(roboTest),
                new PcmkTest8(roboTest),
                new PcmkTestA(roboTest),
                new PcmkTestB(roboTest),
                new PcmkTestC(roboTest),
                new PcmkTestD(roboTest),
                new PcmkTestE(roboTest),
                new PcmkTestF(roboTest),
                new PcmkTestG(roboTest),
                new PcmkTestH(roboTest),
                drbdTest1,
                new DrbdTest2(roboTest, drbdTest1),
                new DrbdTest3(roboTest, drbdTest1, mainPanel),
                new DrbdTest4(roboTest, drbdTest1),
                new DrbdTest5(roboTest, drbdTest1),
                new DrbdTest8(roboTest, drbdTest1, mainPanel),
                new GUITest1(roboTest),
                new GUITest2(roboTest),
                vmTest1,
                new VMTest4(roboTest),
                new VMTest5(vmTest1));

        final Supplier<PopupHostKeyVerifier> popupHostKeyVerifierProvider = () -> new PopupHostKeyVerifier(application);
        final Supplier<Authentication> authenticationProvider = () -> new Authentication(application, popupHostKeyVerifierProvider);
        final Supplier<ConnectionThread> connectionThreadProvider = () -> new ConnectionThread(
                application,
                swingUtils,
                popupHostKeyVerifierProvider);
        final Supplier<Ssh> sshProvider = () -> new Ssh(
                connectionThreadProvider,
                mainData,
                mainPanel,
                progressIndicator,
                application,
                swingUtils,
                authenticationProvider);
        final Supplier<HostLogs> hostLogsProvider = () -> new HostLogs(
                application,
                swingUtils,
                widgetFactory,
                mainData);
        final Supplier<ProgressBar> progressBarProvider = () -> new ProgressBar(swingUtils, widgetFactory);
        val distDetection = new DistDetection(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                checkInstallation);
        val devices = new Devices(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                distDetection);
        val sshDialog = new SSH(progressBarProvider, application, swingUtils, widgetFactory, mainData, devices);
        val configuration = new Configuration(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                devices,
                sshDialog);
        val editHostDialog = new EditHostDialog(sshDialog, mainPanel);
        final Supplier<MyMenuItem> menuItemProvider = () -> new MyMenuItem(application, swingUtils, access);
        final Supplier<MyMenu> menuProvider = () -> new MyMenu(application, access);
        val menuFactory = new MenuFactory(menuItemProvider, menuProvider);
        val hostMenu = new HostMenu(editHostDialog, mainData , mainPresenter , menuFactory, application, hostLogsProvider);
        val hostInfo = new HostInfo(
                hostMenu,
                widgetFactory,
                application,
                swingUtils,
                access,
                mainData);
        val proxyInstDialog = new ProxyInst(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                () -> proxyCheckInstallationDialog);
        proxyCheckInstallationDialog = new ProxyCheckInstallation(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                proxyInstDialog);
        val devicesProxyDialog = new DevicesProxy(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                distDetection,
                proxyCheckInstallationDialog);
        val sshProxyDialog = new SSHProxy(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                devices,
                devicesProxyDialog);
        val configurationProxy = new ConfigurationProxy(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                devices,
                sshDialog,
                sshProxyDialog);
        val newProxyHostDialog = new NewProxyHostDialog(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                configuration,
                mainPanel,
                configurationProxy);
        val proxyHostWizard = new ProxyHostWizard(mainPanel, newProxyHostDialog);
        val bugReport = new BugReport(application, swingUtils, widgetFactory, mainData, allClusters);
        val loggerFactory = new LoggerFactory(mainData, mainMenu, bugReport);
        final Supplier<DrbdsLog> drbdsLogProvider = () -> new DrbdsLog(application, swingUtils, widgetFactory, mainData);
        final Supplier<VGCreate> vgCreateProvider = () -> new VGCreate(progressBarProvider, application, swingUtils, widgetFactory, mainData);
        final Supplier<VGRemove> vgRemoveProvider = () -> new VGRemove(progressBarProvider, application, swingUtils, widgetFactory, mainData);
        final Supplier<LVCreate> lvCreateProvider = () -> new LVCreate(progressBarProvider, application, swingUtils, widgetFactory, mainData);
        final Supplier<LVResize> lvResizeProvider = () -> new LVResize(progressBarProvider, application, swingUtils, widgetFactory, mainData, loggerFactory);
        final Supplier<LVSnapshot> lvSnapshotProvider = () -> new LVSnapshot(progressBarProvider, application, swingUtils, widgetFactory, mainData);
        val hostDrbdMenu = new HostDrbdMenu(
                editHostDialog,
                mainData,
                mainPresenter,
                proxyHostWizard,
                menuFactory,
                application,
                vgCreateProvider,
                lvCreateProvider,
                drbdsLogProvider);
        val hostDrbdInfo = new HostDrbdInfo(
                application,
                swingUtils,
                access,
                mainData,
                hostDrbdMenu,
                widgetFactory);
        val treeMenuController = new TreeMenuController(swingUtils);
        val clusterTreeMenu = new ClusterTreeMenu(treeMenuController);

        final Supplier<DrbdLog> drbdLogProvider = () -> new DrbdLog(application, swingUtils, widgetFactory, mainData);
        val blockDevMenu = new BlockDevMenu(
                progressIndicator,
                menuFactory,
                application,
                vgCreateProvider,
                vgRemoveProvider,
                lvCreateProvider,
                lvResizeProvider,
                lvSnapshotProvider,
                drbdLogProvider,
                access);
        final Supplier<BlockDevInfo> blockDevInfoFactory = () -> new BlockDevInfo(
                application,
                swingUtils,
                access,
                mainData,
                widgetFactory,
                mainPanel,
                blockDevMenu,
                clusterTreeMenu);
        final Supplier<HostBrowser> hostBrowserProvider = () -> {
            val netInterfacesCategory = new CategoryInfo(
                    application,
                    swingUtils,
                    access,
                    mainData);
            val blockDevicesCategory = new CategoryInfo(
                    application,
                    swingUtils,
                    access,
                    mainData);
            val fileSystemsCategory = new CategoryInfo(
                    application,
                    swingUtils,
                    access,
                    mainData);

            final Supplier<CmdLog> cmdLogProvider = CmdLog::new;

            return new HostBrowser(
                    application,
                    hostInfo,
                    hostDrbdInfo,
                    progressIndicator,
                    blockDevInfoFactory,
                    swingUtils,
                    menuFactory,
                    cmdLogProvider,
                    netInterfacesCategory,
                    blockDevicesCategory,
                    fileSystemsCategory,
                    treeMenuController,
                    clusterEventBus,
                    clusterViewFactory);
        };
        final Supplier<TerminalPanel> terminalPanelProvider = () -> terminalPanel;

        hostFactory = new HostFactory(
                hwEventBus,
                swingUtils,
                application,
                mainData,
                progressIndicator,
                allHosts,
                roboTest,
                blockDeviceService,
                vmsXmlProvider,
                drbdXmlProvider,
                terminalPanelProvider,
                sshProvider,
                hostBrowserProvider);

        val finishDialog = new Finish(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                emptyBrowser,
                userConfig);
        val initClusterDialog = new InitCluster(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                finishDialog,
                access);
        val hbConfigDialog = new HbConfig(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                initClusterDialog,
                networkService,
                access);
        val coroConfigDialog = new CoroConfig(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                initClusterDialog,
                networkService,
                access);
        val commStackDialog = new CommStack(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                hbConfigDialog,
                coroConfigDialog);
        val connectDialog = new Connect(progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                commStackDialog);
        val clusterHosts = new ClusterHosts(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                commStackDialog,
                connectDialog,
                mainPresenter,
                allHosts);
        val nameDialog = new Name(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                clusterHosts,
                clusterTabFactory,
                mainPresenter,
                allClusters);
        final Supplier<AddClusterDialog> addClusterDialogProvider = () -> new AddClusterDialog(
                nameDialog,
                mainPresenter,
                mainPanel,
                new Cluster(mainData, swingUtils , blockDeviceService),
                application,
                swingUtils,
                allHosts);

        Supplier<NewHostDialog> newHostDialogFactory = () -> new NewHostDialog(progressBarProvider, application, swingUtils,
                widgetFactory,
                mainData,
                configuration,
                mainPanel);
        val hostFinishDialog = new HostFinish(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                hostFactory,
                addClusterDialogProvider.get(),
                mainPresenter,
                newHostDialogFactory,
                userConfig);
        final Supplier<CheckInstallation> checkInstallationProvider = () -> checkInstallation;
        val drbdCommandInstDialog = new DrbdCommandInst(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                checkInstallationProvider);
        val drbdAvailSourceFilesDialog = new DrbdAvailSourceFiles(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                drbdCommandInstDialog);
        val heartbeatInstDialog = new HeartbeatInst(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                checkInstallationProvider);
        final PacemakerInst pacemakerInstDialog = new PacemakerInst(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                checkInstallationProvider);
        checkInstallation = new CheckInstallation(
                progressBarProvider,
                application,
                swingUtils,
                widgetFactory,
                mainData,
                hostFinishDialog,
                drbdAvailSourceFilesDialog,
                drbdCommandInstDialog,
                heartbeatInstDialog,
                pacemakerInstDialog);

        val argumentParser = new ArgumentParser(
                userConfig,
                roboTest,
                clusterProvider,
                application,
                swingUtils,
                access,
                mainData);
        final Supplier<AddHostDialog> addHostDialogProvider = () -> new AddHostDialog(
                newHostDialogFactory.get(),
                mainPresenter,
                mainPanel,
                application,
                swingUtils);

        emptyViewPanelProvider = () -> new EmptyViewPanel(
                swingUtils,
                emptyBrowser,
                addClusterDialogProvider,
                addHostDialogProvider,
                hostFactory,
                mainData,
                mainPresenter,
                application,
                widgetFactory);
        val aboutDialog = new About(application, swingUtils, widgetFactory, mainData);
        val dialogs = new Dialogs(mainData, application);
        mainMenu = new MainMenu(
                addClusterDialogProvider,
                userConfig,
                addHostDialogProvider,
                hostFactory,
                mainData,
                mainPresenter,
                application,
                swingUtils,
                bugReport,
                aboutDialog,
                dialogs,
                access);
        terminalPanel = new TerminalPanel(
                roboTest,
                mainMenu,
                mainData,
                progressIndicator,
                application,
                swingUtils,
                startTests,
                access);
        new ClusterBrowser(application, swingUtils, domainInfoProvider, connectionInfoProvider, availableServiceInfoProvider, drbdXmlProvider,
                clusterStatusProvider, networksCategory, resourceAgentClassInfoProvider, availableServicesInfo,
                crmInfoProvider, vmsXmlProvider, clusterTreeMenu, clusterEventBus, networkService, networkFactory,
                resourceUpdaterProvider, crmGraph, drbdGraph, crmXml, access, clusterHostsInfo, servicesInfo, rscDefaultsInfoProvider,
                progressIndicator, globalInfo, CategoryInfo resourcesCategory, VMListInfo vmListInfo);

        return new LCMC(
                application,
                argumentParser,
                mainPanel,
                mainMenu,
                progressIndicator,
                mainData,
                mainPresenter,
                blockDeviceService,
                mountPointService,
                fileSystemService,
                networkService,
                swingUtils);
    }

    private RoboTest createRoboTest(MainData mainData) {
        return new RoboTest(mainData);
    }

    private WidgetFactory createWidgetFactory(SwingUtils swingUtils, Access access) {
        final Supplier<Label> labelProvider = () -> new Label(swingUtils, access);
        final Supplier<ComboBox> comboBoxProvider = () -> new ComboBox(swingUtils, access);
        final Supplier<Passwdfield> passwdFieldProvider  = () -> new Passwdfield(swingUtils, access);
        final Supplier<Textfield> textFieldInstance = () -> new Textfield(swingUtils, access);
        final Supplier<TextfieldWithUnit> textFieldWithUnitProvider = () -> new TextfieldWithUnit(swingUtils, access);
        final Supplier<RadioGroup> radioGroupProvider = () -> new RadioGroup(swingUtils, access);
        final Supplier<Checkbox> checkBoxProvider = () -> new Checkbox(swingUtils, access);

        return new WidgetFactory(
                labelProvider,
                comboBoxProvider,
                passwdFieldProvider ,
                textFieldInstance,
                textFieldWithUnitProvider,
                radioGroupProvider,
                checkBoxProvider);
    }
}
