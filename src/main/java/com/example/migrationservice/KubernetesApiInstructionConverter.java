package com.example.migrationservice;

import com.example.migrationservice.dto.MigrationInstruction;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.ClientBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class KubernetesApiInstructionConverter {

    private final MigrationInstruction migrationInstruction;
    private final CoreV1Api api;

    public KubernetesApiInstructionConverter(CoreV1Api api, MigrationInstruction migrationInstruction) throws IOException {
        this.api = api;
        this.migrationInstruction = migrationInstruction;
    }

    public Map<V1Node, List<V1Pod>> getPodAffinities() throws ApiException {
        V1PodList pods = receiveAllPods();
        V1NodeList nodes = receiveAllNodes();

        int amountOfNodes = nodes.getItems().size();
        int amountOfGroups = migrationInstruction.getGroups().size();

        if (amountOfNodes != amountOfGroups) {
            String message = amountOfNodes < amountOfGroups ?
                    "There are less nodes in the cluster than groups of services." :
                    "There are more nodes in the cluster than groups of services.";
            System.out.println(message);
            return null;
        }

        List<Set<Service>> groups = migrationInstruction.getGroups();

        return convertGroupsOfServicesToNodesAndPods(groups, pods, nodes);
    }

    private Map<V1Node, List<V1Pod>> convertGroupsOfServicesToNodesAndPods(List<Set<Service>> groups, V1PodList pods, V1NodeList nodes) {
        Map<V1Node, List<V1Pod>> nodePodAssignement = new HashMap<>();
        groups.forEach(group -> {
            nodePodAssignement.put(nodes.getItems().remove(0), convertServicesToCorrespondingPods(group, pods));
        });

        return nodePodAssignement;
    }

    private List<V1Pod> convertServicesToCorrespondingPods(Set<Service> services, V1PodList pods) {
        return services.stream()
                .map(service -> getPodFromService(pods, service))
                .collect(Collectors.toList());
    }

    private V1Pod getPodFromService(V1PodList allPods, Service service) {
        return allPods.getItems().stream()
                .filter(pod -> pod.getStatus() != null &&
                        service.getIpAdresse().equals(pod.getStatus().getPodIP()))
                .findFirst()
                .orElse(null);
    }

    private V1PodList receiveAllPods() throws ApiException {
        return api.listNamespacedPod(System.getenv("NAMESPACE"), null, null, null, null, null, null, null, null, 10, false);
    }

    private V1NodeList receiveAllNodes() throws ApiException {
        return api.listNode(null, null, null, null, null, null, null, null, 10, false);
    }
}
