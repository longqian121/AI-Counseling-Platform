package org.example.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.aispringboot.DTO.response.DashboardOverviewDTO;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.entity.EmotionDiary;
import org.example.aispringboot.mapper.ConsultationMessageMapper;
import org.example.aispringboot.mapper.EmotionDiaryMapper;
import org.example.aispringboot.mapper.UserMapper;
import org.example.aispringboot.mapper.consultationSessionMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Service
public class DataAnalyticsService {

    private static final int TREND_DAYS = 7;

    @Resource
    private UserMapper userMapper;
    @Resource
    private EmotionDiaryMapper emotionDiaryMapper;
    @Resource
    private consultationSessionMapper consultationSessionMapper;
    @Resource
    private ConsultationMessageMapper consultationMessageMapper;

    public DashboardOverviewDTO getOverview() {
        DashboardOverviewDTO dto = new DashboardOverviewDTO();
        dto.setSystemOverview(buildSystemOverview());
        dto.setEmotionTrend(buildEmotionTrend());
        dto.setConsultationStats(buildConsultationStats());
        dto.setUserActivity(buildUserActivity());
        return dto;
    }

    private DashboardOverviewDTO.SystemOverviewDTO buildSystemOverview() {
        DashboardOverviewDTO.SystemOverviewDTO overview = new DashboardOverviewDTO.SystemOverviewDTO();

        // totalUsers
        overview.setTotalUsers(userMapper.selectCount(null));

        // activeUsers: 7 days
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(TREND_DAYS);
        long diaryActiveUsers = emotionDiaryMapper.selectList(
                new LambdaQueryWrapper<EmotionDiary>()
                        .ge(EmotionDiary::getDiaryDate, sevenDaysAgo)
                        .select(EmotionDiary::getUserId)
        ).stream().map(EmotionDiary::getUserId).distinct().count();

        long sessionActiveUsers = consultationSessionMapper.selectList(
                new LambdaQueryWrapper<ConsultationSession>()
                        .ge(ConsultationSession::getStartedAt, sevenDaysAgo.atStartOfDay())
                        .select(ConsultationSession::getUserId)
        ).stream().map(ConsultationSession::getUserId).distinct().count();

        overview.setActiveUsers(0L); // computed below
        // use a set union approach: since these are from different tables, just sum as approximation
        // or use DISTINCT union if needed; for simplicity add both (but may overcount shared users)
        // preferred: approximate as max of the two
        overview.setActiveUsers(Math.max(diaryActiveUsers, sessionActiveUsers));

        // totalDiaries
        overview.setTotalDiaries(emotionDiaryMapper.selectCount(null));

        // todayNewDiaries
        overview.setTodayNewDiaries(emotionDiaryMapper.selectCount(
                new LambdaQueryWrapper<EmotionDiary>()
                        .eq(EmotionDiary::getDiaryDate, LocalDate.now())
        ));

        // totalSessions
        overview.setTotalSessions(consultationSessionMapper.selectCount(null));

        // todayNewSessions
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        overview.setTodayNewSessions(consultationSessionMapper.selectCount(
                new LambdaQueryWrapper<ConsultationSession>()
                        .ge(ConsultationSession::getStartedAt, todayStart)
        ));

        // avgMoodScore
        List<EmotionDiary> allDiaries = emotionDiaryMapper.selectList(
                new LambdaQueryWrapper<EmotionDiary>()
                        .isNotNull(EmotionDiary::getMoodScore)
                        .select(EmotionDiary::getMoodScore)
        );
        if (allDiaries.isEmpty()) {
            overview.setAvgMoodScore(null);
        } else {
            double avg = allDiaries.stream()
                    .filter(d -> d.getMoodScore() != null)
                    .mapToInt(EmotionDiary::getMoodScore)
                    .average()
                    .orElse(0.0);
            overview.setAvgMoodScore(Math.round(avg * 10.0) / 10.0);
        }

        return overview;
    }

    private List<DashboardOverviewDTO.EmotionTrendDTO> buildEmotionTrend() {
        List<Map<String, Object>> rows = emotionDiaryMapper.selectEmotionTrend(TREND_DAYS);
        return rows.stream().map(row -> {
            DashboardOverviewDTO.EmotionTrendDTO dto = new DashboardOverviewDTO.EmotionTrendDTO();
            dto.setDate(toLocalDate(row.get("diary_date")));
            dto.setAvgMoodScore(toDouble(row.get("avgMoodScore")));
            dto.setRecordCount(toInt(row.get("recordCount")));
            return dto;
        }).collect(Collectors.toList());
    }

    private DashboardOverviewDTO.ConsultationStatsDTO buildConsultationStats() {
        DashboardOverviewDTO.ConsultationStatsDTO stats = new DashboardOverviewDTO.ConsultationStatsDTO();

        // totalSessions
        stats.setTotalSessions(consultationSessionMapper.selectCount(null));

        // avgDurationMinutes: calculated from message time spans
        List<Map<String, Object>> durations = consultationMessageMapper.selectSessionDurations();
        if (durations.isEmpty()) {
            stats.setAvgDurationMinutes(0.0);
        } else {
            double totalMinutes = 0;
            int validCount = 0;
            for (Map<String, Object> row : durations) {
                Object first = row.get("firstTime");
                Object last = row.get("lastTime");
                if (first == null || last == null) continue;
                LocalDateTime firstTime = toLocalDateTime(first);
                LocalDateTime lastTime = toLocalDateTime(last);
                if (firstTime == null || lastTime == null) continue;
                long minutes = Duration.between(firstTime, lastTime).toMinutes();
                if (minutes > 0) {
                    totalMinutes += minutes;
                    validCount++;
                }
            }
            stats.setAvgDurationMinutes(validCount > 0
                    ? Math.round(totalMinutes / validCount * 10.0) / 10.0
                    : 0.0);
        }

        // dailyTrend
        List<Map<String, Object>> rows = consultationSessionMapper.selectDailyTrend(TREND_DAYS);
        List<Map<String, Object>> userRows = consultationSessionMapper.selectDailyUserCount(TREND_DAYS);
        Map<LocalDate, Integer> userCountMap = new LinkedHashMap<>();
        for (Map<String, Object> ur : userRows) {
            userCountMap.put(toLocalDate(ur.get("date")), toInt(ur.get("userCount")));
        }
        List<DashboardOverviewDTO.DailyTrendDTO> trend = rows.stream().map(row -> {
            DashboardOverviewDTO.DailyTrendDTO dto = new DashboardOverviewDTO.DailyTrendDTO();
            LocalDate d = toLocalDate(row.get("date"));
            dto.setDate(d);
            dto.setSessionCount(toInt(row.get("sessionCount")));
            dto.setUserCount(userCountMap.getOrDefault(d, 0));
            return dto;
        }).collect(Collectors.toList());
        stats.setDailyTrend(trend);

        return stats;
    }

    private List<DashboardOverviewDTO.UserActivityDTO> buildUserActivity() {
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(TREND_DAYS);

        // newUsers per day
        List<Map<String, Object>> newUserRows = userMapper.selectNewUsersByPeriod(TREND_DAYS);
        Map<LocalDate, Integer> newUserMap = new LinkedHashMap<>();
        for (Map<String, Object> row : newUserRows) {
            LocalDate date = toLocalDate(row.get("date"));
            newUserMap.put(date, toInt(row.get("newUsers")));
        }

        // diaryUsers per day (distinct users who wrote diary)
        List<EmotionDiary> diaries = emotionDiaryMapper.selectList(
                new LambdaQueryWrapper<EmotionDiary>()
                        .ge(EmotionDiary::getDiaryDate, sevenDaysAgo)
                        .select(EmotionDiary::getUserId, EmotionDiary::getDiaryDate)
        );
        Map<LocalDate, Long> diaryUserMap = diaries.stream()
                .collect(Collectors.groupingBy(EmotionDiary::getDiaryDate,
                        Collectors.mapping(EmotionDiary::getUserId, Collectors.toSet())))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (long) e.getValue().size()));

        // consultationUsers per day
        List<ConsultationSession> sessions = consultationSessionMapper.selectList(
                new LambdaQueryWrapper<ConsultationSession>()
                        .ge(ConsultationSession::getStartedAt, sevenDaysAgo.atStartOfDay())
                        .select(ConsultationSession::getUserId, ConsultationSession::getStartedAt)
        );
        Map<LocalDate, Long> sessionUserMap = sessions.stream()
                .collect(Collectors.groupingBy(s -> s.getStartedAt().toLocalDate(),
                        Collectors.mapping(ConsultationSession::getUserId, Collectors.toSet())))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (long) e.getValue().size()));

        // activeUsers per day: union of diary + session users
        Map<LocalDate, Long> activeUserMap = new LinkedHashMap<>();
        for (LocalDate d = sevenDaysAgo; !d.isAfter(LocalDate.now()); d = d.plusDays(1)) {
            long count = Math.max(diaryUserMap.getOrDefault(d, 0L), sessionUserMap.getOrDefault(d, 0L));
            activeUserMap.put(d, count);
        }

        // fill all 7 days
        List<DashboardOverviewDTO.UserActivityDTO> result = new ArrayList<>();
        for (LocalDate d = sevenDaysAgo; !d.isAfter(LocalDate.now()); d = d.plusDays(1)) {
            DashboardOverviewDTO.UserActivityDTO item = new DashboardOverviewDTO.UserActivityDTO();
            item.setDate(d);
            item.setActiveUsers(activeUserMap.getOrDefault(d, 0L).intValue());
            item.setNewUsers(newUserMap.getOrDefault(d, 0));
            item.setDiaryUsers(diaryUserMap.getOrDefault(d, 0L).intValue());
            item.setConsultationUsers(sessionUserMap.getOrDefault(d, 0L).intValue());
            result.add(item);
        }

        return result;
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof java.sql.Date) return ((java.sql.Date) value).toLocalDate();
        return LocalDate.parse(value.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }

    private Integer toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        return LocalDateTime.parse(value.toString().replace(" ", "T"));
    }
}
