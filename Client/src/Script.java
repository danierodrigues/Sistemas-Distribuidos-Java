import java.io.Serializable;
import java.util.UUID;

public class Script implements Serializable {
    private static final long serialVersionUID = 1L;
    private String script;
    private UUID uuid;

    public Script(String script){
        this.script = script;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}
