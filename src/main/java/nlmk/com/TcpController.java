package nlmk.com;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.stream.IntStream;

public class TcpController implements Runnable{
    private Long timeOut = 6000L;
    private Socket clientDialog;
    private Orchestrator orchestrator;
    private volatile byte[] response = null;
    private static final Logger log = LoggerFactory.getLogger(TcpController.class);

    public TcpController(Socket clientDialog) {
        this.clientDialog = clientDialog;
        orchestrator = Orchestrator.getInstance();
    }

    public void processResponse(RequestWrapper wrapper){
        byte[] responseLocal = wrapper.getResponse();
        byte[] request = wrapper.getRequest();
        log.debug("ORIGINAL RESPONSE: " + Util.convertBytesToString(responseLocal));
        byte masterAddress = request[2];
        int index = IntStream.range(0, responseLocal.length)
                .filter(i -> masterAddress == responseLocal[i])
                .findFirst()
                .orElse(-1);
        if(index != -1){
            byte[] tempResponse = Arrays.copyOfRange(responseLocal,index,responseLocal.length);
            byte[] repairResponse = new byte[tempResponse.length+1];
            repairResponse[0] = (byte)0xff;
            IntStream.range(0, tempResponse.length)
                    .peek(i -> repairResponse[i+1] = responseLocal[i]);
            this.response = repairResponse;
            log.debug("REPAIRED RESPONSE: " + Util.convertBytesToString(repairResponse));
        }else{
            this.response = new byte[10];
        }
    }

    @Override
    public void run() {
        try(DataInputStream in = new DataInputStream(clientDialog.getInputStream());
            DataOutputStream out = new DataOutputStream(clientDialog.getOutputStream());) {
            log.info("DataInputStream created");
            log.info("DataOutputStream  created");
            long oldTime = System.currentTimeMillis();

            while (!clientDialog.isClosed()) {
                if(System.currentTimeMillis() - oldTime > timeOut) break;
                if(response != null){
                    out.write(response,0,response.length);
                    log.info("Send response to client: "+ Util.convertBytesToString(response));
                    out.flush();
                    response = null;
                }
                if(in.available() > 0){
                    byte[] result = readRequest(in);
                    oldTime = System.currentTimeMillis();
                    log.info("Receive message from client- " + Util.convertBytesToString(result));
                    orchestrator.addRequestToQueue(new RequestWrapper(result,this));
                }
            }
            log.info("Client disconnected");
            log.info("Closing connections & channels.");
            clientDialog.close();
        } catch (IOException e) {
            log.error("Error in tcp controller: "+e.getLocalizedMessage()+"\n\t"+e.getMessage());
        }
    }

    private class ResponseConsumer implements Runnable{

        Socket clientDialog;
        boolean stopFlag = false;

        public void stopThread(){
            stopFlag = true;
        }

        public ResponseConsumer(Socket clientDialog){
            this.clientDialog = clientDialog;
        }

        @Override
        public void run() {
            try(DataOutputStream out = new DataOutputStream(clientDialog.getOutputStream())){
                while(!stopFlag){
                    if(response != null){

                    }
                }
            }catch (IOException e){
                log.error("response consumer get error: "+e.getMessage());
            }
        }
    }

    private byte[] readRequest(InputStream is) throws IOException {
        try {
            byte[] buffer = new byte[128];
            byte[] result;
            Arrays.fill(buffer, (byte) -1);
            int counter = 0;
            byte current = -1;
            do {
                current = (byte) is.read();
                buffer[counter] = current;
                counter++;
            } while (current != 0x03);
            result = Arrays.copyOfRange(buffer, 0, counter);
            return result;
        }catch(Exception e){
            log.error("Cant read data from input stream " + e.getCause());
            throw new IOException("Cant read data from input stream",e.getCause());
        }
    }
}
