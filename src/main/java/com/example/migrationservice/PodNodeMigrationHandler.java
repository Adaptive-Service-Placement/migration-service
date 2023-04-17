package com.example.migrationservice;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1Pod;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public class PodNodeMigrationHandler {

    public void migratePods(CoreV1Api api, Map<V1Node, List<V1Pod>> podNodeAssignement) throws ApiException {
        for (Entry<V1Node, List<V1Pod>> entry : podNodeAssignement.entrySet()) {
            List<V1Pod> groupedPods = entry.getValue();
            V1Node destinedNode = entry.getKey();

            for (V1Pod pod : groupedPods) {
                if (pod.getMetadata() == null || destinedNode.getMetadata() == null || pod.getSpec() == null) {
                    continue;
                }

                // check if pod needs to move
                if (pod.getSpec().getNodeName() != null &&
                        destinedNode.getMetadata().getName() != null &&
                        pod.getSpec().getNodeName().equals(destinedNode.getMetadata().getName())) {

                    pod.getSpec().setNodeName(destinedNode.getMetadata().getName());

                    System.out.println("Moving Pod: " + pod.getMetadata().getName() + " to Node: " + destinedNode.getMetadata().getName());

                    api.deleteNamespacedPod(Objects.requireNonNull(pod.getMetadata()).getName(), System.getenv("NAMESPACE"), null, null, 0, null, "Background", null);
                    api.createNamespacedPod(System.getenv("NAMESPACE"), pod, null, null, null, null);
                }
            }
        }
    }
}