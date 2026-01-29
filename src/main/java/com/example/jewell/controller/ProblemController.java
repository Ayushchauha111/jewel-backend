package com.example.jewell.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.jewell.model.Problem;
import com.example.jewell.repository.ProblemRepository;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

   @Autowired
    private ProblemRepository problemRepository;

    // Get all problems with optional tag filter
    @GetMapping
    public List<Problem> getAllProblems(@RequestParam(required = false) String tag) {
        if (tag != null && !tag.isEmpty()) {
            return problemRepository.findByTagsContainingIgnoreCase(tag);
        }
        return problemRepository.findAll();
    }

    // Create a new problem
    @PostMapping
    public Problem createProblem(@RequestBody Problem problem) {
        return problemRepository.save(problem);
    }

    // Get a single problem by ID
    @GetMapping("/{id}")
    public ResponseEntity<Problem> getProblemById(@PathVariable Long id) {
        Optional<Problem> problem = problemRepository.findById(id);
        return problem.map(ResponseEntity::ok)
                     .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update a problem
    @PutMapping("/{id}")
    public ResponseEntity<Problem> updateProblem(@PathVariable Long id, @RequestBody Problem problemDetails) {
        Optional<Problem> optionalProblem = problemRepository.findById(id);
        
        if (optionalProblem.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Problem problem = optionalProblem.get();
        problem.setProblem(problemDetails.getProblem());
        problem.setSolution(problemDetails.getSolution());
        problem.setThoughtProcess(problemDetails.getThoughtProcess());
        problem.setTags(problemDetails.getTags());
        problem.setDifficulty(problemDetails.getDifficulty());
        problem.setIsImportant(problemDetails.getIsImportant());
        
        Problem updatedProblem = problemRepository.save(problem);
        return ResponseEntity.ok(updatedProblem);
    }

    // Delete a problem
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProblem(@PathVariable Long id) {
        if (problemRepository.existsById(id)) {
            problemRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Toggle problem importance
    @PatchMapping("/{id}/importance")
    public ResponseEntity<Problem> toggleImportance(@PathVariable Long id, @RequestBody ImportanceRequest request) {
        Optional<Problem> optionalProblem = problemRepository.findById(id);
        
        if (optionalProblem.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Problem problem = optionalProblem.get();
        problem.setIsImportant(request.isImportant());
        
        Problem updatedProblem = problemRepository.save(problem);
        return ResponseEntity.ok(updatedProblem);
    }

    // Inner class for importance request
    static class ImportanceRequest {
        private boolean isImportant;

        public boolean isImportant() {
            return isImportant;
        }

        public void setImportant(boolean important) {
            isImportant = important;
        }
    }
}