package de.ebusyness.priceproviderservice.dataaccess.unit;

import de.ebusyness.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UnitEntityRepository extends JpaRepository<UnitEntity, String>, JpaSpecificationExecutor<UnitEntity> {

    UnitEntity findBySymbol(String symbol);
}