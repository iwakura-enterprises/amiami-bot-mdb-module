package enterprises.iwakura.amitracker.sigewine;

import org.mapstruct.factory.Mappers;

import enterprises.iwakura.amitracker.mapper.LimitationMapper;
import enterprises.iwakura.amitracker.mapper.ProxyMapper;
import enterprises.iwakura.sigewine.core.annotations.Bean;

public class MappersTreatment {

    @Bean
    public ProxyMapper proxyMapper() {
        return Mappers.getMapper(ProxyMapper.class);
    }

    @Bean
    public LimitationMapper limitationMapper() {
        return Mappers.getMapper(LimitationMapper.class);
    }

}
