package noza.core.worker.events;

import noza.core.ClientRecord;
import noza.core.client.events.PublishMsgEvent;
import noza.core.msg.Msg;
import noza.core.msg.PublishMsg;
import noza.core.worker.ClientWorker;

import java.util.List;


public class PublishMsgBatch
{
    private int size;
    private PublishMsgEvent[] publishBatch;
    //private Db db;
    private PublishMsg msg;
    private List<ClientWorker> workers;
    private boolean autoCommit;

    public PublishMsgBatch(/*Db db, */List<ClientWorker> workers)
    {
        this.autoCommit = true;
        //this.db = db;
        this.workers = workers;

        publishBatch = new PublishMsgEvent[workers.size()];
        for (int i = 0; i < workers.size(); i++) {
            publishBatch[i] = new PublishMsgEvent(workers.get(i));
        }
    }

    public void setMsg(PublishMsg msg)
    {
        this.msg = msg;
    }

    public PublishMsg getMsg()
    {
        return msg;
    }

    public void add(ClientRecord record)
    {
        if (msg.qos > Msg.QOS0) {
            if (!msg.isStored()) {
                //db.storeMsg(msg);
                msg.setStored();
            }
        }

        if (msg.isStored()) {
            if (autoCommit) {
                autoCommit = false;
              //  db.setAutoCommit(false);
            }

           // db.storeClientOutMsg(record.getClientId(), msg.getId(),
             //                    0, PublishMsg.QUEUED);
        }

        int workerId    = record.getWorker().getId();
        String clientId = record.getClientId();

        publishBatch[workerId].add(clientId);

        size++;
    }

    public int size()
    {
        return size;
    }

    public void clear()
    {
        size = 0;
        msg  = null;
    }

    public void flush()
    {
        if (msg.isStored() && size > 0) {
            autoCommit = true;
            //db.setAutoCommit(true);
        }

        for (int i = 0; i < publishBatch.length; i++) {
            if (publishBatch[i].sendEvent(msg)) {
                publishBatch[i] = new PublishMsgEvent(workers.get(i));
            }
        }
    }
}
