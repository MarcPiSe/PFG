package edu.udg.tfg.FileAccessControl.config;

import edu.udg.tfg.FileAccessControl.queue.Receiver;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;

@Configuration
public class RabbitConfig {

    public static final String DELETE_ACCESS_QUEUE = "deleteAccessQueue";
    public final static String CONFIRM_DELETE = "confirmDelteTrash";
    public static final String DELETE_USER_FA = "fileAccessUserDelete";
    public static final String USER_DELETION_CONFIRMATION_QUEUE = "user-delete-confirmation-queue";

    @Bean
    public Queue deleteAccessQueue() {
        return new Queue(DELETE_ACCESS_QUEUE, false);
    }

    @Bean
    public Queue deleteUser() {
        return new Queue(DELETE_USER_FA, false);
    }

    @Bean
    TopicExchange exchange() {
        return new TopicExchange("spring-boot-exchange");
    }

    @Bean
    public Binding bindingDeleteAccessQueue(@Qualifier("deleteAccessQueue") Queue deleteAccessQueue, TopicExchange exchange) {
        return BindingBuilder.bind(deleteAccessQueue).to(exchange).with(DELETE_ACCESS_QUEUE);
    }

    @Bean
    public Binding bindingDeleteUserQueue(@Qualifier("deleteUser") Queue deleteUser, TopicExchange exchange) {
        return BindingBuilder.bind(deleteUser).to(exchange).with(DELETE_USER_FA);
    }

    /*@Bean
    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
                                             MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(DELETE_ACCESS_QUEUE, DELETE_USER_FA);
        container.setMessageListener(listenerAdapter);
        return container;
    }*/

    /*@Bean
    MessageListenerAdapter listenerAdapter(Receiver receiver, @Qualifier("jackson2JsonMessageConverter") MessageConverter jsonMessageConverter) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(receiver, "receiveMessage");
        adapter.setMessageConverter(jsonMessageConverter);
        return adapter;
    }*/

    @Bean(name = "jackson2JsonMessageConverter")
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED); 
        typeMapper.setTrustedPackages("*"); 
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

}
