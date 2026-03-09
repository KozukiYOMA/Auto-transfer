package kozukiyoma.auto_Send_Bedrock;

import kozukiyoma.auto_Send_Bedrock.config.PluginConfig;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

public final class ReloadCommand implements SimpleCommand {
    /** 必要なパーミッション。コンソールはパーミションチェックをバイパスする。 */
    private static final String PERMISSION = "autotransfer.admin";

    private final PluginConfig pluginConfig;
    private final Logger logger;

    public ReloadCommand(PluginConfig pluginConfig, Logger logger) {
        this.pluginConfig = pluginConfig;
        this.logger = logger;
    }

    /**
     * コマンド実行時の処理。
     *
     * <p>サブコマンドが "reload" でなければ使い方を表示する。
     *
     * @param invocation コマンドの呼び出し情報
     */
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        // 引数なし、または "reload" 以外が来た場合は使い方を表示
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            source.sendMessage(Component.text(
                    "使い方: /autotransfer reload", NamedTextColor.YELLOW));
            return;
        }

        // 設定リロード実行
        try {
            pluginConfig.load();
            logger.info("[AutoTransfer] {} が設定をリロードしました。", invocation.source());
            source.sendMessage(Component.text(
                    "[AutoTransfer] 設定をリロードしました。", NamedTextColor.GREEN));
        } catch (Exception e) {
            logger.error("[AutoTransfer] 設定のリロード中にエラーが発生しました。", e);
            source.sendMessage(Component.text(
                    "[AutoTransfer] リロード中にエラーが発生しました。コンソールを確認してください。",
                    NamedTextColor.RED));
        }
    }

    /**
     * コマンドの実行権限チェック。
     *
     * <p>Velocity はコマンド実行前にこのメソッドを呼び出す。
     * {@code false} を返すと Velocity が自動的に "Unknown command" を返し、
     * {@code execute()} は呼び出されない。
     *
     * <p>コンソール ({@code ConsoleCommandSource}) は Velocity の仕様上、
     * すべてのパーミッションを持つため常に {@code true} が返る。
     *
     * @param invocation コマンドの呼び出し情報
     * @return 実行可能であれば {@code true}
     */
    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(PERMISSION);
    }
}
