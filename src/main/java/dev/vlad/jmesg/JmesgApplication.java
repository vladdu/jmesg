package dev.vlad.jmesg;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

@SpringBootApplication
@EnableJms
public class JmesgApplication {

    public static final String MAILBOX = "mailbox";

    @Bean
    public JmsListenerContainerFactory<?> myFactory(ConnectionFactory connectionFactory,
                                                    DefaultJmsListenerContainerFactoryConfigurer configurer) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        // This provides all auto-configured defaults to this factory, including the message converter
        configurer.configure(factory, connectionFactory);
        // You could still override some settings if necessary.
        return factory;
    }

    @Bean // Serialize message content to json using TextMessage
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    public static void main(String[] args) {
        // Launch the application
        ConfigurableApplicationContext context = SpringApplication.run(JmesgApplication.class, args);

        JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);

        // Send a message with a POJO - the template reuse the message converter
        System.out.println("Sending an email message.");
        jmsTemplate.convertAndSend(MAILBOX, new Email("info@example.com", "Hello"));
        jmsTemplate.convertAndSend(MAILBOX, new Email("info2@example.com", "Hello2"));
        jmsTemplate.convertAndSend(MAILBOX, new Email("info3@example.com", "Hello3"));
        //sleep(1000); // Wait for the message to be received

        List<Email> obj = jmsTemplate.browse(MAILBOX, (session, message) -> {
            Enumeration<?> browserEnumeration = message.getEnumeration();
            List<Email> messageList = new ArrayList<>();
            while (browserEnumeration.hasMoreElements()) {
                Message next = (Message) browserEnumeration.nextElement();
                messageList.add((Email) Objects.requireNonNull(jmsTemplate.getMessageConverter()).fromMessage(next));
            }
            return messageList;
        });
        System.out.println(obj);

        //sleep(1000); // Wait for the message to be received
        assert obj != null;
        for (Email email : obj) {
            Object result = jmsTemplate.receiveAndConvert(MAILBOX);
            System.out.println(result);
        }
    }

}
