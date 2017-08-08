package noza.core.worker.events;

import noza.core.ClientRecord;
import noza.core.client.events.PublishMsgEvent;
import noza.core.msg.PublishMsg;
import noza.core.worker.Worker;
import noza.db.Db;
import noza.core.msg.Topic;

import java.util.List;


public class PublishMsgBatch
{
    private int size;
    private PublishMsgEvent[] publishBatch;
    private Db db;
    private Db.ClientOutMsgBatch dbBatch;
    private PublishMsg msg;
    private List<Worker> workers;

    public PublishMsgBatch(Db db, List<Worker> workers)
    {
        this.db = db;
        this.workers = workers;
        this.dbBatch = db.createPublishOutBatch();

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
        if (msg.qos > Topic.QOS0) {
            if (!msg.isStored()) {
                db.storeMsg(msg, false);
                msg.setStored();
            }
        }

        if (msg.isStored()) {
            dbBatch.storeClientOutMsg(record.getClientId(),
                                      msg.getId(),
                                      (short) 0,
                                      PublishMsg.QUEUED);
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
            dbBatch.execute();
        }

        for (int i = 0; i < publishBatch.length; i++) {
            if (publishBatch[i].sendEvent(msg)) {
                publishBatch[i] = new PublishMsgEvent(workers.get(i));
            }
        }
    }
}
