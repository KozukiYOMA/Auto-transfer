package kozukiyoma.auto_Send_Bedrock;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import org.slf4j.Logger;

@Plugin(id = "auto_send_bedrock", name = "Auto_Send_Bedrock", version = "1.0-SNAPSHOT", description = "自動でBedrockプレイヤーを特定の鯖に転送します。",url ="",authors ={"YOMA8338"})

public class Auto_Send_Bedrock {

    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
    }
}