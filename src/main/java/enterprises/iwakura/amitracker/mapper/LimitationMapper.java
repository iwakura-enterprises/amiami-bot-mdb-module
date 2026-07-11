package enterprises.iwakura.amitracker.mapper;

import org.mapstruct.Mapper;

import enterprises.iwakura.amitracker.command.LimitationConfiguration;
import enterprises.iwakura.amitracker.database.entity.LimitationEntity;
import enterprises.iwakura.amitracker.object.LimitationDTO;

@Mapper
public interface LimitationMapper {

    LimitationDTO fromEntity(LimitationEntity entity);

    LimitationDTO fromConfiguration(LimitationConfiguration configuration);

}
