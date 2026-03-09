package kozukiyoma.auto_Send_Bedrock;

import kozukiyoma.auto_Send_Bedrock.config.PluginConfig;
import kozukiyoma.auto_Send_Bedrock.listener.LoginListener;
import kozukiyoma.auto_Send_Bedrock.service.TransferService;
import kozukiyoma.auto_Send_Bedrock.util.FloodgateUtil;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * AutoTransfer プラグインのエントリーポイント。
 *
 * <p>Velocity の依存性注入（Google Guice）を使用してコアコンポーネントを受け取る。
 *
 * <h2>Floodgate との依存関係</h2>
 * <p>{@code @Dependency(id = "floodgate", optional = true)} を指定することで、
 * Floodgate が存在しない環境でもプラグインが正常に起動する。
 * Floodgate が存在する場合、Velocity はこのプラグインより先に Floodgate を初期化する。
 *
 * <h2>将来的な拡張ポイント</h2>
 * <ul>
 *   <li>コマンドハンドラーの登録（設定リロードコマンドなど）</li>
 *   <li>複数のリスナークラスへの分割</li>
 * </ul>
 */
@Plugin(
        id = "autotransfer",
        name = "AutoTransfer",
        version = "1.0.0",
        description = "Automatically transfers players to a backend server after login.",
        authors = {"YourName"},
        dependencies = {
                // Floodgate はオプション依存。存在しない環境でもプラグインは動作する。
                @Dependency(id = "floodgate", optional = true)
        }
)
public final class AutoTransferPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private PluginConfig pluginConfig;
    private TransferService transferService;

    /**
     * Velocity の Guice インジェクターによって呼び出されるコンストラクター。
     *
     * @param server        Velocity プロキシサーバーインスタンス
     * @param logger        SLF4J ロガー（プラグイン ID でスコープ済み）
     * @param dataDirectory プラグインのデータディレクトリ（config.yml の保存先）
     */
    @Inject
    public AutoTransferPlugin(
            ProxyServer server,
            Logger logger,
            @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    /**
     * プロキシの初期化完了後に呼び出される。
     *
     * <p>設定の読み込み、各サービスの初期化、イベントリスナーの登録を行う。
     *
     * @param event ProxyInitializeEvent
     */
    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        // 設定ファイルを読み込む
        this.pluginConfig = new PluginConfig(dataDirectory, logger);
        pluginConfig.load();

        // Floodgate 判定ユーティリティを初期化（存否チェックを含む）
        FloodgateUtil floodgateUtil = new FloodgateUtil(server, logger);

        // 転送サービスを初期化
        // this（プラグインインスタンス）を渡すことで Scheduler が正しく動作する
        this.transferService = new TransferService(this, server, logger);

        // イベントリスナーを登録
        server.getEventManager().register(this,
                new LoginListener(transferService, floodgateUtil, pluginConfig, logger));

        // コマンドを登録
        registerCommands();

        logger.info("[AutoTransfer] プラグインが正常に起動しました。");
    }

    /**
     * プロキシのシャットダウン時に呼び出される。
     *
     * <p>保留中のすべての転送タスクをキャンセルしてリソースを解放する。
     *
     * @param event ProxyShutdownEvent
     */
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (transferService != null) {
            transferService.cancelAll();
        }
        logger.info("[AutoTransfer] プラグインをシャットダウンしました。");
    }

    //コマンド
    private void registerCommands() {
        CommandMeta meta = server.getCommandManager()
                .metaBuilder("autotransfer")
                .aliases("at")
                .plugin(this)
                .build();

        server.getCommandManager().register(meta, new ReloadCommand(pluginConfig, logger));
    }

    // ---- Getter（将来のコマンドハンドラーや外部 API 用） ----

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public TransferService getTransferService() {
        return transferService;
    }
}
