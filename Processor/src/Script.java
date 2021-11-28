import java.io.Serializable;
import java.util.UUID;

public class Script implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID uuid;
    private String script;
    private String fileLocation;

    public Script(String script){
        this.script = script;
    }

    public Script(String script, String fileLocation){
        this.script = script;
        this.fileLocation = fileLocation;
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

    public String getFileLocation() {
        return fileLocation;
    }

    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }
}
