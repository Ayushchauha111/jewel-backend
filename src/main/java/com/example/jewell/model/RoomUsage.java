package com.example.jewell.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * RoomUsage entity for tracking historical room usage data
 * Used for analytics and capacity planning
 */
@Entity
@Table(name = "room_usage")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private StudyRoom room;

    @Column(name = "participant_count", nullable = false)
    private Integer participantCount;

    @Column(name = "recorded_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime recordedAt;

    @Column(name = "hour_of_day")
    private Integer hourOfDay; // 0-23 for hourly analytics

    @Column(name = "day_of_week")
    private Integer dayOfWeek; // 1-7 (Monday-Sunday)

    @PrePersist
    public void prePersist() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
        if (hourOfDay == null) {
            hourOfDay = recordedAt.getHour();
        }
        if (dayOfWeek == null) {
            dayOfWeek = recordedAt.getDayOfWeek().getValue();
        }
    }
}

