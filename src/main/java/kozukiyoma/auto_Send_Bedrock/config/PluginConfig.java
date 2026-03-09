package kozukiyoma.auto_Send_Bedrock.config;

import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * config.yml の内容を保持するクラス。
 *
 * <p>Velocity に同梱されている Configurate (Sponge Configurate) を使用して
 * YAML を読み込む。シャドウジャーに Configurate を含める必要はない。
 */
public final class PluginConfig {

    private final Path dataDirectory;
    private final Logger logger;

    private PlayerTypeConfig javaConfig;
    private PlayerTypeConfig bedrockConfig;

    public PluginConfig(Path dataDirectory, Logger logger) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory);
        this.logger = Objects.requireNonNull(logger);
        // 初期値として安全なデフォルトを設定
        this.javaConfig = PlayerTypeConfig.defaultConfig();
        this.bedrockConfig = PlayerTypeConfig.defaultConfig();
    }

    /**
     * 設定ファイルをディスクから読み込む（または再読み込みする）。
     *
     * <p>データディレクトリに config.yml が存在しない場合は、
     * JAR 内のデフォルト設定を自動的にコピーする。
     */
    public void load() {
        try {
            ensureConfigExists();
            Path configPath = dataDirectory.resolve("config.yml");

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(configPath)
                    .build();

            ConfigurationNode root = loader.load();

            this.javaConfig = parsePlayerTypeConfig(root, "java");
            this.bedrockConfig = parsePlayerTypeConfig(root, "bedrock");

        } catch (IOException e) {
            logger.error("[AutoTransfer] config.yml の読み込みに失敗しました。デフォルト設定を使用します。", e);
        }
    }

    /**
     * データディレクトリに config.yml が存在しない場合に、
     * プラグイン JAR 内のデフォルト設定をコピーする。
     */
    private void ensureConfigExists() throws IOException {
        if (!Files.exists(dataDirectory)) {
            Files.createDirectories(dataDirectory);
        }

        Path configPath = dataDirectory.resolve("config.yml");
        if (!Files.exists(configPath)) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (in == null) {
                    throw new IOException("JAR 内に config.yml が見つかりません");
                }
                Files.copy(in, configPath);
                logger.info("[AutoTransfer] デフォルトの config.yml を生成しました: "+configPath);
            }
        }
    }

    /**
     * ConfigurationNode から指定されたプレイヤー種別の設定を解析する。
     *
     * @param root      ルートノード
     * @param typeKey   "java" または "bedrock"
     * @return 解析された {@link PlayerTypeConfig}
     */
    private PlayerTypeConfig parsePlayerTypeConfig(ConfigurationNode root, String typeKey) {
        ConfigurationNode node = root.node("settings", typeKey);

        boolean enabled = node.node("enabled").getBoolean(false);
        int delay = node.node("delay").getInt(5);
        String server = node.node("server").getString("lobby");

        // delay は 0 以上であることを保証
        if (delay < 0) {
            logger.warn("[AutoTransfer] settings."+typeKey+".delay に負の値が指定されています。0 に補正します。");
            delay = 0;
        }

        return new PlayerTypeConfig(enabled, delay, server);
    }

    // ---- Getter ----

    public PlayerTypeConfig getJavaConfig() {
        return javaConfig;
    }

    public PlayerTypeConfig getBedrockConfig() {
        return bedrockConfig;
    }
}
