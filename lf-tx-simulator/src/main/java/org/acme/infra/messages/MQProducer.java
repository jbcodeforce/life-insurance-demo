package org.acme.infra.messages;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;

import org.acme.domain.Client;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class MQProducer {
    private static Logger logger = Logger.getLogger(MQProducer.class.getName());
    
    @Inject
    @ConfigProperty(name = "mq.host")
    public String mqHostname;
  
    @Inject
    @ConfigProperty(name = "mq.port")
    public int mqHostport;
  
    @Inject
    @ConfigProperty(name = "mq.qmgr", defaultValue = "QM1")
    public String mqQmgr;
  
    @Inject
    @ConfigProperty(name = "mq.channel", defaultValue = "DEV.APP.SVRCONN")
    public String mqChannel;
  
    @Inject
    @ConfigProperty(name = "mq.app_user", defaultValue = "app")
    public String mqAppUser;
  
    @Inject
    @ConfigProperty(name = "mq.app_password", defaultValue = "passw0rd")
    public String mqPassword;
  
    @Inject
    @ConfigProperty(name = "mq.queue_name", defaultValue = "DEV.QUEUE.1")
    public String mqQueueName;
  
    @Inject
    @ConfigProperty(name = "app.name", defaultValue = "TestApp")
    public String appName;
  
    @Inject
    @ConfigProperty(name = "mq.cipher_suite")
    public Optional<String> mqCipherSuite;
  
    @Inject
    @ConfigProperty(name = "mq.ccdt_url")
    public Optional<String> mqCcdtUrl;

    private JMSProducer producer = null;
    private JMSContext jmsContext = null;
    private Destination destination = null;
    private JmsConnectionFactory cf = null;

    private  ObjectMapper parser = new ObjectMapper();



  public void send(Client client, boolean newClient) {
    try {
      TransactionEvent tx = new TransactionEvent();
      if (newClient) {
        tx.type = TransactionEvent.TX_CLIENT_CREATED;
      } else {
        tx.type = TransactionEvent.TX_CLIENT_UPDATED;
      }
        
        tx.payload = client; 
        tx.txid = client.id;
      String msg = parser.writeValueAsString(tx);
      TextMessage message = jmsContext.createTextMessage(msg);
      message.setJMSCorrelationID(tx.txid);
      producer.send(destination, message);
      logger.info("sent to MQ:" + msg);
    } catch (Exception e) {
      if (e != null) {
        if (e instanceof JMSException) {
          processJMSException((JMSException) e);
        } else {
          logger.severe(e.getMessage());
          logger.severe(e.getCause().getMessage());
        }
      }
    }
  }

  public boolean preProcessing() {
    try {
      jmsContext = buildJMSConnectionSession();
    } catch (JMSException e) {
      e.printStackTrace();
      return false;
    }
    producer = jmsContext.createProducer();
    return true;
  }

  private String validateCcdtFile() {
    /*
     * Modeled after
     * github.com/ibm-messaging/mq-dev-patterns/blob/master/JMS/com/ibm/mq/samples/
     * jms/SampleEnvSetter.java
     */
    String value = mqCcdtUrl.orElse("");
    String filePath = null;
    if (value != null && !value.isEmpty()) {
      logger.info("Checking for existence of file " + value);
      File tmp = new File(value);
      if (!tmp.exists()) {
        logger.info(value + " does not exist...");
        filePath = null;
      } else {
        logger.info(value + " exists!");
        filePath = value;
      }
    }
    return filePath;
  }

  private JMSContext buildJMSConnectionSession() throws JMSException {
    JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
    cf = ff.createConnectionFactory();
    cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
    cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, this.mqQmgr);
    cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, this.appName);

    cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
    cf.setStringProperty(WMQConstants.USERID, this.mqAppUser);
    cf.setStringProperty(WMQConstants.PASSWORD, this.mqPassword);

    String ccdtFilePath = validateCcdtFile();
    if (ccdtFilePath == null) {
      logger.info("No valid CCDT file detected. Using host, port, and channel properties instead.");
      cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, this.mqHostname);
      cf.setIntProperty(WMQConstants.WMQ_PORT, this.mqHostport);
      cf.setStringProperty(WMQConstants.WMQ_CHANNEL, this.mqChannel);
    } else {
      logger.info("Setting CCDTURL to 'file://" + ccdtFilePath + "'");
      cf.setStringProperty(WMQConstants.WMQ_CCDTURL, "file://" + ccdtFilePath);
    }

    if (this.mqCipherSuite != null && !("".equalsIgnoreCase(this.mqCipherSuite.orElse("")))) {
      cf.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, this.mqCipherSuite.orElse(""));
    }

    // Create JMS objects
    jmsContext = cf.createContext();
    destination = jmsContext.createQueue("queue:///" + this.mqQueueName);
    logger.info(cf.toString());
    return jmsContext;
  }


private static void processJMSException(JMSException jmsex) {
    logger.info(jmsex.getMessage());
    Throwable innerException = jmsex.getLinkedException();
    logger.severe("Exception is: " + jmsex);
    if (innerException != null) {
      logger.severe("Inner exception(s):");
    }
    while (innerException != null) {
      logger.severe(innerException.getMessage());
      innerException = innerException.getCause();
    }
    return;
}
}
