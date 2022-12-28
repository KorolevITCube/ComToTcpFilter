package nlmk.com;

public class RequestWrapper {
    private final byte[] request;
    private byte[] response;
    private final TcpController controllerLink;

    public byte[] getRequest() {
        return request;
    }

    public TcpController getControllerLink() {
        return controllerLink;
    }

    public byte[] getResponse() {
        return response;
    }

    public void setResponse(byte[] response) {
        this.response = response;
    }

    public RequestWrapper(byte[] request, TcpController controllerLink) {
        this.request = request;
        this.controllerLink = controllerLink;
    }
}
