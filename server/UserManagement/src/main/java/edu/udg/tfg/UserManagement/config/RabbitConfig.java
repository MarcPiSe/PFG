package edu.udg.tfg.UserManagement.config;

import edu.udg.tfg.UserManagement.queue.Receiver;
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

    public static final String DELETE_USER_FM = "fileManagerUserDelete";
    public static final String DELETE_USER_FA = "fileAccessUserDelete";
    public static final String DELETE_USER_FS = "fileSharingUserDelete";
    public static final String DELETE_USER_SS = "syncUserDelete";
    public static final String DELETE_USER_TR = "trashUserDelete";
    public static final String DELETE_USER_UA = "userAuthUserDelete";
    public static final String DELETE_USER_UM = "userManagementUserDelete";
    public static final String USER_DELETION_CONFIRMATION_QUEUE = "user-delete-confirmation-queue";

    @Bean
    public Queue deleteUserUMQueue() {
        return new Queue(DELETE_USER_UM, false);
    }

    @Bean
    TopicExchange exchange() {
        return new TopicExchange("spring-boot-exchange");
    }

    @Bean
    public Binding bindingDeleteUserUM(@Qualifier("deleteUserUMQueue") Queue deleteUserUMQueue, TopicExchange exchange) {
        return BindingBuilder.bind(deleteUserUMQueue).to(exchange).with(DELETE_USER_UM);
    }

    @Bean
    public Queue userDeletionConfirmationQueue() {
        return new Queue(USER_DELETION_CONFIRMATION_QUEUE, true);
    }

    @Bean
    public Binding bindingUserDeletionConfirmation(@Qualifier("userDeletionConfirmationQueue") Queue userDeletionConfirmationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(userDeletionConfirmationQueue).to(exchange).with(USER_DELETION_CONFIRMATION_QUEUE);
    }

    /*@Bean
    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
                                             MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(
                QUEUE_NAME,
                DELETE_USER_UM,
                USER_DELETION_CONFIRMATION_QUEUE
        );
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
