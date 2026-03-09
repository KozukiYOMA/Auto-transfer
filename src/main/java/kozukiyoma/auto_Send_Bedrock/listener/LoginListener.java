package kozukiyoma.auto_Send_Bedrock.listener;

import kozukiyoma.auto_Send_Bedrock.config.PlayerTypeConfig;
import kozukiyoma.auto_Send_Bedrock.config.PluginConfig;
import kozukiyoma.auto_Send_Bedrock.service.TransferService;
import kozukiyoma.auto_Send_Bedrock.util.FloodgateUtil;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import org.slf4j.Logger;

/**
 * プレイヤーのログイン・ログアウトイベントを処理するリスナー。
 *
 * <h2>処理フロー</h2>
 * <ol>
 *   <li>{@link PostLoginEvent}: プレイヤーが Velocity に接続した直後に発火</li>
 *   <li>Java / Bedrock 判定 → 対応する設定を取得</li>
 *   <li>機能が有効であれば {@link TransferService} に転送タスクを委譲</li>
 *   <li>{@link DisconnectEvent}: ログアウト時に保留タスクをキャンセル</li>
 * </ol>
 *
 * <h2>将来的な拡張ポイント</h2>
 * <ul>
 *   <li>プレイヤーのパーミッションを確認して転送をスキップ</li>
 *   <li>接続元バーチャルホスト ({@code player.getVirtualHost()}) による条件分岐</li>
 *   <li>転送前メッセージの送信</li>
 * </ul>
 */
public final class LoginListener {

    private final TransferService transferService;
    private final FloodgateUtil floodgateUtil;
    private final PluginConfig pluginConfig;
    private final Logger logger;

    public LoginListener(
            TransferService transferService,
            FloodgateUtil floodgateUtil,
            PluginConfig pluginConfig,
            Logger logger) {
        this.transferService = transferService;
        this.floodgateUtil = floodgateUtil;
        this.pluginConfig = pluginConfig;
        this.logger = logger;
    }

    /**
     * プレイヤーが Velocity に正常にログインした後に呼び出される。
     *
     * <p>{@code PostLoginEvent} は Velocity 3.x で非同期に発火するため、
     * スレッド安全な実装が必要。
     *
     * @param event PostLoginEvent
     */
    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();

        // Java / Bedrock 判定
        boolean isBedrock = floodgateUtil.isBedrockPlayer(player.getUniqueId());

        // 対応する設定を取得
        PlayerTypeConfig config = isBedrock
                ? pluginConfig.getBedrockConfig()
                : pluginConfig.getJavaConfig();

        // 機能が無効なら何もしない
        if (!config.enabled()) {
            return;
        }

        // 転送タスクをスケジュール
        transferService.scheduleTransfer(player, config);
    }

    /**
     * プレイヤーが Velocity から切断した後に呼び出される。
     *
     * <p>タイマー終了前にプレイヤーがログアウトした場合に、
     * 不要なタスクをキャンセルしてリソースリークを防ぐ。
     *
     * @param event DisconnectEvent
     */
    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        transferService.cancelTransfer(event.getPlayer().getUniqueId());
    }
}
