package com.example.migrationservice.consumer;

import com.example.migrationservice.KubernetesApiInstructionConverter;
import com.example.migrationservice.PodNodeMigrationHandler;
import com.example.migrationservice.Service;
import com.example.migrationservice.config.MessagingConfig;
import com.example.migrationservice.dto.MigrationInstruction;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.ClientBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MigrationRequestConsumer {

    @Autowired
    RabbitTemplate template;

    @RabbitListener(queues = MessagingConfig.EXECUTE_MIGRATION_QUEUE)
    public void handleMigrationRequest(MigrationInstruction migrationInstruction) {
        System.out.println("Migration Request has been received!");
        for (Set<Service> group : migrationInstruction.getGroups()) {
            System.out.println("Please migrate these services together:");
            System.out.println(group.stream()
                    .map(service -> service.getIpAdresse() + ":" + service.getPort())
                    .collect(Collectors.joining(", ")));
        }

        try {
            ApiClient client = ClientBuilder.cluster().build();
            Configuration.setDefaultApiClient(client);

            CoreV1Api api = new CoreV1Api();

            KubernetesApiInstructionConverter converter = new KubernetesApiInstructionConverter(api, migrationInstruction);
            Map<V1Node, List<V1Pod>> podNodeAssignement = converter.getPodNodeAssignement();

            if (podNodeAssignement != null) {
                new PodNodeMigrationHandler().migratePods(api, podNodeAssignement);
            }
        } catch (IOException e) {
            System.out.println("Oops something went wrong");
            e.printStackTrace();
        } catch (ApiException e) {
            System.out.println("Oops something went wrong");
            System.out.println(e.getResponseBody());
        }

    }
}
