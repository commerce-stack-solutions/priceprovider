package io.commercestacksolutions.priceproviderservice.dataaccess.pricerow;

import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceRowEntityRepository extends JpaRepository<PriceRowEntity, Long>, JpaSpecificationExecutor<PriceRowEntity> {
}