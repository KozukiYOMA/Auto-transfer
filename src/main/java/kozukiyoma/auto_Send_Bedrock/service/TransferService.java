package kozukiyoma.auto_Send_Bedrock.service;

import kozukiyoma.auto_Send_Bedrock.config.PlayerTypeConfig;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * プレイヤーの自動転送スケジューリングと実行を担うサービスクラス。
 *
 * <h2>スレッド安全性</h2>
 * <p>Velocity のスケジューラーはすべてのタスクを非同期に実行するため、
 * タスク管理マップには {@link ConcurrentHashMap} を使用している。
 *
 * <h2>将来的な拡張ポイント</h2>
 * <ul>
 *   <li>転送前にプレイヤーへメッセージを送信する</li>
 *   <li>パーミッションによる転送スキップ</li>
 *   <li>接続元バーチャルホストによる条件転送</li>
 * </ul>
 */
public final class TransferService {

    private final ProxyServer server;
    private final Logger logger;

    /**
     * プレイヤー UUID → スケジュール済みタスク のマッピング。
     * プレイヤーがログアウトしたときにタスクをキャンセルするために使用する。
     */
    private final ConcurrentHashMap<UUID, ScheduledTask> pendingTasks = new ConcurrentHashMap<>();

    /** @param pluginInstance プラグインメインクラスのインスタンス（スケジューラー登録に必要） */
    private final Object pluginInstance;

    public TransferService(Object pluginInstance, ProxyServer server, Logger logger) {
        this.pluginInstance = pluginInstance;
        this.server = server;
        this.logger = logger;
    }

    /**
     * 指定されたプレイヤーに対して転送タスクをスケジュールする。
     *
     * <p>すでに同一プレイヤーのタスクが存在する場合は上書きせず、
     * 既存タスクをそのまま維持する（二重スケジュール防止）。
     *
     * @param player プレイヤー
     * @param config そのプレイヤー種別の転送設定
     */
    public void scheduleTransfer(Player player, PlayerTypeConfig config) {
        UUID uuid = player.getUniqueId();
        String playerName = player.getUsername();
        String targetServerName = config.targetServer();
        int delaySeconds = config.delaySeconds();

        // 二重スケジュール防止
        if (pendingTasks.containsKey(uuid)) {
            return;
        }

        logger.info("[AutoTransfer] Scheduling transfer for player "+playerName+" to "+targetServerName+" in "+delaySeconds+" seconds");

        // Velocity Scheduler でディレイ実行
        // Velocity のタスクはすべて非同期実行のためスレッド安全に処理する
        ScheduledTask task = server.getScheduler()
                .buildTask(pluginInstance, () -> executeTransfer(uuid, playerName, targetServerName))
                .delay(delaySeconds, TimeUnit.SECONDS)
                .schedule();

        pendingTasks.put(uuid, task);
    }

    /**
     * 実際の転送処理を実行する。
     *
     * <p>転送前に以下を確認し、条件を満たさない場合は転送を中止する:
     * <ol>
     *   <li>プレイヤーがまだオンラインか</li>
     *   <li>転送先サーバーが velocity.toml に定義されているか</li>
     * </ol>
     *
     * @param uuid             プレイヤー UUID
     * @param playerName       プレイヤー名（ログ用）
     * @param targetServerName 転送先サーバー名
     */
    private void executeTransfer(UUID uuid, String playerName, String targetServerName) {
        // タスクマップからクリーンアップ（転送実行後は不要になる）
        pendingTasks.remove(uuid);

        // ① プレイヤーのオンライン確認
        Optional<Player> playerOpt = server.getPlayer(uuid);
        if (playerOpt.isEmpty()) {
            // プレイヤーがすでにオフラインの場合は静かに中止（正常ケースのため INFO レベル不要）
            return;
        }
        Player player = playerOpt.get();

        // ② 転送先サーバーの存在確認
        Optional<RegisteredServer> targetServerOpt = server.getServer(targetServerName);
        if (targetServerOpt.isEmpty()) {
            logger.error("[AutoTransfer] Target server not found: "+targetServerName);
            return;
        }
        RegisteredServer targetServer = targetServerOpt.get();

        // ③ 転送実行
        // createConnectionRequest + connectWithIndication でエラーハンドリングを行う
        player.createConnectionRequest(targetServer)
                .connectWithIndication()
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("[AutoTransfer] プレイヤー "+playerName+" の転送中に例外が発生しました: "+throwable.getMessage(), throwable);
                    } else if (Boolean.FALSE.equals(result)) {
                        logger.warn("[AutoTransfer] プレイヤー "+playerName+" の "+targetServerName+" への転送が失敗しました（サーバーが拒否した可能性があります）");
                    }
                });
    }

    /**
     * 指定されたプレイヤーの保留中タスクをキャンセルする。
     *
     * <p>プレイヤーがタイマー終了前にログアウトした場合に呼び出す。
     *
     * @param uuid プレイヤー UUID
     */
    public void cancelTransfer(UUID uuid) {
        ScheduledTask task = pendingTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * すべての保留中タスクをキャンセルする。
     *
     * <p>プロキシのシャットダウン時に呼び出す。
     */
    public void cancelAll() {
        pendingTasks.forEach((uuid, task) -> task.cancel());
        pendingTasks.clear();
    }
}
