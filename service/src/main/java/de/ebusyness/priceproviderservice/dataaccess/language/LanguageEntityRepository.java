package de.ebusyness.priceproviderservice.dataaccess.language;

import de.ebusyness.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LanguageEntityRepository extends JpaRepository<LanguageEntity, String>, JpaSpecificationExecutor<LanguageEntity> {
    LanguageEntity findByIsoKey(String isoKey);
    List<LanguageEntity> findByActive(Boolean active);
    List<LanguageEntity> findByMandatory(Boolean mandatory);
}
