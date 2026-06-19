package enterprises.iwakura.amitracker.service;

import enterprises.iwakura.amitracker.database.entity.UserEntity;
import enterprises.iwakura.amitracker.database.repository.UserRepository;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;

@Bean
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Get or create a user entity based on the Discord user.
     *
     * @param user Discord user
     *
     * @return The existing or newly created UserEntity
     */
    public UserEntity getOrCreateUser(User user) {
        return userRepository.getOrCreate(user.getIdLong(), user.getGlobalName());
    }

    /**
     * Get a user by ID or throw an exception if not found.
     *
     * @param userId User ID
     *
     * @return The UserEntity
     */
    public UserEntity getUserOrThrow(long userId) {
        return userRepository.findById(userId).orElseThrow(() ->
            new IllegalArgumentException("User with ID " + userId + " not found"));
    }
}
