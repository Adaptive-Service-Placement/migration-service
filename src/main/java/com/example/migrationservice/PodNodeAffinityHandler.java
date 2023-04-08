package com.example.migrationservice;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class PodNodeAffinityHandler {

    private final Map<V1Node, List<V1Pod>> podNodeAssignement;
    private final CoreV1Api api;

    public PodNodeAffinityHandler(CoreV1Api api, Map<V1Node, List<V1Pod>> podNodeAssignement) {
        this.api = api;
        this.podNodeAssignement = podNodeAssignement;
    }

    public void setAllAffinities() throws ApiException {
        int index = 0;
        for (Entry<V1Node, List<V1Pod>> entry : podNodeAssignement.entrySet()) {
            List<V1Pod> groupedPods = entry.getValue();

            String key = "group";
            String value = "group" + index;

            for (V1Pod pod : groupedPods) {
                if (pod.getMetadata() == null) {
                    continue;
                }
                this.setLabel(pod, key, value);
                V1Affinity affinity = new V1Affinity();

                V1PodAffinityTerm podAffinityTerm = new V1PodAffinityTerm();
                podAffinityTerm.setLabelSelector(new V1LabelSelector().putMatchLabelsItem(key, value));
                podAffinityTerm.setTopologyKey("kubernetes.io/hostname");

                V1PodAffinity podAffinity = new V1PodAffinity();
                podAffinity.addRequiredDuringSchedulingIgnoredDuringExecutionItem(podAffinityTerm);

                affinity.setPodAffinity(podAffinity);

                if (pod.getSpec() != null) {
                    pod.setSpec(pod.getSpec().affinity(affinity));
                }

                api.replaceNamespacedPod(pod.getMetadata().getName(), System.getenv("NAMESPACE"), pod, null, null, null, null);
            }
            index++;
        }
    }

    private void setLabel(V1Pod pod, String key, String value) {
        if (pod.getMetadata() != null && pod.getMetadata().getLabels() != null) {
            Map<String, String> labels = pod.getMetadata().getLabels();

            labels.put(key, value);
        }
    }
}
