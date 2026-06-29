package enterprises.iwakura.amitracker.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import enterprises.iwakura.amitracker.constant.ProxyState;
import enterprises.iwakura.amitracker.database.entity.ProxyEntity;
import enterprises.iwakura.amitracker.object.ProxyDTO;

@Mapper(imports = {ProxyState.class}, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProxyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "score", ignore = true)
    @Mapping(target = "lastUsedAt", ignore = true)
    @Mapping(target = "state", expression = "java(ProxyState.NOT_READY)")
    ProxyEntity create(ProxyDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "protocol", ignore = true)
    @Mapping(target = "ip", ignore = true)
    @Mapping(target = "port", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "score", ignore = true)
    @Mapping(target = "lastUsedAt", ignore = true)
    ProxyEntity update(@MappingTarget ProxyEntity entity, ProxyDTO dto);

    ProxyDTO toDTO(ProxyEntity proxy);

    @AfterMapping
    default void afterMapping(ProxyEntity entity) {
        entity.calculateScore();
    }
}
