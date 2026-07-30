package com.holdhive.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.holdhive.common.error.ApiException;
import com.holdhive.portfolio.domain.AssetType;

class FundLookthroughServiceTest {

    private final FundLookthroughService service = new FundLookthroughService();

    @Test
    void returnsDemoLookthroughForKnownFundInstrument() {
        FundLookthrough lookthrough = service.getLookthrough(102L);

        assertThat(lookthrough.fundInstrumentId()).isEqualTo(102L);
        assertThat(lookthrough.ticker()).isEqualTo("VOO");
        assertThat(lookthrough.assetType()).isEqualTo(AssetType.ETF);
        assertThat(lookthrough.coveragePercent()).isEqualByComparingTo("41.15000000");
        assertThat(lookthrough.holdings()).extracting(FundComponent::ticker)
            .containsExactly("AAPL", "MSFT", "NVDA");
        assertThat(lookthrough.holdings()).extracting(FundComponent::assetType)
            .containsOnly(AssetType.STOCK);
        assertThat(lookthrough.warnings()).singleElement()
            .asString()
            .contains("latest available disclosure");
    }

    @Test
    void rejectsUnknownFundInstrumentWithNotFoundError() {
        assertThatThrownBy(() -> service.getLookthrough(999L))
            .isInstanceOfSatisfying(ApiException.class, exception -> {
                assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(exception.code()).isEqualTo("FUND_LOOKTHROUGH_NOT_FOUND");
            });
    }
}
