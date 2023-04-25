package com.example.migrationservice;

import com.example.migrationservice.dto.MigrationFinishedMessage;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.*;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class PodNodeMigrationHandler {

    public MigrationFinishedMessage migratePods(AppsV1Api appsV1Api, Map<V1Node, List<V1Pod>> podNodeAssignement) throws ApiException {
        System.out.println("Migrating services...");
        for (Entry<V1Node, List<V1Pod>> entry : podNodeAssignement.entrySet()) {
            List<V1Pod> groupedPods = entry.getValue();
            V1Node destinedNode = entry.getKey();

            for (V1Pod pod : groupedPods) {
                if (pod.getMetadata() == null || destinedNode.getMetadata() == null || pod.getSpec() == null) {
                    System.out.println("Pod does not have the necessary information!");
                    continue;
                }

                System.out.println("Moving Pod: " + pod.getMetadata().getName() + " to: " + destinedNode.getMetadata().getName());

                // find deployment responsible for the pod
                String releaseLabelValue = pod.getMetadata().getLabels() != null ? pod.getMetadata().getLabels().get("release") : "";
                V1DeploymentList deployments = appsV1Api.listNamespacedDeployment("default", null, null, null, null, "release=" + releaseLabelValue, null, null, null, null, null);

                if (deployments != null) {
                    System.out.println("Deployment gefunden!");
                    V1Deployment oldDeployment = deployments.getItems().get(0);
                    V1Deployment newDeployment = clone(oldDeployment);

                    V1PodSpec podSpec = getPodSpecFromDeploymentNullsafe(newDeployment);
                    // deploy pod on destined node
                    if (podSpec != null) {
                        podSpec.putNodeSelectorItem("kubernetes.io/hostname", destinedNode.getMetadata().getName());
                    }
                    if (oldDeployment.getMetadata() != null) {
                        appsV1Api.deleteNamespacedDeployment(oldDeployment.getMetadata().getName(), "default", null, null, 0, null, "Background", null);
                        appsV1Api.createNamespacedDeployment("default", newDeployment, null, null, null, null);
                    }
                } else {
                    System.out.println("No Deployment found!");
                }
            }
        }


        try {
            // wait 2 minutes for pods to be ready (TODO: implement waiting logic)
            System.out.println("Waiting for pods to be ready...");
            Thread.sleep(120000);
            MigrationFinishedMessage message = new MigrationFinishedMessage();
            message.setMigrationSuccessful(true);
            System.out.println("Migration successful!");
            return message;
        } catch (InterruptedException e) {
            e.printStackTrace();
            MigrationFinishedMessage message = new MigrationFinishedMessage();
            message.setMigrationSuccessful(false);
            System.out.println("Migration failed!");
            return message;
        }
    }

    private V1PodSpec getPodSpecFromDeploymentNullsafe(V1Deployment deployment) {
        if (deployment != null && deployment.getSpec() != null && deployment.getSpec() != null && deployment.getSpec().getTemplate() != null) {
            return deployment.getSpec().getTemplate().getSpec();
        }
        return null;
    }

    private V1Deployment clone(V1Deployment deploymentToClone) {
        V1Deployment clonedDeployment = new V1Deployment();
        clonedDeployment.setApiVersion(deploymentToClone.getApiVersion());
        clonedDeployment.setKind(deploymentToClone.getKind());
        clonedDeployment.setMetadata(deploymentToClone.getMetadata());
        if (clonedDeployment.getMetadata() != null) {
            clonedDeployment.getMetadata().setResourceVersion(null);
        }
        clonedDeployment.setSpec(deploymentToClone.getSpec());
        clonedDeployment.setStatus(deploymentToClone.getStatus());

        return clonedDeployment;
    }
}
