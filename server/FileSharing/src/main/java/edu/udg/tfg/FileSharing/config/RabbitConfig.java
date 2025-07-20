package edu.udg.tfg.FileSharing.config;

import edu.udg.tfg.FileSharing.queue.Receiver;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;

@Configuration
public class RabbitConfig {

    public final static String COMMAND_QUEUE = "commandQueue";
    public final static String DELETE_SHARING = "deleteSharing";
    public final static String CONFIRM_DELETE = "confirmDelteTrash";
    public static final String DELETE_USER_FS = "fileSharingUserDelete";
    public static final String USER_DELETION_CONFIRMATION_QUEUE = "user-delete-confirmation-queue";

    @Bean
    TopicExchange exchange() {
        return new TopicExchange("spring-boot-exchange");
    }

    @Bean(name = "deleteSharingQueue")
    Queue queue() {
        return new Queue(DELETE_SHARING, false);
    }

    @Bean
    Binding binding(@Qualifier("deleteSharingQueue") Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(DELETE_SHARING);
    }

    @Bean(name = "queueDeleteUser")
    Queue queueDeleteUser() {
        return new Queue(DELETE_USER_FS, false);
    }

    @Bean
    Binding bindingDeleteUser(@Qualifier("queueDeleteUser") Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(RabbitConfig.DELETE_USER_FS);
    }

    /*@Bean
    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
                                             MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(DELETE_SHARING, RabbitConfig.DELETE_USER_FS);
        container.setMessageListener(listenerAdapter);
        return container;
    }

    @Bean
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
