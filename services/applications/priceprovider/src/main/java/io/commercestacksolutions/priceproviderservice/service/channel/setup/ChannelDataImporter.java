package io.commercestacksolutions.priceproviderservice.service.channel.setup;

import io.commercestacksolutions.commons.service.setup.AbstractSetupDataImporter;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import io.commercestacksolutions.priceproviderservice.service.channel.ChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChannelDataImporter extends AbstractSetupDataImporter<ChannelEntity> {

    @Autowired
    public ChannelDataImporter(ChannelService entityService) {
        super(entityService);
    }

    @Override
    public int getPriority() {
        return 80; // Load after tax classes (70) but before price rows
    }

    @Override
    public String getEntityTypeName() {
        return "Channel";
    }

}
