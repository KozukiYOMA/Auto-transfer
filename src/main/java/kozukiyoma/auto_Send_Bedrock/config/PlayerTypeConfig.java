package kozukiyoma.auto_Send_Bedrock.config;

/**
 * Java Edition / Bedrock Edition それぞれの転送設定を保持する不変レコード。
 *
 * <p>将来的な拡張例:
 * <ul>
 *   <li>接続元ドメインによる絞り込み (allowedDomains)</li>
 *   <li>転送前に送るメッセージ (preTransferMessage)</li>
 *   <li>転送をスキップするパーミッション (bypassPermission)</li>
 * </ul>
 */
public record PlayerTypeConfig(
        boolean enabled,
        int delaySeconds,
        String targetServer
) {

    /**
     * デフォルト値でインスタンスを生成するファクトリメソッド。
     *
     * @return 無効状態のデフォルト設定
     */
    public static PlayerTypeConfig defaultConfig() {
        return new PlayerTypeConfig(false, 2, "lobby");
    }
}
