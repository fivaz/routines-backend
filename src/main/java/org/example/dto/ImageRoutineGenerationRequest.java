package org.example.dto;

import jakarta.validation.constraints.NotNull;

public class ImageRoutineGenerationRequest {

    @NotNull
    private String routineId;
    @NotNull
    private String routineName;

    // Getters and Setters
    public String getRoutineId() {
        return routineId;
    }

    public void setRoutineId(String routineId) {
        this.routineId = routineId;
    }

    public String getRoutineName() {
        return routineName;
    }

    public void setRoutineName(String routineName) {
        this.routineName = routineName;
    }
}
