package com.example.jewell.repository;

import com.example.jewell.model.World;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorldRepository extends JpaRepository<World, Long> {
    List<World> findByAdventureMapIdAndIsActiveTrueOrderByDisplayOrderAsc(Long adventureMapId);
    
    @Query("SELECT DISTINCT w FROM World w LEFT JOIN FETCH w.lessons l WHERE w.adventureMap.id = :mapId AND w.isActive = true AND (l.isActive = true OR l IS NULL) ORDER BY w.displayOrder ASC, l.displayOrder ASC")
    List<World> findActiveWorldsByMapId(@Param("mapId") Long mapId);
}
