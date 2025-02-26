package org.example.dto;

import jakarta.validation.constraints.NotNull;

public class ImageTaskGenerationRequest {
    @NotNull
    private String taskId;
    @NotNull
    private String routineId;
    @NotNull
    private String taskName;
    @NotNull
    private String focus;

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getRoutineId() {
        return routineId;
    }

    public void setRoutineId(String routineId) {
        this.routineId = routineId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getFocus() {
        return focus;
    }

    public void setFocus(String focus) {
        this.focus = focus;
    }
}
