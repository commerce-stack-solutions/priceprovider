package de.ebusyness.priceproviderservice.dataaccess.taxclass;

import de.ebusyness.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TaxClassEntityRepository extends JpaRepository<TaxClassEntity, String>, JpaSpecificationExecutor<TaxClassEntity> {
    TaxClassEntity findByTaxClassId(String taxClassId);
}
