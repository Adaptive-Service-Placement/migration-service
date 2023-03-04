package com.example.migrationservice.consumer;

import com.example.migrationservice.config.MessagingConfig;
import com.example.migrationservice.dto.MigrationInstruction;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MigrationRequestConsumer {

    @Autowired
    RabbitTemplate template;

    @RabbitListener(queues = MessagingConfig.EXECUTE_MIGRATION_QUEUE)
    public void handleMigrationRequest(MigrationInstruction migrationInstruction) {
    }
}
