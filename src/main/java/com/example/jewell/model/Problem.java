package com.example.jewell.model;

import jakarta.persistence.*;

@Entity
public class Problem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(columnDefinition = "TEXT")
    private String problem;
    
    @Column(columnDefinition = "TEXT")
    private String solution;
    
    @Column(columnDefinition = "TEXT")
    private String thoughtProcess;
    
    private String tags;
    private String difficulty;


    public boolean getIsImportant() {
        return isImportant;
    }

    public void setIsImportant(boolean isImportant) {
        this.isImportant = isImportant;
    }

    private boolean isImportant;

    // Constructors
    public Problem() {}

    public Problem(String problem, String solution, String thoughtProcess, String tags, String difficulty) {
        this.problem = problem;
        this.solution = solution;
        this.thoughtProcess = thoughtProcess;
        this.tags = tags;
        this.difficulty = difficulty;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProblem() {
        return problem;
    }

    public void setProblem(String problem) {
        this.problem = problem;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public String getThoughtProcess() {
        return thoughtProcess;
    }

    public void setThoughtProcess(String thoughtProcess) {
        this.thoughtProcess = thoughtProcess;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
}