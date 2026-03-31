package io.commercestacksolutions.priceproviderservice.dataaccess.channel;

import io.commercestacksolutions.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ChannelEntityRepository extends JpaRepository<ChannelEntity, String>, JpaSpecificationExecutor<ChannelEntity> {
}
