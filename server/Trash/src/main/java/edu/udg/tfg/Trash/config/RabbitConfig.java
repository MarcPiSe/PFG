package edu.udg.tfg.Trash.config;

import edu.udg.tfg.Trash.queue.Receiver;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;

@Configuration
public class RabbitConfig {

    public static final String DELETE_ACCESS_QUEUE = "deleteAccessQueue";
    public final static String DELETE_SHARING = "deleteSharing";
    public final static String DELETE_ELEMENT = "deleteElementQueue";
    public final static String COMMAND_QUEUE = "commandQueue";
    public final static String CONFIRM_DELETE = "confirmDelteTrash";
    public static final String DELETE_USER_TR = "trashUserDelete";

    public static final String USER_DELETION_CONFIRMATION_QUEUE = "user-delete-confirmation-queue";

    @Bean(name = "confirmDeleteQueue")
    Queue queue() {
        return new Queue(CONFIRM_DELETE, false);
    }

    @Bean
    TopicExchange exchange() {
        return new TopicExchange("spring-boot-exchange");
    }

    @Bean
    public Queue userDeletionConfirmationQueue() {
        return new Queue(USER_DELETION_CONFIRMATION_QUEUE, true);
    }

    @Bean
    public Binding bindingUserDeletionConfirmation(@Qualifier("userDeletionConfirmationQueue") Queue userDeletionConfirmationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(userDeletionConfirmationQueue).to(exchange).with(USER_DELETION_CONFIRMATION_QUEUE);
    }

    @Bean
    Binding binding(@Qualifier("confirmDeleteQueue") Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(CONFIRM_DELETE);
    }

    @Bean(name = "queueDeleteUser")
    Queue queueDeleteUser() {
        return new Queue(DELETE_USER_TR, false);
    }

    @Bean
    Binding bindingDeleteUser(@Qualifier("queueDeleteUser") Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(DELETE_USER_TR);
    }

    @Bean(name = "jackson2JsonMessageConverter")
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.setTrustedPackages("*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
