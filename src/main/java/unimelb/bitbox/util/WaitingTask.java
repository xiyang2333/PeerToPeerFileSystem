package unimelb.bitbox.util;

/**
 * Created by xiyang on 2019/5/26
 */
public class WaitingTask {
    private HostPort hostPort;

    private Document request;

    public WaitingTask(HostPort hostPort, Document request) {
        this.hostPort = hostPort;
        this.request = request;
    }

    public HostPort getHostPort() {
        return hostPort;
    }

    public void setHostPort(HostPort hostPort) {
        this.hostPort = hostPort;
    }

    public Document getRequest() {
        return request;
    }

    public void setRequest(Document request) {
        this.request = request;
    }
}
