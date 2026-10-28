package org.example.aispringboot.DTO.response;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DashboardOverviewDTO {
    private SystemOverviewDTO systemOverview;
    private List<EmotionTrendDTO> emotionTrend;
    private ConsultationStatsDTO consultationStats;
    private List<UserActivityDTO> userActivity;

    @Data
    public static class SystemOverviewDTO {
        private Long totalUsers;
        private Long activeUsers;
        private Long totalDiaries;
        private Long todayNewDiaries;
        private Long totalSessions;
        private Long todayNewSessions;
        private Double avgMoodScore;
    }

    @Data
    public static class EmotionTrendDTO {
        private LocalDate date;
        private Double avgMoodScore;
        private Integer recordCount;
    }

    @Data
    public static class ConsultationStatsDTO {
        private Long totalSessions;
        private Double avgDurationMinutes;
        private List<DailyTrendDTO> dailyTrend;
    }

    @Data
    public static class DailyTrendDTO {
        private LocalDate date;
        private Integer sessionCount;
		private Integer userCount;
    }

    @Data
    public static class UserActivityDTO {
        private LocalDate date;
        private Integer activeUsers;
        private Integer newUsers;
        private Integer diaryUsers;
        private Integer consultationUsers;
    }
}
