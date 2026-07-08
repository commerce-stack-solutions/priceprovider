package io.commercestacksolutions.priceproviderservice.dataaccess.unit;

import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UnitEntityRepository extends JpaRepository<UnitEntity, String>, JpaSpecificationExecutor<UnitEntity> {

    UnitEntity findBySymbol(String symbol);
}