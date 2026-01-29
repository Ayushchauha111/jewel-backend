package com.example.jewell.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.jewell.model.ERole;
import com.example.jewell.model.Role;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
  Optional<Role> findByName(ERole name);
  
  // Custom query to get first role if duplicates exist
  @Query("SELECT r FROM Role r WHERE r.name = :name ORDER BY r.id ASC")
  List<Role> findAllByName(@Param("name") ERole name);
}