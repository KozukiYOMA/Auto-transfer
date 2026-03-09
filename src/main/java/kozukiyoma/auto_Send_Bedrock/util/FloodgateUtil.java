package kozukiyoma.auto_Send_Bedrock.util;

import com.velocitypowered.api.proxy.ProxyServer;
import org.geysermc.floodgate.api.FloodgateApi;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Floodgate プラグインの存否を確認し、Bedrock Edition プレイヤーを判定するユーティリティ。
 *
 * <p>Floodgate が Velocity にインストールされていない環境でも動作するよう、
 * クラスのロードを遅延させ、ClassNotFoundException を安全にハンドリングする。
 *
 * <h2>判定ロジック</h2>
 * <ol>
 *   <li>Velocity の PluginManager で "floodgate" プラグインの存在を確認する</li>
 *   <li>存在する場合のみ {@code FloodgateApi.getInstance().isFloodgatePlayer(uuid)} を呼び出す</li>
 *   <li>存在しない場合は常に {@code false} (= Java プレイヤーとして扱う) を返す</li>
 * </ol>
 */
public final class FloodgateUtil {

    private final boolean floodgatePresent;
    private final Logger logger;

    public FloodgateUtil(ProxyServer server, Logger logger) {
        this.logger = logger;
        // PluginManager でプラグインの存在確認 → クラスロード前に判定することで
        // NoClassDefFoundError を回避する
        this.floodgatePresent = server.getPluginManager().getPlugin("floodgate").isPresent();

        if (this.floodgatePresent) {
            logger.info("[AutoTransfer] Floodgate を検出しました。Bedrock プレイヤー判定が有効です。");
        } else {
            logger.info("[AutoTransfer] Floodgate が見つかりませんでした。すべてのプレイヤーを Java Edition として扱います。");
        }
    }

    /**
     * 指定された UUID のプレイヤーが Bedrock Edition かどうかを返す。
     *
     * @param uuid プレイヤーの UUID
     * @return Floodgate が存在し、かつ Bedrock プレイヤーであれば {@code true}
     */
    public boolean isBedrockPlayer(UUID uuid) {
        if (!floodgatePresent) {
            return false;
        }
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
        } catch (Exception e) {
            // Floodgate の初期化が間に合わない等、予期しない状態でも安全に false を返す
            logger.warn("[AutoTransfer] FloodgateApi の呼び出し中にエラーが発生しました (uuid={}): {}", uuid, e.getMessage());
            return false;
        }
    }

    /**
     * Floodgate プラグインが Velocity に存在するかどうかを返す。
     *
     * @return Floodgate が存在する場合 {@code true}
     */
    public boolean isFloodgatePresent() {
        return floodgatePresent;
    }
}
