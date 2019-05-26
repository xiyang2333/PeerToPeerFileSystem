package unimelb.bitbox.util;

/**
 * Created by xiyang on 2019/5/26
 */
public class WaitingTask {
    private HostPort hostPort;

    private Document request;

    private int retryTimes;

    public WaitingTask(HostPort hostPort, Document request, int retryTimes) {
        this.hostPort = hostPort;
        this.request = request;
        this.retryTimes = retryTimes;
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

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }
}
