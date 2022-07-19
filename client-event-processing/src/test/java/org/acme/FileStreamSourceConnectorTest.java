package org.acme;

import org.apache.kafka.connect.connector.ConnectorContext;
import org.apache.kafka.connect.errors.ConnectException;
import org.powermock.api.easymock.PowerMock;
import org.acme.infra.FileStreamSourceConnector;

import org.acme.infra.FileStreamSourceTask;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FileStreamSourceConnectorTest {

    private static final String SINGLE_TOPIC = "test";
    private static final String MULTIPLE_TOPICS = "test1,test2";
    private static final String FILENAME = "categories.txt";

    private FileStreamSourceConnector connector;
    private ConnectorContext ctx;
    private Map<String, String> sourceProperties;



    @BeforeEach
    public void setup() {
        connector = new FileStreamSourceConnector();
        ctx = PowerMock.createMock(ConnectorContext.class);
        connector.initialize(ctx);

        sourceProperties = new HashMap<>();
        sourceProperties.put(FileStreamSourceConnector.TOPIC_CONFIG, SINGLE_TOPIC);
        sourceProperties.put(FileStreamSourceConnector.FILE_CONFIG, FILENAME);
    }

   // @Test
    public void testSourceTasks() {
        PowerMock.replayAll();
        System.out.println("starting Tests");
        connector.start(sourceProperties);
        List<Map<String, String>> taskConfigs = connector.taskConfigs(1);
        assertEquals(1, taskConfigs.size());
        assertEquals(FILENAME,
                taskConfigs.get(0).get(FileStreamSourceConnector.FILE_CONFIG));
        assertEquals(SINGLE_TOPIC,
                taskConfigs.get(0).get(FileStreamSourceConnector.TOPIC_CONFIG));

        // Should be able to return fewer than requested #
        taskConfigs = connector.taskConfigs(2);
        assertEquals(1, taskConfigs.size());
        assertEquals(FILENAME,
                taskConfigs.get(0).get(FileStreamSourceConnector.FILE_CONFIG));
        assertEquals(SINGLE_TOPIC,
                taskConfigs.get(0).get(FileStreamSourceConnector.TOPIC_CONFIG));

        PowerMock.verifyAll();
    }

   // @Test
    public void testSourceTasksStdin() {
        PowerMock.replayAll();

        sourceProperties.remove(FileStreamSourceConnector.FILE_CONFIG);
        connector.start(sourceProperties);
        List<Map<String, String>> taskConfigs = connector.taskConfigs(1);
        assertEquals(1, taskConfigs.size());
        assertNull(taskConfigs.get(0).get(FileStreamSourceConnector.FILE_CONFIG));

        PowerMock.verifyAll();
    }

    //@Test(expected = ConnectException.class)
    public void testMultipleSourcesInvalid() {
        sourceProperties.put(FileStreamSourceConnector.TOPIC_CONFIG, MULTIPLE_TOPICS);
        connector.start(sourceProperties);
    }

   // @Test
    public void testTaskClass() {
        PowerMock.replayAll();

        connector.start(sourceProperties);
        assertEquals(FileStreamSourceTask.class, connector.taskClass());

        PowerMock.verifyAll();
    }
}