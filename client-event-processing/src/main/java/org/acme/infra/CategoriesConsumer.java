import org.eclipse.microprofile.reactive.messaging.Incoming;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CategoriesConsumer {

  //  @Incoming("test")
    public void process() {
       // String key = record.key(); // Can be `null` if the incoming record has no key
        //String value = record.value(); // Can be `null` if the incoming record has no value
        //String topic = record.topic();
        //int partition = record.partition();
        // ...
    }

}