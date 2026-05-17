package com.ghana.commoditymonitor.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import com.ghana.commoditymonitor.entity.Commodity;
import com.ghana.commoditymonitor.entity.Market;
import com.ghana.commoditymonitor.entity.PriceRecord;
import com.ghana.commoditymonitor.enums.PriceRecordStatus;
import com.ghana.commoditymonitor.repository.CommodityRepository;
import com.ghana.commoditymonitor.repository.MarketRepository;
import com.ghana.commoditymonitor.repository.PriceRecordRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class EsokoScraperService {

    private final CommodityRepository commodityRepository;
    private final MarketRepository marketRepository;
    private final PriceRecordRepository priceRecordRepository;

    @Value("${app.scraper.esoko-url:https://marketplace.esoko.com/}")
    private String esokoUrl;

    @Value("${app.scraper.enabled:true}")
    private boolean scraperEnabled;

    /**
     * Internal record to hold scraped data before it is mapped to our database entities.
     */
    @Data
    @Builder
    public static class ScrapedPriceData {
        private String commodityName;
        private String marketName;
        private BigDecimal price;
    }

    public List<ScrapedPriceData> fetchAndParsePrices() {
        if (!scraperEnabled) {
            log.info("Esoko Scraper is disabled in configuration.");
            return List.of();
        }

        log.info("Starting Esoko price scraping from URL: {}", esokoUrl);
        List<ScrapedPriceData> scrapedDataList = new ArrayList<>();

        try {
            // 1. Connect to the URL and fetch the HTML document
            // We use a user agent to mimic a real browser to avoid being blocked
            Document doc = Jsoup.connect(esokoUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000)
                    .get();

            // 2. Parse the HTML to extract data
            // NOTE: These CSS selectors (.price-table, tr, td) are placeholders. 
            // We will need to adjust them once we see the actual HTML structure.
            Elements rows = doc.select("table tr"); // Generic selector for table rows

            if (rows.isEmpty()) {
                log.warn("No table rows found! The site might be using dynamic JS rendering or the structure is different.");
                // For debugging: dump a snippet of the body to see what we actually fetched
                log.debug("Fetched body snippet: {}", doc.body().text().substring(0, Math.min(doc.body().text().length(), 500)));
            }

            for (Element row : rows) {
                Elements cols = row.select("td");

                if (cols.size() >= 3) {
                    try {
                        String commodityName = cols.get(0).text().trim();
                        String marketName = cols.get(1).text().trim();
                        String priceStr = cols.get(2).text().replaceAll("[^\\d.]", ""); 
                        
                        if (!commodityName.isEmpty() && !marketName.isEmpty() && !priceStr.isEmpty()) {
                            BigDecimal price = new BigDecimal(priceStr);
                            
                            ScrapedPriceData data = ScrapedPriceData.builder()
                                    .commodityName(commodityName)
                                    .marketName(marketName)
                                    .price(price)
                                    .build();
                                    
                            scrapedDataList.add(data);
                            log.debug("Extracted: {}", data);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse price on row: {}", row.text());
                    }
                }
            }

            log.info("Successfully scraped {} price records.", scrapedDataList.size());

        } catch (IOException e) {
            log.error("Error connecting to Esoko Marketplace: {}", e.getMessage());
        }

        return scrapedDataList;
    }

    @Transactional
    public void runScrapingJob() {
        log.info("Starting Esoko scraping job...");
        List<ScrapedPriceData> scrapedData = fetchAndParsePrices();
        
        if (scrapedData.isEmpty()) {
            log.info("No data scraped. Aborting persistence.");
            return;
        }
        
        int savedCount = 0;
        int unknownEntitiesCount = 0;
        LocalDate today = LocalDate.now();

        for (ScrapedPriceData data : scrapedData) {
            Optional<Commodity> commodityOpt = commodityRepository.findByNameIgnoreCase(data.getCommodityName());
            Optional<Market> marketOpt = marketRepository.findByNameIgnoreCase(data.getMarketName());

            if (commodityOpt.isPresent() && marketOpt.isPresent()) {
                PriceRecord record = PriceRecord.builder()
                        .commodity(commodityOpt.get())
                        .market(marketOpt.get())
                        .price(data.getPrice())
                        .recordedDate(today)
                        .source("ESOKO_AUTOMATED")
                        .status(PriceRecordStatus.APPROVED)
                        .build();

                priceRecordRepository.save(record);
                savedCount++;
            } else {
                unknownEntitiesCount++;
                log.debug("Skipping record due to unmapped entities. Commodity: '{}' (Found: {}), Market: '{}' (Found: {})", 
                        data.getCommodityName(), commodityOpt.isPresent(), 
                        data.getMarketName(), marketOpt.isPresent());
            }
        }

        log.info("Scraping job completed. Saved {} records. Skipped {} records due to unmapped entities.", 
                savedCount, unknownEntitiesCount);
    }
}
