/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DRBD; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.configs;

import java.util.Arrays;

/**
 * Here are japanese texts.
 */
public final class TextResource_ja_JP extends java.util.ListResourceBundle {

    /** Get contents. */
    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        {"DrbdMC.Title",
         "Linux Cluster Management Console"},

        /* Main Menu */
        {"MainMenu.Session",
         "セッション"},

        {"MainMenu.New",
         "新規"},

        {"MainMenu.Load",
         "開く"},

        {"MainMenu.Save",
         "保存"},

        {"MainMenu.SaveAs",
         "名前を付けて保存"},

        {"MainMenu.Host",
         "ホスト"},

        {"MainMenu.Cluster",
         "クラスター"},

        {"MainMenu.RemoveEverything",
         "すべてを削除"},

        {"MainMenu.Exit",
         "終了"},

        {"MainMenu.Settings",
         "設定"},

        {"MainMenu.LookAndFeel",
         "ルック アンド フィール"},

        {"MainMenu.Help",
         "ヘルプ"},

        {"MainMenu.About",
         "LCMCについて"},

        {"MainMenu.DrbdGuiFiles",
         "DRBD Management Console ファイル"},

        /* Main panel */
        {"MainPanel.Clusters",
         "クラスター"},

        {"MainPanel.ClustersAlt",
         "クラスター ビューを表示するにはここをクリックします"},

        {"MainPanel.Hosts",
         "ホスト"},

        {"MainPanel.HostsAlt",
         "ホスト ビューを表示するにはここをクリックします"},

        {"MainPanel.UpgradeCheck",
         "利用できるアップグレードを確認しています..."},

        {"MainPanel.UpgradeCheckDisabled",
         "アップグレードの確認が無効にされました"},

        {"MainPanel.UpgradeCheckFailed",
         "アップグレードの確認が失敗しました"},

        {"MainPanel.UpgradeAvailable",
         "<font color=black>新しいMC&nbsp;@LATEST@&nbsp;が利用できます。"
         + "</font><br><a href=\"http://lcmc.sourceforge.net/"
         + "?from-lcmc\">さあ、入手しよう</a>！"},

        {"MainPanel.NoUpgradeAvailable",
         ""},

        /** Clusters panel */
        {"ClustersPanel.ClustersTab",
         "すべてのクラスター"},

        {"ClustersPanel.ClustersTabTip",
         "すべてのクラスター"},

        /** Hosts panel */
        {"HostsPanel.NewTabTip",
         "新しいホスト"},

        /** Tools */
        {"Tools.ExecutingCommand",
         "コマンドを実行しています..."},

        {"Tools.CommandDone",
         "[終了]"},

        {"Tools.CommandFailed",
         "[失敗]"},

        {"Tools.Loading",
         "読み込んでいます..."},

        {"Tools.Saving",
         "\"@FILENAME@\" を保存しています..."},

        {"Tools.Warning.Title",
         "警告: "},

        {"Tools.sshError.command",
         "コマンド:"},

        {"Tools.sshError.returned",
         "終了コード"},

        /* Cluster tab */
        {"ClusterTab.AddNewCluster",
         "クラスターの追加 / 設定ウィザード"},

        {"ClusterTab.AddNewHost",
         "ホストの追加 / 設定ウィザード"},

        /* Cluster view panel */
        {"ClusterViewPanel.ClusterButtons",
         "クラスター"},

        {"MainMenu.OperatingMode.ToolTip",
         "操作モード"},

        /* Progress bar */
        {"ProgressBar.Cancel",
         "キャンセル"},

        /* Dialogs */
        {"Dialog.Dialog.Next",
         "次へ"},

        {"Dialog.Dialog.Back",
         "戻る"},

        {"Dialog.Dialog.Cancel",
         "キャンセル"},

        {"Dialog.Dialog.Finish",
         "完了"},

        {"Dialog.Dialog.Retry",
         "再試行"},

        {"Dialog.Dialog.PrintErrorAndRetry",
         "コマンドの実行が失敗しました。"},

        {"Dialog.Dialog.Ok",
         "OK"},

        {"Dialog.Host.NewHost.Title",
         "ホスト設定ウィザード"},

        {"Dialog.Host.NewHost.Description",
         "サーバーの<b>ホスト名/IPアドレス</b>と<b>ユーザー名</b>を入力してください。ホストにはホスト名かIPアドレスを入力できます。ホスト名はDNSで名前の解決ができるときのみ使えます。ユーザー名はSSH接続とコマンドの実行で使われます。通常は<b>root</b>ユーザーあるいはsudoを利用できるユーザーにすべきです。"
         + "<br><br>サーバーがいくつかの<b>ホップ</b>を経由していて直接到達できなければ、カンマ区切りでさらにホストを入力してください。この場合は、ホップとして同じ数のユーザー名とホスト名/IPアドレスを入力する必要があります。"},

        {"Dialog.Host.NewHost.EnterHost",
         "ホスト:"},

        {"Dialog.Host.NewHost.EnterUsername",
         "ユーザー名:"},

        {"Dialog.Host.NewHost.SSHPort",
         "SSHポート:"},

        {"Dialog.Host.NewHost.UseSudo",
         "sudoの利用:"},

        {"Dialog.Host.NewHost.EnterPassword",
         "パスワード:"},

        {"Dialog.Host.Configuration.Title",
         "ホスト設定"},

        {"Dialog.Host.Configuration.Description",
         "ホスト名の解決を行います。ホスト名の解決に失敗したら、前の画面に戻ってください。ホスト名がDNSで解決できなければ、ホスト名にはIPアドレスを入力するか、あるいは、ホスト名を解決できるようにしてください。"},

        {"Dialog.Host.Configuration.Name",
         "ノード名:"}, // TODO: this is not necessary anymore

        {"Dialog.Host.Configuration.Hostname",
         "ホスト名:"},

        {"Dialog.Host.Configuration.Ip",
         "IP:"},

        {"Dialog.Host.Configuration.DNSLookup",
         "ホスト名の解決"},

        {"Dialog.Host.Configuration.DNSLookupOk",
         "ホスト名の解決に成功しました。"},

        {"Dialog.Host.Configuration.DNSLookupError",
         "ホスト名の解決に失敗しました。"},

        {"Dialog.Host.SSH.Title",
         "SSHサーバへの接続"},

        {"Dialog.Host.SSH.Description",
         "ssh経由でホストに接続します。ポップアップ ダイアログでRSA鍵やDSA鍵やパスワードのいずれかを入力できます。入力なしにEnterキーを押すことでパスフレーズとパスワード認証を切り替えることができます。"},

        {"Dialog.Host.SSH.Connecting",
         "接続しています..."},

        {"Dialog.Host.SSH.Connected",
         "サーバに接続しました。"},

        {"Dialog.Host.SSH.NotConnected",
         "接続に失敗しました。"},

        {"Dialog.Host.Devices.Title",
         "ホスト デバイス"},

        {"Dialog.Host.Devices.Description",
         "ブロック デバイスとネットワーク デバイスの情報とホストのインストール情報を取得しています。"},

        {"Dialog.Host.Devices.Executing",
         "情報を取得しています..."},

        {"Dialog.Host.Devices.CheckError",
         "失敗しました。"},

        {"Dialog.Host.DrbdLinbitAvailPackages.Title",
         "利用できるパッケージ"},

        {"Dialog.Host.DrbdLinbitAvailPackages.Description",
         "利用できるDRBDのバイナリ パッケージを選ぶために、サーバーのディストリビューションとカーネル パッケージとCPUアーキテクチャを一致させます。何も選択されなければ、そのシステムで利用できるDRBDパッケージがありません。ディストリビューションのカーネルを使っていれば、パッケージはLINBITサポート（有償）で提供されます。再び試みた後に、このステップをやり直してください。"},

        {"Dialog.Host.DrbdLinbitAvailPackages.NotAvailable.Dist",
         "利用しているディストリビューションではDRBDパッケージを利用できません。"},

        {"Dialog.Host.DrbdLinbitAvailPackages.NotAvailable.Kernel",
         "利用しているカーネルのバージョンではDRBDパッケージを利用できません。"},

        {"Dialog.Host.DrbdLinbitAvailPackages.NotAvailable.Arch",
         "www.linbit.comでは利用しているカーネルのCPUアーキテクチャのDRBDパッケージを利用できません。"},

        {"Dialog.Host.DrbdLinbitAvailPackages.AvailablePackages",
         "www.linbit.comで利用できるパッケージ: "},

        {"Dialog.Host.DrbdLinbitAvailPackages.NoDist",
         "DRBDパッケージが見つかりません。"},

        {"Dialog.Host.DrbdLinbitAvailPackages.NotALinux",
         "オペレーティング システムを判定できません。"},

        {"Dialog.Host.DrbdLinbitAvailPackages.NoArch",
         "CPUアーキテクチャを判定できません"},

        {"Dialog.Host.DrbdLinbitAvailPackages.Executing",
         "利用できるパッケージを探しています..."},

        {"Dialog.Host.DrbdLinbitAvailPackages.AvailVersions",
         "利用できるバージョン: "},

        {"Dialog.Host.DrbdLinbitAvailPackages.NoKernels",
         "このカーネルでは利用できません"},

        {"Dialog.Host.DrbdLinbitAvailPackages.NoVersions",
         "DRBDのバージョンが見つかりません"},

        {"Dialog.Host.DrbdLinbitAvailPackages.NoDistributions",
         "ディストリビューションが見つかりません"},

        {"Dialog.Host.DrbdLinbitAvailPackages.NoArchs",
         "CPUアーキテクチャがわかりません"},

        {"Dialog.Host.DistDetection.Title",
         "ディストリビューションの検出"},

        {"Dialog.Host.DistDetection.Description",
         "そのホストのLinuxディストリビューションを検出を試みます。"
         + "Linuxであるかどうかも含めて検出します。検出できなければ、そのディストリビューションはサポートされていません。"
         + "似ているディストリビューションを選ぶことができますが、動くかどうかはわかりません。"},

        {"Dialog.Host.DistDetection.Executing",
         "実行しています..."},

        {"Dialog.Host.CheckInstallation.Title",
         "インストール環境の確認"},

        {"Dialog.Host.CheckInstallation.Description",
         "DRBDとPacemakerとその他の重要なコンポーネントがインストールされているかを確認します。"
         + "もしインストールされていない場合は、構築する環境の「インストール」ボタンを押してインストールすることができます。"
         + "DRBDを使用する場合は、「アップグレードの確認」ボタンを押してアップグレードの情報を調べることができます。"
         + "Pacemakerは、Clusterlabsのリポジトリに収録されているものが最新バージョンのパッケージとなります。"
        },

        {"Dialog.Host.CheckInstallation.Drbd.NotInstalled",
         "DRBDがインストールされていません。「インストール」ボタンを押して新しいDRBDをインストールしてください。"},

        {"Dialog.Host.CheckInstallation.Heartbeat.AlreadyInstalled",
         "Heartbeatは既にインストールされています。"},

        {"Dialog.Host.CheckInstallation.Heartbeat.NotInstalled",
         "Heartbeatがインストールされていないか、正しくインストールされていません。"
         + "「次へ」を押して、Heartbeatパッケージのインストールを行ってください。"},

        {"Dialog.Host.CheckInstallation.Heartbeat.CheckError",
         "Heartbeatの確認に失敗しました。"},

        {"Dialog.Host.CheckInstallation.Checking",
         "確認しています..."},

        {"Dialog.Host.CheckInstallation.CheckError",
         "確認に失敗しました。"},

        {"Dialog.Host.CheckInstallation.AllOk",
         "すべての必要なコンポーネントがインストールされています。"},

        {"Dialog.Host.CheckInstallation.SomeFailed",
         "いくつかの必要なコンポーネントがインストールされていません。"},

        {"Dialog.Host.CheckInstallation.DrbdNotInstalled",
         "インストールされていません"},

        {"Dialog.Host.CheckInstallation.PmNotInstalled",
         "インストールされていません"},

        {"Dialog.Host.CheckInstallation.HbPmNotInstalled",
         "インストールされていません"},

        {"Dialog.Host.CheckInstallation.DrbdUpgradeButton",
         "アップグレード"},

        {"Dialog.Host.CheckInstallation.DrbdCheckForUpgradeButton",
         "アップグレードの確認"},

        {"Dialog.Host.CheckInstallation.DrbdInstallButton",
         "インストール"},

        {"Dialog.Host.CheckInstallation.PmInstallButton",
         "インストール"},

        {"Dialog.Host.CheckInstallation.HbPmInstallButton",
         "インストール"},

        {"Dialog.Host.CheckInstallation.CheckingPm",
         "Pacemakerを確認しています..."},

        {"Dialog.Host.CheckInstallation.CheckingHbPm",
         "Pacemaker/Heartbeatを確認しています..."},

        {"Dialog.Host.CheckInstallation.CheckingDrbd",
         "DRBDを確認しています..."},

        {"Dialog.Host.CheckInstallation.InstallMethod",
         "インストール方法: "},

        {"Dialog.Host.LinbitLogin.Title",
         "ログイン"},

        {"Dialog.Host.LinbitLogin.Description",
         "利用しているディストリビューションのパッケージを取得するためにはhttp://www.linbit.com/supportのダウンロード エリアにログインする必要があります。あなたのユーザー名とパスワードを入力してください。アカウントを得るためにはLINBITサポートに問い合わせてください。別な方法としては、自身でDRBDをインストール/アップグレードすることができます。続けるために前のダイアログに戻ります。<br><br>"
         + "間違ったユーザー名やパスワードを入力してみたら、次のステップで何もダウンロードできないことがわかります。"},

        {"Dialog.Host.LinbitLogin.EnterUser",
         "ユーザー名"},

        {"Dialog.Host.LinbitLogin.EnterPassword",
         "パスワード"},

        {"Dialog.Host.LinbitLogin.Save",
         "保存"},

        {"Dialog.Host.DrbdAvailFiles.Title",
         "利用できるDRBDパッケージ"},

        {"Dialog.Host.DrbdAvailFiles.Description",
         "利用できるパッケージを検出します。ディストリビューションのカーネルを使っていれば、一つのモジュールと一つのユーティリティ パッケージになります。パッケージが自動検出できなければ、プルダウン メニューで適切なカーネル バージョンを選びます。あなたのシステムでパッケージがビルドされていなくてもよいです。その場合はLINBITサポートに問い合わせてください。できるだけ早くパッケージが提供されるでしょう。"},

        {"Dialog.Host.DrbdAvailFiles.Executing",
         "実行しています..."},

        {"Dialog.Host.DrbdAvailFiles.NoFiles",
         "パッケージが見つかりません。"},

        {"Dialog.Host.DrbdAvailFiles.NoBuilds",
         "ビルドが見つかりません。"},

        {"Dialog.Host.DrbdAvailSourceFiles.Title",
         "利用できるDRBDソースコードのtarファイル"},

        {"Dialog.Host.DrbdAvailSourceFiles.Description",
         "LINBITウェブサイトから利用できるソースコードのtarファイルを探します。インストールすべきDRBDバージョンがわからなければ、すでに選択されたものを使ってください。これは最新のものになります。"},

        {"Dialog.Host.DrbdAvailSourceFiles.Executing",
         "実行しています..."},

        {"Dialog.Host.DrbdAvailSourceFiles.NoBuilds",
         "ビルドが見つかりません"},

        {"Dialog.Host.DrbdLinbitInst.Title",
         "DRBDのインストール"},

        {"Dialog.Host.DrbdLinbitInst.Description",
         "DRBDをインストールしています。認証エラーで失敗したら、間違ったユーザー名とパスワードを入力しています。前に戻って、正しく入力して修正します。他の可能性としては、誤ったディストリビューションが選択されていて、インストールがうまく動かないことです。別の可能性としては、LINBITサーバーがダウンしていることです。しかし、LINBITサーバはDRBDにより耐障害性があるため、ほとんど起こりません。"},

        {"Dialog.Host.DrbdLinbitInst.CheckingFile",
         "インストールされるファイルを確認しています..."},

        {"Dialog.Host.DrbdLinbitInst.FileExists",
         "ファイルはすでに存在します。"},

        {"Dialog.Host.DrbdLinbitInst.Downloading",
         "ダウンロードしています..."},

        {"Dialog.Host.DrbdLinbitInst.Installing",
         "DRBDをインストールしています..."},

        {"Dialog.Host.DrbdLinbitInst.InstallationDone",
         "インストールが完了しました。"},

        {"Dialog.Host.DrbdLinbitInst.InstallationFailed",
         "インストールが失敗しました。"},

        {"Dialog.Host.DrbdLinbitInst.Executing",
         "実行しています..."},

        {"Dialog.Host.DrbdLinbitInst.Starting",
         "DRBDを開始しています..."},

        {"Dialog.Host.DrbdLinbitInst.MkdirError",
         "ディレクトリが生成できませんでした"},

        {"Dialog.Host.DrbdLinbitInst.WgetError",
         "DRBDパッケージを取得できませんでした。"},

        {"Dialog.Host.HeartbeatInst.Title",
         "Heartbeatのインストール"},

        {"Dialog.Host.HeartbeatInst.Description",
         "HeartbeatとPacemakerのパッケージをインストールしています。"},

        {"Dialog.Host.HeartbeatInst.Executing",
         "HeartbeatとPacemakerをインストールしています..."},

        {"Dialog.Host.HeartbeatInst.InstOk",
         "HeartbeatとPacemakerのインストールに成功しました。"},

        {"Dialog.Host.HeartbeatInst.InstError",
         "インストール エラー: コマンドラインに移り、修正が必要なものを修正してください。"},

        {"Dialog.Host.PacemakerInst.Title",
         "Corosync/OpenAIS/Pacemakerのインストール"},

        {"Dialog.Host.PacemakerInst.Description",
         "CorosyncやOpenAISと一緒にPacemakerパッケージをインストールしています。"},

        {"Dialog.Host.PacemakerInst.Executing",
         "Pacemakerをインストールしています..."},

        {"Dialog.Host.PacemakerInst.InstOk",
         "Pacemakerのインストールに成功しました。"},

        {"Dialog.Host.PacemakerInst.InstError",
         "インストール エラー: コマンドラインに移り、修正が必要なものを修正してください。"},

        {"Dialog.Host.DrbdCommandInst.Title",
         "DRBDのインストール"},

        {"Dialog.Host.DrbdCommandInst.Description",
         "DRBDをインストールしています。ディストリビューションによっては、現在利用できるものより古いカーネルを使うのであれば、自身で利用しているカーネルの<b>kernel-devel</b>パッケージをダウンロードしてインストールするか、カーネルをアップグレードしてください。\"uname -r\"コマンドでカーネル バージョンがわかります。再び試みた後に、このステップをやり直してください。"},

        {"Dialog.Host.DrbdCommandInst.Executing",
         "DRBDをインストールしています..."},

        {"Dialog.Host.DrbdCommandInst.InstOk",
         "DRBDのインストールに成功しました。"},

        {"Dialog.Host.DrbdCommandInst.InstError",
         "インストール エラー: コマンドラインに移り、修正が必要なものを修正してください。"},

        {"Dialog.Host.Finish.Title",
         "ホスト設定完了"},

        {"Dialog.Host.Finish.Description",
         "ホストの設定が完了しました。「他のホストの追加」を押してホストを追加するか、"
         + "「クラスター設定」を押して、クラスター設定ができます。"},

        {"Dialog.Host.Finish.AddAnotherHostButton",
         "他のホストの追加"},

        {"Dialog.Host.Finish.ConfigureClusterButton",
         "クラスター設定"},

        {"Dialog.Host.Finish.Save",
         "保存する"},

        {"Dialog.ConfigDialog.NoMatch",
         "一致しない"},

        {"Dialog.ConfigDialog.SkipButton",
         "この画面をスキップする"},

        {"Dialog.ConnectDialog.Title",
         "SSH接続"},

        {"Dialog.ConnectDialog.Description",
         "SSHサーバーとの接続を確立しています。"},

        {"Dialog.Cluster.Name.Title",
         "クラスター設定ウィザード"},

        {"Dialog.Cluster.Name.EnterName",
         "名前:"},

        {"Dialog.Cluster.Name.Description",
         "クラスターの名前を入力します。この名前はユニークであれば何でもよく、GUIで識別するためだけに使われます。後から変更できます。"},

        {"Dialog.Cluster.ClusterHosts.Title",
         "ホストの選択"},

        {"Dialog.Cluster.ClusterHosts.Description",
         "DRBD/Pacemakerクラスターを構成する2台以上のホストを選択します。"},

        {"Dialog.Cluster.Connect.Title",
         "クラスターへの接続"},
        {"Dialog.Cluster.Connect.Description",
         "クラスターのすべてのホストに接続します。"},

        {"Dialog.Cluster.CommStack.Title",
         "クラスター通信スタック"},

        {"Dialog.Cluster.CommStack.Description",
         "Corosync/OpenAISとHeartbeatの両方をインストールしていれば、どちらかを選びます。今使っているものから他のものにするには、理論上は、いつでも継ぎ目無く切り替えることができるかもしれません。Heartbeatは広く使われており、十分に試験されています。しかし、もはや活発には開発されていなく、保守のみが行われています。"},

        {"Dialog.Cluster.CoroConfig.Title",
         "Corosync/OpenAIS構成ファイル"},

        {"Dialog.Cluster.CoroConfig.Description",
         "このステップでは、Corosyncの構成ファイル(/etc/corosync/corosync.conf)やOpenAISの構成ファイル(/etc/ais/openais.conf)が生成され、OpenAISが開始されます。特別なオプションがあれば、古い構成ファイルを上書きしてはいけません。クラスターの各ホストで手動でそれを編集することができます。すべてのホストで新しい構成を保存するために「構成の生成」ボタンを押してください。"},

        {"Dialog.Cluster.CoroConfig.NextButton", "次へ / 構成を維持"},

        {"Dialog.Cluster.CoroConfig.CreateAisConfig",
         "構成の生成/上書き"},

        {"Dialog.Cluster.CoroConfig.WarningAtLeastTwoInt",
         "最低2個以上のインターフェースを設定してください。"}, // TODO: does not work so good

        {"Dialog.Cluster.CoroConfig.WarningAtLeastTwoInt.OneMore",
         "最低2個以上のインターフェースを設定してください：残り1個以上"},

        {"Dialog.Cluster.CoroConfig.RemoveIntButton",
         "削除"},

        {"Dialog.Cluster.CoroConfig.AddIntButton",
         "追加"},

        {"Dialog.Cluster.CoroConfig.UseDopdCheckBox.ToolTip",
         "DRBD Peer Outdaterを使います"},

        {"Dialog.Cluster.CoroConfig.UseDopdCheckBox",
         ""},

        {"Dialog.Cluster.CoroConfig.UseMgmtdCheckBox.ToolTip",
         "Pacemaker-GUIを使いたければ、mgmtdを使います"},

        {"Dialog.Cluster.CoroConfig.UseMgmtdCheckBox",
         ""},

        {"Dialog.Cluster.CoroConfig.NoConfigFound",
         ": 見つかりません"},

        {"Dialog.Cluster.CoroConfig.ConfigsNotTheSame",
         "構成ファイルはすべてのホストで同じではありません"},

        {"Dialog.Cluster.CoroConfig.Loading",
         "読み込んでいます..."},

        {"Dialog.Cluster.CoroConfig.CurrentConfig",
         "現在の構成:"},

        {"Dialog.Cluster.CoroConfig.Interfaces",
         "インターフェース:"},

        {"Dialog.Cluster.CoroConfig.ais.conf.ok",
         "はすべてのノードで同じ"},

        {"Dialog.Cluster.CoroConfig.Checkbox.EditConfig",
         "新しい構成を編集する"},

        {"Dialog.Cluster.CoroConfig.Checkbox.SeeExisting",
         "既存の構成を表示する"},

        {"Dialog.Cluster.HbConfig.Title",
         "Heartbeatの初期化"},

        {"Dialog.Cluster.HbConfig.Description",
         "このステップでは、Heartbeatの構成ファイル(/etc/ha.d/ha.cf)が生成され、Heartbeatが開始します。特別なオプションがあれば、古い構成ファイルを上書きしてはいけません。クラスターの各ホストで手動でそれを編集することができます。すべてのホストで新しい構成を保存するために「Heartbeat構成の生成」ボタンを押してください。また、ノード自身のホストのucastアドレスを持つことはOKです。これは同時にすべてのホストで同じ構成ファイルを持てるようにします。"},

        {"Dialog.Cluster.HbConfig.NextButton", "次へ / 構成を維持"},

        {"Dialog.Cluster.HbConfig.CreateHbConfig",
         "Heartbeat構成の生成/上書き"},

        {"Dialog.Cluster.HbConfig.WarningAtLeastTwoInt",
         "最低2個以上のインターフェースを設定してください。"},

        {"Dialog.Cluster.HbConfig.WarningAtLeastTwoInt.OneMore",
         "最低2個以上のインターフェースを設定してください：残り1個以上"},

        {"Dialog.Cluster.HbConfig.RemoveIntButton",
         "削除"},

        {"Dialog.Cluster.HbConfig.AddIntButton",
         "追加"},

        {"Dialog.Cluster.HbConfig.UseDopdCheckBox.ToolTip",
         "DRBD Peer Outdaterを使います"},

        {"Dialog.Cluster.HbConfig.UseDopdCheckBox",
         ""},

        {"Dialog.Cluster.HbConfig.UseMgmtdCheckBox.ToolTip",
         "Pacemaker-GUIを使いたければ、mgmtdを使います"},

        {"Dialog.Cluster.HbConfig.UseMgmtdCheckBox",
         ""},

        {"Dialog.Cluster.HbConfig.NoConfigFound",
         "/etc/ha.d/ha.cf: ファイルが見つかりません"},

        {"Dialog.Cluster.HbConfig.ConfigsNotTheSame",
         "構成ファイルはすべてのホストで同じではありません"},

        {"Dialog.Cluster.HbConfig.Loading",
         "読み込んでいます..."},

        {"Dialog.Cluster.HbConfig.CurrentConfig",
         "現在の構成:"},

        {"Dialog.Cluster.HbConfig.Interfaces",
         "インターフェース:"},

        {"Dialog.Cluster.HbConfig.ha.cf.ok",
         "/etc/ha.d/ha.cf: すべてのノードで同じ"},

        {"Dialog.Cluster.HbConfig.Checkbox.EditConfig",
         "新しい構成を編集する"},

        {"Dialog.Cluster.HbConfig.Checkbox.SeeExisting",
         "既存の構成を表示する"},

        {"Dialog.Cluster.Init.Title",
         "クラスター/DRBDの初期化"},

        {"Dialog.Cluster.Init.Description",
         "クラスター/DRBDの初期化。ここでは、DRBDをロードして、Corosync(OpenAIS)かHeartbeatを開始することができます。"},

        {"Dialog.Cluster.Init.CheckingDrbd",
         "確認しています..."},

        {"Dialog.Cluster.Init.LoadDrbdButton",
         "ロードする"},

        {"Dialog.Cluster.Init.CheckingPm",
         "確認しています..."},

        {"Dialog.Cluster.Init.StartCsAisButton",
         "開始"},

        {"Dialog.Cluster.Init.CsAisButtonRc",
         "自動起動させる"},

        {"Dialog.Cluster.Init.CsAisButtonSwitch",
         "Corosyncに切替"},

        {"Dialog.Cluster.Init.CsAisIsRunning",
         "は起動している"},

        {"Dialog.Cluster.Init.CsAisIsRc",
         "は自動起動している"},

        {"Dialog.Cluster.Init.CsAisIsStopped",
         "は停止している"},

        {"Dialog.Cluster.Init.CsAisIsNotInstalled",
         "はインストールされている"},

        {"Dialog.Cluster.Init.CsAisIsNotConfigured",
         "は構成されていない"},

        {"Dialog.Cluster.Init.CheckingHb",
         "確認しています..."},

        {"Dialog.Cluster.Init.StartHbButton",
         "開始"},

        {"Dialog.Cluster.Init.HbButtonRc",
         "自動起動させる"},

        {"Dialog.Cluster.Init.HbButtonSwitch",
         "Heartbeatに切替"},

        {"Dialog.Cluster.Init.HbIsRunning",
         "Heartbeatは起動している"},

        {"Dialog.Cluster.Init.HbIsRc",
         "Heartbeatは自動起動している"},

        {"Dialog.Cluster.Init.HbIsStopped",
         "Heartbeatは停止している"},

        {"Dialog.Cluster.Init.HbIsNotInstalled",
         "Heartbeatはインストールされていない"},

        {"Dialog.Cluster.Init.HbIsNotConfigured",
         "Heartbeatは構成されていない"},

        {"Dialog.Cluster.Init.DrbdIsLoaded",
         "DRBDはロードされている"},

        {"Dialog.Cluster.Init.DrbdIsNotLoaded",
         "DRBDはロードされていない"},

        {"Dialog.About.Title",
         "Linux Cluster Management Console. Release: "},

        {"Dialog.About.Description",
         "<b> by "
         + "rasto.levrinc@gmail.com.</b><br>"
         + "(C)opyright 2011 by Rasto Levrinc.<br>"
         + "(C)opyright 2007 - 2011 by Rasto Levrinc, LINBIT HA-Solution GmbH.<br>"
         + "Please visit the website:<br><br>"
         + "http://lcmc.sourceforge.net<br>"
         + "http://github.com/rasto/lcmc<br>"
         + "Old mailing list: http://lists.linbit.com/listinfo/drbd-mc<br>" },


        {"Dialog.About.Licences",
"Linux Cluster Management Console is free software; you can redistribute it and/or\n"
+ "modify it under the terms of the GNU General Public License as published\n"
+ "by the Free Software Foundation; either version 2, or (at your option)\n"
+ "any later version.\n\n"

+ "Linux Cluster Management Console is distributed in the hope that it will be useful,\n"
+ "but WITHOUT ANY WARRANTY; without even the implied warranty of\n"
+ "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"
+ "GNU General Public License for more details.\n\n"

+ "You should have received a copy of the GNU General Public License\n"
+ "along with LCMC; see the file COPYING.  If not, write to\n"
+ "the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.\n\n"

+ "This software uses the following libraries:\n"
+ "* JUNG, which is released under the terms of the BSD License\n"
+ "* Trilead SSH for Java, released under a BSD style License\n"
+ "* colt, released partly under a CERN permissive license and the LGPL\n"
+ "* bcel, released under the terms of the Apache License\n"
+ "* commons, released under the terms of the Apache License\n"
+ "* muse, released under the terms of the Apache License\n"
+ "* xalan, released under the terms of the Apache License\n"
+ "* xml, released under the terms of the Apache License\n"
+ "* tightvnc, released under the terms of the GPL License\n"
+ "* ultravnc, released under the terms of the GPL License\n"
+ "* realvnc, released under the terms of the GPL License\n"
         },


        {"Dialog.Cluster.Finish.Title",
         "完了"},

        {"Dialog.Cluster.Finish.Description",
         "クラスターの構成が完了しました。クラスター ビューでメニューからDRBDとPacemakerを構成することができます。"},

        {"Dialog.Cluster.Finish.Save",
         "保存する"},

        {"Dialog.DrbdConfig.Start.Title",
         "DRBDボリュームの構成"},

        {"Dialog.DrbdConfig.Start.Description",
         "DRBDリソースを生成するのか、既存のDRBDリソースにボリュームを追加するのかを選びます。"},

        {"Dialog.DrbdConfig.Start.DrbdResource",
         "DRBDリソース"},

        {"Dialog.DrbdConfig.Start.NewDrbdResource",
         "新しいDRBDリソース"},

        {"Dialog.DrbdConfig.Resource.Title",
         "DRBDリソースの構成"},

        {"Dialog.DrbdConfig.Resource.Description",
         "新しいDRBDリソースを構成します。リソースの<b>名前</b>を入力します。名前はユニークであれば何でもよいです。DRBDデバイスについても同じです。この<b>デバイス</b>は/dev/drbdXの形式になります。DRBDがレプリケーションのために使う<b>プロトコル</b>を選びます。プロトコル（レプリケーション モード）については<a href=\"http://www.drbd.org/docs/introduction/\">DRBD User's Guide: Introduction to DRBD</a>で学ぶことができます。その欄を変更した後、あるいはデフォルトでよければ、<b>次へ</b>を押して、続けてます。"},

        {"Dialog.DrbdConfig.Volume.Title",
         "DRBDボリュームの構成"},

        {"Dialog.DrbdConfig.Volume.Description",
         "新しいDRBDボリュームを構成します。 "
         + "<b>デバイス</b>は/dev/drbdXの形式になります。 "},

        {"Dialog.DrbdConfig.BlockDev.Title",
         "DRBDブロック デバイスの構成"},

        {"Dialog.DrbdConfig.BlockDev.Description",
         "DRBDブロック デバイスについての情報を入力します。DRBD通信のために使われるネットワーク インターフェースとポート番号を選びます。ポート番号は他のサービスで使用したり、別のDRBDブロック デバイスで使用したりしてはいけません。ネットワーク インターフェースはPacemakerにより使われているものとは異なるものにするべきです。DRBDメタ データが書き込まれる場所を選びます。単純にするためにinternalを選ぶか、速くするためにexternal DRBDメタディスクを選ぶことができます。"},

        {"Dialog.DrbdConfig.CreateFS.Title",
         "DRBDブロック デバイスの初期化"},

        {"Dialog.DrbdConfig.CreateFS.Description",
         "このステップでは、DRBDクラスターを初期化して、開始することができます。プライマリー ホストとして1つのホストを選びます。その上にファイルシステムを生成します。この場合はプライマリーとして1つのホストを選び、ファイルシステムを選んで「ファイルシステムの生成」ボタンを押します。"},

        {"Dialog.DrbdConfig.CreateFS.NoHostString",
         "なし"},

        {"Dialog.DrbdConfig.CreateFS.ChooseHost",
         "ホスト (プライマリー)"},

        {"Dialog.DrbdConfig.CreateFS.Filesystem",
         "ファイルシステム"},

        {"Dialog.DrbdConfig.CreateFS.SkipSync",
         "初期完全同期を省きます"},

        {"Dialog.DrbdConfig.CreateFS.SelectFilesystem",
         "既存データを使います"},

        {"Dialog.DrbdConfig.CreateFS.CreateFsButton",
         "ファイルシステムの生成"},

        {"Dialog.DrbdConfig.CreateFS.MakeFS",
         "ファイルシステムを生成しています..."},

        {"Dialog.DrbdConfig.CreateFS.MakeFS.Done",
         "ファイルシステムが生成されました。"},

        {"Dialog.DrbdConfig.CreateMD.Title",
         "DRBDメタデータの生成"},

        {"Dialog.DrbdConfig.CreateMD.Description",
         "このステップでは、新しいメタデータを生成できます。すでに存在していれば、上書きしたり、既存のものを使ったりできます。"},

        {"Dialog.DrbdConfig.CreateMD.Metadata",
         "メタデータ"},

        {"Dialog.DrbdConfig.CreateMD.UseExistingMetadata",
         "既存のメタデータを使う"},

        {"Dialog.DrbdConfig.CreateMD.CreateNewMetadata",
         "新しいメタデータを生成する"},

        {"Dialog.DrbdConfig.CreateMD.CreateNewMetadataDestroyData",
         "新しいメタデータを生成し、データを破壊する"},

        {"Dialog.DrbdConfig.CreateMD.OverwriteMetadata",
         "メタデータを上書きする"},

        {"Dialog.DrbdConfig.CreateMD.CreateMDButton",
         "メタデータの生成"},

        {"Dialog.DrbdConfig.CreateMD.OverwriteMDButton",
         "メタデータの上書き"},

        {"Dialog.DrbdConfig.CreateMD.CreateMD.Done",
         "@HOST@上のメタデータは生成されました。"  },

        {"Dialog.DrbdConfig.CreateMD.CreateMD.Failed",
         "@HOST@上のメタデータを生成できませんでした。"  },

        {"Dialog.DrbdConfig.CreateMD.CreateMD.Failed.40",
         "メタデータ用の場所をとれないため、@HOST@上のメタデータを生成できませんでした。プルダウン メニューから「新しいメタデータを生成し、データを破壊する」を選んでファイルシステムを破壊するか、手動でファイルシステムをリサイズするか、外部メタデータを使うかを行うことができます。"},

        {"Dialog.Drbd.SplitBrain.Title",
         "DRBDスプリット ブレインの解決"},

        {"Dialog.Drbd.SplitBrain.Description",
         "スプリット ブレインの状態が検出されました。2つ以上のノードが互いのノードについて知らずに同じDRBDブロック デバイスに書き込んでいます。新しいデータあるいは正しいデータと思うホストを選んでください。このブロック デバイスの他のホストのデータは破棄されることに注意してください。"},

        {"Dialog.Drbd.SplitBrain.ChooseHost",
         "ホスト: "},

        {"Dialog.Drbd.SplitBrain.ResolveButton",
         "解決"},

        {"Dialog.HostLogs.Title",
         "ログ ビューアー"},

        {"Dialog.ClusterLogs.Title",
         "ログ ビューアー"},

        {"Dialog.Logs.RefreshButton",
         "更新"},

        {"AppError.Title",
         "アプリケーション エラー"},

        {"AppError.Text",
         "アプリケーション内にエラーが検出されました。修正するために次の情報を私たちに送ってください。\n"},

        {"Clusters.DefaultName",
         "Cluster "},

        {"ConfirmDialog.Title",
         "確認ダイアログ"},

        {"ConfirmDialog.Description",
         "本当によいですか？"},

        {"ConfirmDialog.Yes",
         "はい"},

        {"ConfirmDialog.No",
         "いいえ"},

        {"Dialog.vm.Domain.Title",
         "新しい仮想マシンの生成"},

        {"Dialog.vm.Domain.Description",
         "このステップでは、新しい仮想マシンを生成できます..."},

        {"Dialog.vm.InstallationDisk.Title",
         "インストール ディスク"},

        {"Dialog.vm.InstallationDisk.Description",
         "インストール ディスクあるいはイメージファイルを選びます..."},

        {"Dialog.vm.Storage.Title",
         "ストレージ"},

        {"Dialog.vm.Storage.Description",
         "この仮想マシンのストレージを入力します。"},

        {"Dialog.vm.Network.Title",
         "ネットワーク インターフェースの構成"},

        {"Dialog.vm.Network.Description",
         "この仮想マシンのネットワーク インターフェースを入力します。"},

        {"Dialog.vm.Display.Title",
         "リモート ディスプレイ"},

        {"Dialog.vm.Display.Description",
         "リモート ディスプレイを構成します。"},

        {"Dialog.vm.Finish.Title",
         "ドメインの開始"},

        {"Dialog.vm.Finish.Description",
         "ドメインとビューアーを開始します。"},

        {"EmptyBrowser.LoadMarkedClusters",
         "クラスターに接続"},

        {"EmptyBrowser.LoadMarkedClusters.ToolTip",
         "GUI画面でチェックしたクラスターに接続します。"},

        {"EmptyBrowser.UnloadMarkedClusters",
         "クラスターの切断"},

        {"EmptyBrowser.UnloadMarkedClusters.ToolTip",
         "GUI画面でチェックしたクラスターから切断します。"},

        {"EmptyBrowser.RemoveMarkedClusters",
         "クラスターの削除"},

        {"EmptyBrowser.RemoveMarkedClusters.ToolTip",
         "GUI画面からチェックしたクラスターを削除します。"},

        {"EmptyBrowser.LoadClusterButton",
         "接続"},

        {"EmptyBrowser.NewHostWizard",
         "ホスト設定ウィザード"},

        {"EmptyBrowser.confirmRemoveMarkedClusters.Title",
         "クラスターの削除"},

        {"EmptyBrowser.confirmRemoveMarkedClusters.Desc",
         "以下のクラスターを削除します。本当によいですか？<br>@CLUSTERS@"},

        {"EmptyBrowser.confirmRemoveMarkedClusters.Yes",
         "削除"},

        {"EmptyBrowser.confirmRemoveMarkedClusters.No",
         "キャンセル"},

        {"Browser.ActionsMenu",
         "アクション"},

        {"Browser.Resources",
         "リソース"},

        {"Browser.ParamDefault",
         "デフォルト値: "},

        {"Browser.ParamType",
         "タイプ: "},

        {"Browser.AdvancedMode",
         "拡張モード"},

        {"Browser.ApplyResource",
         "適用"},

        {"Browser.ApplyGroup",
         "グループの適応"},

        {"Browser.CommitResources",
         "すべて適用する"},

        {"Browser.ApplyDRBDResource",
         "適用"},

        {"Browser.RevertResource",
         "戻す"},

        {"Browser.RevertResource.ToolTip",
         "適応していない変更を元に戻します"},

        {"ClusterBrowser.Host.Disconnected",
         "切断されました"},

        {"ClusterBrowser.AdvancedSubmenu",
         "拡張オプション"},

        {"ClusterBrowser.MigrateSubmenu",
         "マイグレーション オプション"},

        {"ClusterBrowser.Host.Offline",
         "オフライン"},

        {"ClusterBrowser.confirmRemoveAllServices.Title",
         "すべてのサービスの削除"},

        {"ClusterBrowser.confirmRemoveAllServices.Description",
         "すべてのサービスと制約を削除します。本当によいですか？"},

        {"ClusterBrowser.confirmRemoveAllServices.Yes",
         "削除"},

        {"ClusterBrowser.confirmRemoveAllServices.No",
         "キャンセル"},

        {"ClusterBrowser.confirmRemoveDrbdResource.Title",
         "DRBDリソースの削除"},

        {"ClusterBrowser.confirmRemoveDrbdResource.Description",
         "DRBDリソース@RESOURCE@を削除します。本当によいですか？"},

        {"ClusterBrowser.confirmRemoveDrbdResource.Yes",
         "削除"},

        {"ClusterBrowser.confirmRemoveDrbdResource.No",
         "キャンセル"},

        {"ClusterBrowser.confirmRemoveService.Title",
         "サービスの削除"},

        {"ClusterBrowser.confirmRemoveService.Description",
         "サービス@SERVICE@を削除します。本当によいですか？"},

        {"ClusterBrowser.confirmRemoveService.Yes",
         "削除"},

        {"ClusterBrowser.confirmRemoveService.No",
         "キャンセル"},

        {"ClusterBrowser.confirmRemoveGroup.Title",
         "グループの削除"},

        {"ClusterBrowser.confirmRemoveGroup.Description",
         "グループ@GROUP@とそのサービス@SERVICES@を削除します。"
         + "本当によいですか？"},

        {"ClusterBrowser.confirmRemoveGroup.Yes",
         "削除"},

        {"ClusterBrowser.confirmRemoveGroup.No",
         "キャンセル"},

        {"ClusterBrowser.confirmLinbitDrbd.Title",
         "Linbit:DRBDサービスの生成"},

        {"ClusterBrowser.confirmLinbitDrbd.Description",
         "<b>良い考えではない！</b><br>"
         + "利用しているHeartbeat @VERSION@(!) は古すぎて、Linbit:DRBDリソース エージェントを正しく動かせません。"
         + "Pacemakerにアップグレードするか、<b>drbddisk</b>リソース エージェントを使うべきです。<br>"
         + "<b>本当によいですか？！<b>"},

        {"ClusterBrowser.confirmLinbitDrbd.Yes",
         "はい（クリックするな）"},

        {"ClusterBrowser.confirmLinbitDrbd.No",
         "キャンセル"},

        {"ClusterBrowser.confirmHbDrbd.Title",
         "Heartbeat:DRBDサービスの生成"},

        {"ClusterBrowser.confirmHbDrbd.Description",
         "<b>良い考えではない！</b><br>"
         + "代わりに、Linbit:DRBDリソース エージェントを使うべきです。<br>"
         + "<b>本当によいですか？！<b>"},

        {"ClusterBrowser.confirmHbDrbd.Yes",
         "はい（クリックするな）"},

        {"ClusterBrowser.confirmHbDrbd.No",
         "キャンセル"},

        {"ClusterBrowser.CreateDir.Title",
         "@DIR@が存在しない"},

        {"ClusterBrowser.CreateDir.Description",
         "マウント ポイント @DIR@ は @HOST@ に存在しません。生成しますか？"},

        {"ClusterBrowser.CreateDir.Yes",
         "生成する"},

        {"ClusterBrowser.CreateDir.No",
         "生成しない"},

        {"ClusterBrowser.UpdatingServerInfo",
         ": サーバーの情報を更新しています..."},

        {"ClusterBrowser.UpdatingVMsStatus",
         ": VMの状態を更新しています..."},

        {"ClusterBrowser.UpdatingDrbdStatus",
         ": DRBDの状態を更新しています..."},

        /* Cluster Resource View */
        {"ClusterBrowser.AllHosts",
         "すべてのホスト"},

        {"ClusterBrowser.ClusterHosts",
         "クラスター ホスト"},

        {"ClusterBrowser.Networks",
         "ネットワーク"},

        {"ClusterBrowser.CommonBlockDevices",
         "共有ディスク"},

        {"ClusterBrowser.Drbd",
         "ストレージ (DRBD)"},

        {"ClusterBrowser.ClusterManager",
         "クラスター マネージャー"},

        {"ClusterBrowser.VMs",
         "仮想マシン"},


        {"ClusterBrowser.Services",
         "サービス"},

        {"ClusterBrowser.Scores",
         "スコア"},

        {"ClusterBrowser.DrbdResUnconfigured",
         "???"},

        {"ClusterBrowser.CommonBlockDevUnconfigured",
         "???"},

        {"ClusterBrowser.ClusterBlockDevice.Unconfigured",
         "未構成"},

        {"ClusterBrowser.Ip.Unconfigured",
         "未構成"},

        {"ClusterBrowser.SelectBlockDevice",
         "選択..."},

        {"ClusterBrowser.SelectFilesystem",
         "選択..."},

        {"ClusterBrowser.SelectNetInterface",
         "選択..."},

        {"ClusterBrowser.SelectMountPoint",
         "選択..."},

        {"ClusterBrowser.None",
         "なし"},

        {"ClusterBrowser.HeartbeatId",
         "ID"},

        {"ClusterBrowser.HeartbeatProvider",
         "プロバイダー"},

        {"ClusterBrowser.ResourceClass",
         "クラス"},

        {"ClusterBrowser.Group",
         "グループ"},

        {"ClusterBrowser.HostLocations",
         "ホストの場所"},

        {"ClusterBrowser.Operations",
         "操作"},

        {"ClusterBrowser.AdvancedOperations",
         "他の操作"},

        {"ClusterBrowser.availableServices",
         "利用可能なサービス"},

        {"ClusterBrowser.ClStatusFailed",
         "<h2>クラスター状態になるのを待っています...</h2>"},

        {"ClusterBrowser.Hb.RemoveAllServices",
         "すべてのサービスの削除"},

        {"ClusterBrowser.Hb.StopAllServices",
         "すべてのサービスの停止"},

        {"ClusterBrowser.Hb.UnmigrateAllServices",
         "すべてのマイグレーション制約の削除"},

        {"ClusterBrowser.Hb.RemoveService",
         "サービスの削除"},

        {"ClusterBrowser.Hb.AddService",
         "サービスの追加"},

        {"ClusterBrowser.Hb.AddStartBefore",
         "事前に開始"},

        {"ClusterBrowser.Hb.AddDependentGroup",
         "新しい依存グループの追加"},

        {"ClusterBrowser.Hb.AddDependency",
         "新しい依存サービスの追加"},

        {"ClusterBrowser.Hb.AddGroupService",
         "グループ サービスの追加"},

        {"ClusterBrowser.Hb.AddGroup",
         "グループの追加"},

        {"ClusterBrowser.Hb.StartResource",
         "開始"},

        {"ClusterBrowser.Hb.StopResource",
         "停止"},

        {"ClusterBrowser.Hb.UpResource",
         "グループを上に移動"},

        {"ClusterBrowser.Hb.DownResource",
         "グループを下に移動"},

        {"ClusterBrowser.Hb.MigrateResource",
         "移動先"},

        {"ClusterBrowser.Hb.ForceMigrateResource",
         "強制移動先"},

        {"ClusterBrowser.Hb.MigrateFromResource",
         "移動元"},


        {"ClusterBrowser.Hb.UnmigrateResource",
         "マイグレーション制約の削除"},

        {"ClusterBrowser.Hb.ViewServiceLog",
         "サービス ログの表示"},

        {"ClusterBrowser.Hb.RemoveEdge",
         "同居制約(collocation)と順序制約(order)の削除"},

        {"ClusterBrowser.Hb.RemoveEdge.ToolTip",
         "順序制約(order)と同居制約(collocation)の依存を削除します。"},

        {"ClusterBrowser.Hb.RemoveOrder",
         "順序制約(order)の削除"},

        {"ClusterBrowser.Hb.RemoveOrder.ToolTip",
         "順序制約(order)の依存を削除します。"},

        {"ClusterBrowser.Hb.ReverseOrder",
         "順序制約(order)の逆"},

        {"ClusterBrowser.Hb.ReverseOrder.ToolTip",
         "順序制約(order)を逆にします。"},

        {"ClusterBrowser.Hb.RemoveColocation",
         "同居制約(collocation)の削除"},

        {"ClusterBrowser.Hb.RemoveColocation.ToolTip",
         "同居制約(collocation)の依存を削除します。"},

        {"ClusterBrowser.Hb.AddOrder",
         "順序制約(order)の追加"},

        {"ClusterBrowser.Hb.AddOrder.ToolTip",
         "順序制約(order)の依存を追加します。"},

        {"ClusterBrowser.Hb.AddColocation",
         "同居制約(collocation)の追加"},

        {"ClusterBrowser.Hb.AddColocation.ToolTip",
         "同居制約(collocation)の依存を追加します。"},

        {"ClusterBrowser.Hb.CleanUpFailedResource",
         "故障したリソースの再開（cleanup）"},

        {"ClusterBrowser.Hb.CleanUpResource",
         "フェイルカウントの削除（cleanup）"},

        {"ClusterBrowser.Hb.ViewLogs",
         "ログの参照"},

        {"ClusterBrowser.Hb.ResGrpMoveUp",
         "上に移動"},

        {"ClusterBrowser.Hb.ResGrpMoveDown",
         "下に移動"},

        {"ClusterBrowser.Hb.ManageResource",
         "CRMで管理する"},

        {"ClusterBrowser.Hb.UnmanageResource",
         "CRMで管理しない"},

        {"ClusterBrowser.Hb.NoInfoAvailable",
         "情報が利用できません"},

        {"ClusterBrowser.Hb.GroupStopped",
         "停止しました（グループ）"},

        {"ClusterBrowser.Hb.StartingFailed",
         "開始が失敗しました"},

        {"ClusterBrowser.Hb.Starting",
         "開始しています..."},

        {"ClusterBrowser.Hb.Stopping",
         "停止しています..."},

        {"ClusterBrowser.Hb.Enslaving",
         "スレーブにしています..."},

        {"ClusterBrowser.Hb.Migrating",
         "移動しています..."},

        {"ClusterBrowser.Hb.ColOnlySubmenu",
         "同居制約(collocation)のみ"},

        {"ClusterBrowser.Hb.OrdOnlySubmenu",
         "順序制約(order)のみ"},

        {"ClusterBrowser.Hb.ClusterWizard",
         "クラスター設定ウィザード"},


        {"ClusterBrowser.HbUpdateResources",
         "クラスター リソースを更新しています..."},

        {"ClusterBrowser.HbUpdateStatus",
         "クラスターの状態を更新しています..."},

        {"ClusterBrowser.Drbd.RemoveEdge",
         "DRBDリソースの削除"},

        {"ClusterBrowser.Drbd.RemoveEdge.ToolTip",
         "DRBDリソースを削除します"},

        {"ClusterBrowser.Drbd.ResourceConnect",
         "接続"},

        {"ClusterBrowser.Drbd.ResourceConnect.ToolTip",
         "接続します"},

        {"ClusterBrowser.Drbd.ResourceDisconnect",
         "切断"},

        {"ClusterBrowser.Drbd.ResourceDisconnect.ToolTip",
         "切断します"},

        {"ClusterBrowser.Drbd.ResourceResumeSync",
         "同期の再開"},

        {"ClusterBrowser.Drbd.ResourceResumeSync.ToolTip",
         "同期を再開します"},

        {"ClusterBrowser.Drbd.ResourcePauseSync",
         "同期の一時停止"},

        {"ClusterBrowser.Drbd.ResourcePauseSync.ToolTip",
         "同期を一時停止します"},

        {"ClusterBrowser.Drbd.ResolveSplitBrain",
         "スプリットブレインの解消"},

        {"ClusterBrowser.Drbd.ResolveSplitBrain.ToolTip",
         "スプリットブレインを解消します"},

        {"ClusterBrowser.Drbd.Verify",
         "オンライン検証"},

        {"ClusterBrowser.Drbd.Verify.ToolTip",
         "オンライン検証を開始します。"},

        {"ClusterBrowser.Drbd.ViewLogs",
         "ログの参照"},

        {"ClusterBrowser.DrbdUpdate",
         "DRBDリソースを更新しています..."},

        {"ClusterBrowser.DifferentHbVersionsWarning",
         "<i>警告: heartbeatのバージョンが異なります</i>"},

        {"ClusterBrowser.linbitDrbdMenuName",
         "Filesystem + Linbit:DRBD"},

        {"ClusterBrowser.DrbddiskMenuName",
         "Filesystem + drbddisk (obsolete)"},

        {"ClusterBrowser.StartingPtest",
         "<html><b>適応時の情報:</b><br>"
         + "ポリシー エンジンの確認を開始しています...</html>"},

        {"ClusterBrowser.StartingDRBDtest",
         "<html><b>適応時の情報:</b><br>"
         + "drbdadm --dry-run の確認を開始しています...</html>"},

        {"ClusterBrowser.SameAs",
         "右の値と同じ"},

        {"ClusterBrowser.AddServiceToCluster",
         "サービスをクラスターに追加"},

        {"ClusterBrowser.RAsOverviewButton",
         "リソース エージェント一覧"},

        {"ClusterBrowser.ClassesOverviewButton",
         "クラス一覧"},

        {"ServicesInfo.AddConstraintPlaceholder",
          "制約のプレースホルダーの追加"},

        {"ServicesInfo.AddConstraintPlaceholder.ToolTip",
          "リソースセットを生成するために制約のプレースホルダーを追加します。"},

        {"PtestData.ToolTip",
         "適応時の情報:"},

        {"PtestData.NoToolTip",
         "アクションなし"},

        {"DRBDtestData.ToolTip",
         "適応時の情報:"},

        {"DRBDtestData.NoToolTip",
         "アクションなし"},

        {"HostBrowser.HostWizard",
         "ホスト設定ウィザード"},

        {"HostBrowser.Drbd.NoInfoAvailable",
         "情報が利用できません"},

        {"HostBrowser.Drbd.AddDrbdResource",
         "複製するディスクの追加"},

        {"HostBrowser.Drbd.RemoveDrbdResource",
         "DRBDボリュームの削除"},

        {"HostBrowser.Drbd.SetPrimary",
         "プライマリーに昇格"},

        {"HostBrowser.Drbd.SetPrimaryOtherSecondary",
         "プライマリーに昇格"},

        {"HostBrowser.Drbd.Attach",
         "ディスクの接続"},

        {"HostBrowser.Drbd.Attach.ToolTip",
         "ディスクに接続します"},

        {"HostBrowser.Drbd.Detach",
         "ディスクの切断"},

        {"HostBrowser.Drbd.Detach.ToolTip",
         "ディスクへの接続を切断し、このDRBDデバイスをディスクレスにします"},

        {"HostBrowser.Drbd.Connect",
         "相手に接続"},

        {"HostBrowser.Drbd.Disconnect",
         "相手からの切断"},

        {"HostBrowser.Drbd.SetSecondary",
         "セカンダリーに降格"},

        {"HostBrowser.Drbd.SetSecondary.ToolTip",
         "セカンダリーに降格します"},

        {"HostBrowser.Drbd.ForcePrimary",
         "プライマリーに強制昇格"},

        {"HostBrowser.Drbd.Invalidate",
         "無効化"},

        {"HostBrowser.Drbd.Invalidate.ToolTip",
         "このデバイス上のデータを無効にして、他のノードから完全同期を開始します"},

        {"HostBrowser.Drbd.DiscardData",
         "データの破棄"},

        {"HostBrowser.Drbd.DiscardData.ToolTip",
         "データを破棄して、他のノードから完全同期します"},

        {"HostBrowser.Drbd.Resize",
         "サイズ変更"},

        {"HostBrowser.Drbd.Resize.ToolTip",
         "ブロックデバイスがサイズ変更したときに、DRBDブロックデバイスのサイズ変更を行います"},

        {"HostBrowser.Drbd.ResumeSync",
         "同期の再開"},

        {"HostBrowser.Drbd.ResumeSync.ToolTip",
         "同期を再開します"},

        {"HostBrowser.Drbd.PauseSync",
         "同期の一時停止"},

        {"HostBrowser.Drbd.PauseSync.ToolTip",
         "同期を一時停止します"},

        {"HostBrowser.Drbd.ViewLogs",
         "ログの参照"},

        {"HostBrowser.Drbd.AttachAll",
         "すべてのディスクの接続"},

        {"HostBrowser.Drbd.DetachAll",
         "すべてのディスクの切断"},

        {"HostBrowser.Drbd.LoadDrbd",
         "DRBDモジュールの読み込み"},

        {"HostBrowser.Drbd.AdjustAllDrbd",
         "DRBD構成の読み込み（適応）"},

        {"HostBrowser.Drbd.AdjustAllDrbd.ToolTip",
         "DRBDモジュールへのDRBD構成の読み込み(drbdadm adjust all)"},

        {"HostBrowser.Drbd.UpAll",
         "すべてのDRBDの開始"},

        {"HostBrowser.Drbd.UpgradeDrbd",
         "DRBDの更新"},

        {"HostBrowser.Drbd.ChangeHostColor",
         "色の変更"},

        {"HostBrowser.Drbd.ViewDrbdLog",
         "ログファイルの表示"},

        {"HostBrowser.Drbd.ConnectAll",
         "すべてのDRBDデバイスの接続"},

        {"HostBrowser.Drbd.DisconnectAll",
         "すべてのDRBDデバイスの切断"},

        {"HostBrowser.Drbd.SetAllPrimary",
         "すべてのDRBDデバイスをプライマリーにする"},

        {"HostBrowser.Drbd.SetAllSecondary",
         "すべてのDRBDデバイスをセカンダリーにする"},

        {"HostInfo.CRM.AllMigrateFrom",
         "すべてのリソースを移動する"},

        {"HostInfo.StopCorosync",
         "Corosyncの停止"},

        {"HostInfo.StopOpenais",
         "Openaisの停止"},

        {"HostInfo.StopHeartbeat",
         "Heartbeatの停止"},

        {"HostInfo.StartCorosync",
         "Corosyncの起動"},

        {"HostInfo.StartPacemaker",
         "Pacemakerの開始"},

        {"HostInfo.StartOpenais",
         "Openaisの起動"},

        {"HostInfo.StartHeartbeat",
         "Heartbeatの起動"},

        {"HostInfo.confirmCorosyncStop.Title",
         "Corosyncの停止"},

        {"HostInfo.confirmCorosyncStop.Desc",
         "Corosyncを停止しますか？"},

        {"HostInfo.confirmCorosyncStop.Yes",
         "停止"},

        {"HostInfo.confirmCorosyncStop.No",
         "キャンセル"},

        {"HostInfo.confirmHeartbeatStop.Title",
         "Heartbeatの停止"},

        {"HostInfo.confirmHeartbeatStop.Desc",
         "Heartbeatを停止しますか？"},

        {"HostInfo.confirmHeartbeatStop.Yes",
         "停止"},

        {"HostInfo.confirmHeartbeatStop.No",
         "キャンセル"},

        {"HostBrowser.CRM.StandByOn",
         "スタンバイモードへ移行"},

        {"HostBrowser.CRM.StandByOff",
         "スタンバイモードの解除"},

        {"HostBrowser.RemoveHost",
         "ホストの削除"},

        /* Host Browser */
        {"HostBrowser.idDrbdNode",
         "はDRBDノードです"},

        {"HostBrowser.NetInterfaces",
         "ネットワーク インターフェース"},

        {"HostBrowser.BlockDevices",
         "ブロックデバイス"},

        {"HostBrowser.FileSystems",
         "ファイルシステム"},

        {"HostBrowser.MetaDisk.Internal",
         "内蔵（Internal）"},

        {"HostBrowser.DrbdNetInterface.Select",
         "選択..."},

        {"HostBrowser.Hb.NoInfoAvailable",
         "情報が利用できません"},

        {"HostDrbdInfo.LVMMenu",
         "LVM"},

        {"HostDrbdInfo.AddToVG",
         "VGにLVを生成する"},

        {"HostBrowser.AdvancedSubmenu",
         "拡張オプション"},

        {"HostBrowser.MakeKernelPanic",
         "カーネルパニックを起こす: "},

        {"HostBrowser.MakeKernelReboot",
         "すぐにリブートする: "},

        {"CRMXML.GlobalRequiredOptions",
         "全体の必須オプション"},

        {"CRMXML.GlobalOptionalOptions",
         "全体のオプション"},

        {"CRMXML.RscDefaultsSection",
         "全体のリソースのデフォルト"},

        {"CRMXML.RequiredOptions",
         "必須オプション"},

        {"CRMXML.MetaAttrOptions",
         "メタ属性"},

        {"CRMXML.OptionalOptions",
         "拡張オプション"},

        {"CRMXML.GetOCFParameters",
         "OCFのパラメータを取得しています..."},

        {"CRMXML.TargetRole.ShortDesc",
         "Target Role"},

        {"CRMXML.TargetRole.LongDesc",
         "サービスが開始、停止、昇格されるべきかを選択します。"},

        {"CRMXML.IsManaged.ShortDesc",
         "Is Managed By Cluster"},

        {"CRMXML.IsManaged.LongDesc",
         "サービスがクラスターで管理されるべきかを選択します。"},

        {"CRMXML.AllowMigrate.ShortDesc",
         "Allow Migrate"},

        {"CRMXML.AllowMigrate.LongDesc",
         "ライブ マイグレーションを行いたいときにはこれを設定します。"},

        {"CRMXML.Priority.ShortDesc",
         "Priority"},

        {"CRMXML.Priority.LongDesc",
         "アクティブのままにするのを優先します。"},

        {"CRMXML.ResourceStickiness.ShortDesc",
         "Resource Stickiness"},

        {"CRMXML.ResourceStickiness.LongDesc",
         "リソースがどれくらいの強さでとどまるべきかをスコアとして設定します。"},

        {"CRMXML.MigrationThreshold.ShortDesc",
         "Migration Threshold"},

        {"CRMXML.MigrationThreshold.LongDesc",
         "マイグレーションの閾値の失敗の後に移動します。"},

        {"CRMXML.FailureTimeout.ShortDesc",
         "Failure Timeout"},

        {"CRMXML.FailureTimeout.LongDesc",
         "失敗を無視する時間を秒数で設定します。"},

        {"CRMXML.MultipleActive.ShortDesc",
         "Multiple Active"},

        {"CRMXML.MultipleActive.LongDesc",
         "リソースが複数のノードで誤ってアクティブになったときにどうするかを設定します。"},

        {"CRMXML.ColocationSectionParams",
         "同居制約(colocation)パラメータ"},
        {"CRMXML.OrderSectionParams",
         "順序制約(order)パラメータ"},

        {"Widget.Select",
         "選択..."},

        {"Widget.NothingSelected",
         "<<選択なし>>"},

        {"CRMGraph.ColOrd",
         "同居 / 順序"},

        {"CRMGraph.Colocation",
         "同居指定"},

        {"CRMGraph.NoColOrd",
         "反発 / 順序"},

        {"CRMGraph.NoColocation",
         "反発"},

        {"CRMGraph.Order",
         "順序指定"},

        {"CRMGraph.Removing",
         " 削除しています... "},

        {"CRMGraph.Unconfigured",
         "未設定"},

        /* Score */
        {"Score.InfinityString",
         "常時"},

        {"Score.MinusInfinityString",
         "否定"},

        {"Score.ZeroString",
         "影響無し"},

        {"Score.PlusString",
         "影響有り"},

        {"Score.MinusString",
         "反響"},

        {"Score.Unknown",
         "不明"},

        {"SSH.Enter.password",
         "の<font color=red>パスワード</font>を入力してください:"},

        {"SSH.Enter.passphrase",
         "鍵の<font color=red>パスフレーズ</font>を入力してください:"},

        {"SSH.Enter.passphrase2",
         "（パスワード認証の場合は&lt;Enter&gt;キーを押します）"},

        {"SSH.Enter.sudoPassword",
         "&nbsp;<font color=red>sudo</font>パスワード:"},

        {"SSH.Publickey.Authentication.Failed",
         "認証が失敗しました。"},

        {"SSH.KeyboardInteractive.DoesNotWork",
         "Keyboard-interactive認証は使えません。"},

        {"SSH.KeyboardInteractive.Failed",
         "Keyboard-interactive認証が失敗しました。"},

        {"SSH.Password.Authentication.Failed",
         "パスワード認証が失敗しました。"},

        {"SSH.RSA.DSA.Authentication",
         "RSA/DSA認証"},

        {"SSH.PasswordAuthentication",
         "パスワード認証"},

        {"SSH.SudoAuthentication",
         "sudo認証"},

        {"Heartbeat.getClusterMetadata",
         "メタデータを取得しています"},

        {"Heartbeat.ExecutingCommand",
         "CRMコマンドを実行しています..."},

        {"DrbdNetInterface",
         "ネットワーク インターフェース"},

        {"DrbdNetInterface.Long",
         "DRBDネットワーク インターフェース"},

        {"DrbdMetaDisk",
         "メタディスク"},

        {"DrbdMetaDisk.Long",
         "DRBDメタディスク"},

        {"DrbdNetInterfacePort",
         "ポート番号"},

        {"DrbdNetInterfacePort.Long",
         "DRBDネットワーク インターフェース ポート番号"},

        {"DrbdMetaDiskIndex",
         "メタディスク インデックス"},

        {"DrbdMetaDiskIndex.Long",
         "DRBDメタディスク インデックス"},

        {"ProgressIndicatorPanel.Cancel",
         "キャンセル"},

        {"CIB.ExecutingCommand",
         "CRMコマンドを実行しています..."},

        {"Openais.ExecutingCommand",
         "OpenAISコマンドを実行しています..."},

        {"Corosync.ExecutingCommand",
         "Corosyncコマンドを実行しています..."},

        {"DRBD.ExecutingCommand",
         "DRBDコマンドを実行しています..."},

        {"DrbdXML.GetConfig",
         "DRBD構成を取得しています..."},

        {"DrbdXML.GetParameters",
         "DRBDパラメータを取得しています..."},
        {"Error.Title",
         "エラー"},

        {"LVM.ExecutingCommand",
         "LVMコマンドを実行しています..."},

        {"VIRSH.ExecutingCommand",
         "virshコマンドを実行しています..."},

        {"VMSXML.GetConfig",
         "libvirt構成を解析しています..."},

        {"VMSInfo.AddNewDomain",
         "新しい仮想マシンの追加"},

        {"VMSVirtualDomainInfo.Section.VirtualSystem",
         "仮想システム"},

        {"VMSVirtualDomainInfo.Section.Options",
         "拡張オプション"},

        {"VMSVirtualDomainInfo.Section.Features",
         "機能"},

        {"VMSVirtualDomainInfo.Section.CPUMatch",
         "CPU Match"},

        {"VMSVirtualDomainInfo.Short.Name",
         "ドメイン名"},

        {"VMSVirtualDomainInfo.Short.Type",
         "タイプ"},

        {"VMSVirtualDomainInfo.Short.Vcpu",
         "CPU数"},

        {"VMSVirtualDomainInfo.Short.CurrentMemory",
         "現在のメモリー使用量"},

        {"VMSVirtualDomainInfo.Short.Memory",
         "最大メモリー使用量"},

        {"VMSVirtualDomainInfo.Short.Os.Boot",
         "ブート デバイス"},

        {"VMSVirtualDomainInfo.Short.Os.Loader",
         "ローダー"},

        {"VMSVirtualDomainInfo.Short.Autostart",
         "自動起動"},

        {"VMSVirtualDomainInfo.Short.Arch",
         "CPUアーキテクチャー"},

        {"VMSVirtualDomainInfo.Short.Acpi",
         "ACPI"},

        {"VMSVirtualDomainInfo.Short.Apic",
         "APIC"},

        {"VMSVirtualDomainInfo.Short.Pae",
         "PAE"},

        {"VMSVirtualDomainInfo.Short.Hap",
         "HAP"},

        {"VMSVirtualDomainInfo.Short.CPU.Match",
         "Match"},

        {"VMSVirtualDomainInfo.Short.CPUMatch.Model",
         "Model"},

        {"VMSVirtualDomainInfo.Short.CPUMatch.Vendor",
         "Vendor"},

        {"VMSVirtualDomainInfo.Short.CPUMatch.TopologySockets",
         "Topology Sockets"},

        {"VMSVirtualDomainInfo.Short.CPUMatch.TopologyCores",
         "Topology Cores"},

        {"VMSVirtualDomainInfo.Short.CPUMatch.TopologyThreads",
         "Topology Threads"},

        {"VMSVirtualDomainInfo.Short.CPUMatch.Policy",
         "Policy"},

        {"VMSVirtualDomainInfo.Short.CPUMatch.Features",
         "Features"},

        {"VMSVirtualDomainInfo.Short.OnPoweroff",
         "On Poweroff"},

        {"VMSVirtualDomainInfo.Short.OnReboot",
         "リブート時"},

        {"VMSVirtualDomainInfo.Short.OnCrash",
         "クラッシュ時"},

        {"VMSVirtualDomainInfo.Short.Emulator",
         "エミュレーター"},

        {"VMSVirtualDomainInfo.StartVNCViewerOn",
         "Console (@VIEWER@ VNC) on "},

        {"VMSVirtualDomainInfo.StartOn",
         "Start on "},

        {"VMSVirtualDomainInfo.ShutdownOn",
         "Gracefully shutdown on "},

        {"VMSVirtualDomainInfo.DestroyOn",
         "Kill on "},

        {"VMSVirtualDomainInfo.RebootOn",
         "Reboot on "},

        {"VMSVirtualDomainInfo.SuspendOn",
         "Suspend on "},

        {"VMSVirtualDomainInfo.ResumeOn",
         "Resume on "},

        {"VMSVirtualDomainInfo.AddNewDisk",
         "新しいディスク"},

        {"VMSVirtualDomainInfo.AddNewInterface",
         "新しいネットワーク インターフェース"},

        {"VMSVirtualDomainInfo.AddNewInputDev",
         "新しい入力デバイス（マウス/タブレット）"},

        {"VMSVirtualDomainInfo.AddNewGraphics",
         "新しいグラフィック デバイス（VNC, SDL）"},

        {"VMSVirtualDomainInfo.AddNewSound",
         "新しいサウンド デバイス"},

        {"VMSVirtualDomainInfo.AddNewSerial",
         "新しいシリアル デバイス"},

        {"VMSVirtualDomainInfo.AddNewParallel",
         "新しいパラレル デバイス"},

        {"VMSVirtualDomainInfo.AddNewVideo",
         "新しいビデオ デバイス"},

        {"VMSVirtualDomainInfo.AddNewHardware",
         "新しいハードウェア"},

        {"VMSVirtualDomainInfo.MoreOptions",
         "レジューム/サスペンド"},

        {"VMSVirtualDomainInfo.RemoveDomain",
         "ドメインの削除"},

        {"VMSVirtualDomainInfo.CancelDomain",
         "新しいドメインのキャンセル"},

        {"VMSVirtualDomainInfo.confirmRemove.Title",
         "仮想ドメインの削除"},

        {"VMSVirtualDomainInfo.confirmRemove.Description",
         "仮想ドメイン\"@DOMAIN@\"を削除します。"
         + "本当によいですか？"},

        {"VMSVirtualDomainInfo.confirmRemove.Yes",
         "削除"},

        {"VMSVirtualDomainInfo.confirmRemove.No",
         "キャンセル"},

        {"VMSVirtualDomainInfo.AvailableInVersion",
         "libvirt バージョン@VERSION@で利用可能"},

        {"ConstraintPHInfo.ToolTip",
         "リソースセットのプレースホルダー"},

        {"ConstraintPHInfo.Remove",
         "削除"},

        {"ConstraintPHInfo.confirmRemove.Title",
         "制約のプレースホルダーの削除"},

        {"ConstraintPHInfo.confirmRemove.Description",
         "すべての制約と共にこのプレースホルダーを削除します。"
         + "本当によいですか？"},

        {"ConstraintPHInfo.confirmRemove.Yes",
         "削除"},

        {"ConstraintPHInfo.confirmRemove.No",
         "キャンセル"},

        {"ConfigData.OpMode.RO",
         "リードオンリー"},

        {"ConfigData.OpMode.OP",
         "オペレーター"},

        {"ConfigData.OpMode.ADMIN",
         "管理者"},

        {"ConfigData.OpMode.GOD",
         "開発者レベル10"},

        {"EditableInfo.MoreOptions",
         "拡張モードではより多くのオプションが利用できます..."},

        {"VMSDiskInfo.FileChooserTitle",
         "イメージを選択します。"},

        {"VMSDiskInfo.Approve",
         "選択"},

        {"VMSDiskInfo.Approve.ToolTip",
         "このイメージを選択します。"},

        {"VMSVideoInfo.ModelType",
         "タイプ"},

        {"VMSVideoInfo.ModelVRAM",
         "ビデオ メモリー (Kb)"},

        {"VMSVideoInfo.ModelHeads",
         "画面の数"},

        {"VMSVideoInfo.ModelVRAM.ToolTip",
         "ビデオ メモリー(VRAM) (Kb)"},

        {"VMSVideoInfo.ModelHeads.ToolTip",
         "画面の数 (Heads)"},

        {"VMSHardwareInfo.Menu.Remove",
         "削除"},

        {"VMSHardwareInfo.Menu.Cancel",
         "キャンセル"},

        {"VMSHardwareInfo.confirmRemove.Title",
         "仮想ハードウェアの削除"},

        {"VMSHardwareInfo.confirmRemove.Description",
         "仮想\"@HW@\"を削除します。"
         + "本当によいですか？"},

        {"VMSHardwareInfo.confirmRemove.Yes",
         "削除"},

        {"VMSHardwareInfo.confirmRemove.No",
         "キャンセル"},

        {"ServiceInfo.PingdToolTip",
         "<html><b>pingd</b><br>"
         + "接続可能性に従って場所を設定します。<br>"
         + "うまく動くように、ping/pingdリソースを設定してください。</html>"},

        {"ServiceInfo.Filesystem.RunningOn",
         "動作中"},

        {"ServiceInfo.Filesystem.NotRunning",
         "停止中"},

        {"ServiceInfo.Filesystem.MoutedOn",
         "マウント中"},

        {"ServiceInfo.Filesystem.NotMounted",
         "マウントなし"},

        {"ServiceInfo.NotRunningAnywhere",
         "どのノードでも動作していない"},

        {"ServiceInfo.AlreadyRunningOnNode",
         "このノードで常に動作している"},

        {"ServiceInfo.AlreadyStarted",
         "既に起動している"},

        {"ServiceInfo.AlreadyStopped",
         "既に停止している"},

        {"DrbdInfo.CommonSection",
         "common "},

        {"DrbdResourceInfo.HostAddresses",
         "Net Interface"},

        {"DrbdResourceInfo.AddressOnHost",
         "on "},

        {"DrbdResourceInfo.NetInterfacePort",
         "Port"},

        {"BlockDevice.MetaDiskSection",
         "DRBDメタディスク"},

        {"DrbdVolumeInfo.VolumeSection",
         "DRBD Volume"},

        {"DrbdVolumeInfo.Number",
         "Number"},

        {"DrbdVolumeInfo.Device",
         "Device"},
    };
}
