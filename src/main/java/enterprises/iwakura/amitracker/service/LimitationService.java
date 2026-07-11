package enterprises.iwakura.amitracker.service;

import enterprises.iwakura.amitracker.database.repository.LimitationRepository;
import enterprises.iwakura.amitracker.mapper.LimitationMapper;
import enterprises.iwakura.amitracker.object.LimitationDTO;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;

@Bean
@RequiredArgsConstructor
public class LimitationService {

    private final ConfigurationService configurationService;

    private final LimitationRepository limitationRepository;
    private final LimitationMapper limitationMapper;

    /**
     * Finds limitations for user with fallback to configuration
     *
     * @param userId User ID
     *
     * @return Limitation DTO
     */
    public LimitationDTO findEffectiveLimitationsForUser(long userId) {
        return limitationRepository.findByUserId(userId)
            .map(limitationMapper::fromEntity)
            .orElseGet(() -> limitationMapper.fromConfiguration(configurationService.getLimitationConfiguration()));
    }

    /**
     * Finds limitations for guild with fallback to configuration
     *
     * @param guildId Guild ID
     *
     * @return Limitation DTO
     */
    public LimitationDTO findEffectiveLimitationForGuild(long guildId) {
        return limitationRepository.findByGuildId(guildId)
            .map(limitationMapper::fromEntity)
            .orElseGet(() -> limitationMapper.fromConfiguration(configurationService.getLimitationConfiguration()));
    }
}
