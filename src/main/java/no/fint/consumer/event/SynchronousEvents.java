package no.fint.consumer.event;

import no.fint.event.model.Event;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class SynchronousEvents {

    private ConcurrentMap<String, BlockingQueue<Event>> queues = new ConcurrentSkipListMap<>();

    public BlockingQueue<Event> register(Event event) {
        BlockingQueue<Event> queue = new LinkedBlockingQueue<>();
        queues.put(event.getCorrId(), queue);
        return queue;
    }

    public boolean dispatch(Event event) {
        BlockingQueue<Event> queue = queues.remove(event.getCorrId());
        if (queue != null) {
            return queue.offer(event);
        }
        return false;
    }
}
