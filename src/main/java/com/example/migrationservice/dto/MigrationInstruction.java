package com.example.migrationservice.dto;

import com.example.migrationservice.Connection;
import com.example.migrationservice.Service;

import java.util.*;

public class MigrationInstruction {
    private List<Set<Service>> groups;
    private Map<Service, List<Connection>> adjacencyMap;

    public List<Set<Service>> getGroups() {
        if (groups == null) {
            groups = new ArrayList<>();
        }
        return groups;
    }

    public Map<Service, List<Connection>> getAdjacencyMap() {
        if (adjacencyMap == null) {
            adjacencyMap = new HashMap<>();
        }
        return adjacencyMap;
    }

}
