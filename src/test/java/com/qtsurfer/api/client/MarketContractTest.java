package com.qtsurfer.api.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtsurfer.api.client.invoker.ApiClient;
import com.qtsurfer.api.client.model.AuthTokenResponse;
import com.qtsurfer.api.client.model.InstrumentCatalogueResponse;
import com.qtsurfer.api.client.model.InstrumentListResponse;
import com.qtsurfer.api.client.model.PoolListResponse;
import com.qtsurfer.api.client.model.ResponseError;
import com.qtsurfer.api.client.model.LivePaperConfig;
import com.qtsurfer.api.client.model.PoolLinks;
import com.qtsurfer.api.client.model.SweepAxis;
import com.qtsurfer.api.client.model.SweepAxisOneOf;
import com.qtsurfer.api.client.model.SweepAxisOneOf1;
import com.qtsurfer.api.client.model.SweepAxisOneOf1ValuesInner;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Regression coverage for venue discrimination, absent market data and structured recovery. */
class MarketContractTest {
    private final ObjectMapper mapper = new ApiClient().getObjectMapper();

    @Test
    void emptyCataloguesStillSelectExactlyOneVenue() throws Exception {
        for (String venue : List.of("cex", "dex")) {
            String json = "{\"data\":[],\"meta\":{\"venueType\":\"" + venue + "\"},\"_links\":{}}";
            var response = mapper.readValue(json, InstrumentCatalogueResponse.class);
            Class<?> expected = "cex".equals(venue) ? InstrumentListResponse.class : PoolListResponse.class;
            assertInstanceOf(expected, response.getActualInstance());
        }
    }

    @Test
    void poolsKeepAddressesAndOptionalMarketFields() throws Exception {
        var response = mapper.readValue("""
                {"data":[{"id":"0x0000000000000000000000000000000000000001","symbol":"WETH/USDC",
                "base":"WETH","quote":"USDC","baseAddress":"0x02","quoteAddress":"0x03",
                "feePercent":0.3,"tvlUsd":20000,"volume24hUsd":1000}],
                "meta":{"venueType":"dex","segment":"uniswap-v3"},"_links":{}}
                """, InstrumentCatalogueResponse.class);
        var pools = assertInstanceOf(PoolListResponse.class, response.getActualInstance()).getData();
        assertEquals("0x02", pools.get(0).getBaseAddress());
        assertEquals("WETH/USDC", pools.get(0).getSymbol());
        assertNull(pools.get(0).getLastPrice());
        assertNull(pools.get(0).getVolume24h());
        assertNull(pools.get(0).getCreatedAt());
    }

    @Test
    void tokenScopesAreResourceActionLists() throws Exception {
        var token = mapper.readValue("""
                {"access_token":"token","token_type":"Bearer","expires_in":3600,"tier":"free",
                "scopes":{"exchanges":["read"],"backtests":["read","execute"]}}
                """, AuthTokenResponse.class);
        assertEquals(List.of("read", "execute"), token.getScopes().get("backtests"));
        assertTrue(mapper.readTree(mapper.writeValueAsString(token)).get("scopes").isObject());
    }

    @Test
    void recoveryCallKeepsItsRequestBody() throws Exception {
        var error = mapper.readValue("""
                {"message":"Use an import","reason":"OUTSIDE_WINDOW",
                "next":{"method":"POST","path":"/datasets/imports","body":{"type":"cex"}}}
                """, ResponseError.class);
        assertEquals("OUTSIDE_WINDOW", error.getReason());
        assertEquals("POST", error.getNext().getMethod().getValue());
        assertEquals("cex", mapper.valueToTree(error.getNext().getBody()).get("type").asText());
    }

    @Test
    void sweepAxesSerializeTheirFieldsWithoutMapOrWrapperLoss() throws Exception {
        var range = new SweepAxis(new SweepAxisOneOf().from(7.0).to(28.0).step(1.0));
        var rangeJson = mapper.readTree(mapper.writeValueAsString(range));
        assertEquals(7.0, rangeJson.get("from").asDouble());
        assertEquals(28.0, rangeJson.get("to").asDouble());
        assertEquals(1.0, rangeJson.get("step").asDouble());
        assertEquals(3, rangeJson.size());

        var values = new SweepAxis(new SweepAxisOneOf1().values(List.of(
                new SweepAxisOneOf1ValuesInner(true), new SweepAxisOneOf1ValuesInner(false))));
        assertEquals(mapper.readTree("{\"values\":[true,false]}"),
                mapper.readTree(mapper.writeValueAsString(values)));
    }

    @Test
    void paperConfigurationPreservesFundingAndFees() throws Exception {
        var paper = new LivePaperConfig().initialFunding(1000.0).feeRate(0.002)
                .output(LivePaperConfig.OutputEnum.MIX);
        var json = mapper.readTree(mapper.writeValueAsString(paper));
        assertEquals(1000.0, json.get("initialFunding").asDouble());
        assertEquals(0.002, json.get("feeRate").asDouble());
        assertEquals("mix", json.get("output").asText());
        var roundTrip = mapper.treeToValue(json, LivePaperConfig.class);
        assertEquals(1000.0, roundTrip.getInitialFunding());
    }

    @Test
    void poolLinksKeepSelfAndProtocolLinks() throws Exception {
        var json = mapper.readTree("""
                {"self":{"href":"/exchange/robinhood/instruments"},
                "uniswap-v3":{"href":"/exchange/robinhood/uniswap-v3/instruments"}}
                """);
        var links = mapper.treeToValue(json, PoolLinks.class);
        assertEquals("/exchange/robinhood/instruments", links.getSelf().getHref());
        assertEquals(json, mapper.readTree(mapper.writeValueAsString(links)));
    }
}
