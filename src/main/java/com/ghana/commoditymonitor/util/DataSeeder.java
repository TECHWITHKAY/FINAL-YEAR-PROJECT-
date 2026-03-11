package com.ghana.commoditymonitor.util;

import com.ghana.commoditymonitor.entity.*;
import com.ghana.commoditymonitor.enums.Role;
import com.ghana.commoditymonitor.enums.PriceRecordStatus;
import com.ghana.commoditymonitor.repository.*;
import com.ghana.commoditymonitor.service.MarketHealthScoreService;
import com.ghana.commoditymonitor.service.SeasonalPatternService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Utility class to seed initial data on application startup.
 * Specifically handles creating the initial administrator user if one doesn't exist.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final MarketRepository marketRepository;
    private final CommodityRepository commodityRepository;
    private final PriceRecordRepository priceRecordRepository;
    private final MarketHealthScoreRepository marketHealthScoreRepository;
    private final SeasonalPatternRepository seasonalPatternRepository;
    private final PasswordEncoder passwordEncoder;
    private final MarketHealthScoreService marketHealthScoreService;
    private final SeasonalPatternService seasonalPatternService;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.email}")
    private String adminEmail;

    private static final boolean FORCE_RESEED = true; // Set to true to bypass count checks

    @Override
    public void run(String... args) throws Exception {
        transactionTemplate.execute(status -> {
            seedAdminUser();
            return null;
        });

        long approvedCount = priceRecordRepository.countByStatus(PriceRecordStatus.APPROVED);
        if (FORCE_RESEED) {
            log.info("FORCE_RESEED is enabled. Performing NUCLEAR WIPE for a clean start...");
            transactionTemplate.execute(status -> {
                priceRecordRepository.deleteAllInBatch();
                marketHealthScoreRepository.deleteAllInBatch();
                seasonalPatternRepository.deleteAllInBatch();
                marketRepository.deleteAllInBatch();
                cityRepository.deleteAllInBatch();
                commodityRepository.deleteAllInBatch();
                return null;
            });
            log.info("Wipe complete. Starting fresh seeding...");
            
            transactionTemplate.execute(status -> {
                seedMarketData();
                return null;
            });
            
            generateMassivePriceRecords();
        } else if (approvedCount < 100000) {
            transactionTemplate.execute(status -> {
                seedMarketData();
                return null;
            });
            generateMassivePriceRecords();
        } else {
            log.info("Sufficient data exists. Skipping seeding.");
        }
        
        log.info("Triggering recomputation of health scores and seasonal patterns...");
        marketHealthScoreService.computeAllMarketScores();
        seasonalPatternService.computeAllPatterns();
        log.info("Recomputation complete. Dashboards should now be fully populated.");
    }

    protected void seedAdminUser() {
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            log.info("Creating default admin: {}", adminUsername);
            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .active(true)
                    .build();
            userRepository.save(admin);
        }
    }

    private void seedMarketData() {
        log.info("Starting market metadata seeding (cities, markets, commodities)...");

        // 1. Seed Cities (Expanded)
        Map<String, String[]> regions = new LinkedHashMap<>();
        regions.put("Greater Accra", new String[]{"Accra", "Tema", "Madina"});
        regions.put("Ashanti", new String[]{"Kumasi", "Obuasi", "Ejura"});
        regions.put("Northern", new String[]{"Tamale", "Yendi"});
        regions.put("Western", new String[]{"Takoradi", "Tarkwa"});
        regions.put("Eastern", new String[]{"Koforidua", "Nkawkaw"});
        regions.put("Central", new String[]{"Cape Coast", "Mankessim"});
        regions.put("Volta", new String[]{"Ho", "Hohoe"});
        regions.put("Bono", new String[]{"Sunyani", "Techiman"});
        regions.put("Upper East", new String[]{"Bolgatanga"});
        regions.put("Upper West", new String[]{"Wa"});

        Map<String, City> cityMap = new HashMap<>();
        regions.forEach((region, cities) -> {
            for (String cityName : cities) {
                City city = cityRepository.findByName(cityName)
                        .orElseGet(() -> cityRepository.save(City.builder().name(cityName).region(region).build()));
                cityMap.put(cityName, city);
            }
        });

        // 2. Seed Markets (Massive List)
        String[][] marketData = {
                {"Makola Market", "Accra"}, {"Madina Market", "Madina"}, {"Kaneshie Market", "Accra"},
                {"Kejetia Market", "Kumasi"}, {"Central Market", "Kumasi"}, {"Ejura Market", "Ejura"},
                {"Tamale Central Market", "Tamale"}, {"Aboabo Market", "Tamale"}, {"Yendi Market", "Yendi"},
                {"Market Circle", "Takoradi"}, {"Koforidua Central Market", "Koforidua"}, {"Nkawkaw Market", "Nkawkaw"},
                {"Kotokuraba Market", "Cape Coast"}, {"Mankessim Market", "Mankessim"}, 
                {"Ho Central Market", "Ho"}, {"Hohoe Market", "Hohoe"},
                {"Sunyani Central Market", "Sunyani"}, {"Techiman Market", "Techiman"},
                {"Bolgatanga Central Market", "Bolgatanga"}, {"Wa Central Market", "Wa"}
        };

        List<Market> allMarkets = new ArrayList<>();
        for (String[] m : marketData) {
            String marketName = m[0];
            City city = cityMap.get(m[1]);
            Market market = marketRepository.findAll().stream()
                    .filter(mk -> mk.getName().equals(marketName) && mk.getCity().getId().equals(city.getId()))
                    .findFirst()
                    .orElseGet(() -> marketRepository.save(Market.builder().name(marketName).city(city).build()));
            allMarkets.add(market);
        }

        // 3. Seed Commodities (Expanded)
        String[][] commodities = {
                {"Maize (White)", "Grains", "100kg Bag", "300", "500"},
                {"Maize (Yellow)", "Grains", "100kg Bag", "320", "520"},
                {"Rice (Local)", "Grains", "50kg Bag", "400", "650"},
                {"Rice (Imported)", "Grains", "50kg Bag", "600", "950"},
                {"Millet", "Grains", "93kg Bag", "450", "700"},
                {"Sorghum", "Grains", "109kg Bag", "400", "650"},
                {"Yam (Pona)", "Tubers", "100 Tubers", "1200", "2500"},
                {"Cassava", "Tubers", "91kg Bag", "150", "350"},
                {"Plantain (Apem)", "Tubers", "Bunch", "40", "120"},
                {"Plantain (Apantu)", "Tubers", "Bunch", "50", "150"},
                {"Tomato", "Vegetables", "Large Box", "400", "1500"},
                {"Onion", "Vegetables", "70kg Bag", "500", "1200"},
                {"Pepper (Fresh)", "Vegetables", "Sack", "200", "600"},
                {"Garden Eggs", "Vegetables", "Sack", "150", "450"},
                {"Cowpea (White)", "Legumes", "100kg Bag", "600", "1000"},
                {"Soya Beans", "Legumes", "100kg Bag", "500", "900"},
                {"Gari", "Tubers", "100kg Bag", "400", "800"},
                {"Groundnut", "Legumes", "Sack", "500", "900"},
                {"Pineapple", "Fruits", "Dozen", "60", "150"},
                {"Mango", "Fruits", "Box", "80", "200"}
        };

        List<Commodity> allCommodities = new ArrayList<>();
        for (String[] c : commodities) {
            String commName = c[0];
            Commodity commodity = commodityRepository.findByName(commName)
                    .orElseGet(() -> commodityRepository.save(Commodity.builder()
                            .name(commName)
                            .category(c[1])
                            .unit(c[2])
                            .build()));
            allCommodities.add(commodity);
        }
    }

    private void generateMassivePriceRecords() {
        // Find existing data
        Map<String, City> cityMap = new HashMap<>();
        cityRepository.findAll().forEach(c -> cityMap.put(c.getName(), c));
        
        List<Market> allMarkets = marketRepository.findAll();
        List<Commodity> allCommodities = commodityRepository.findAll();

        Random random = new Random();
        User admin = userRepository.findByUsername(adminUsername).orElse(null);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(12);

        log.info("Generating MASSIVE historical price records (Daily for 12 months)...");
        List<PriceRecord> batch = new ArrayList<>();
        int count = 0;

        // Base price map for commodities
        Map<String, double[]> basePrices = new HashMap<>();
        basePrices.put("Maize (White)", new double[]{300, 500});
        basePrices.put("Maize (Yellow)", new double[]{320, 520});
        basePrices.put("Rice (Local)", new double[]{400, 650});
        basePrices.put("Rice (Imported)", new double[]{600, 950});
        basePrices.put("Millet", new double[]{450, 700});
        basePrices.put("Sorghum", new double[]{400, 650});
        basePrices.put("Yam (Pona)", new double[]{1200, 2500});
        basePrices.put("Cassava", new double[]{150, 350});
        basePrices.put("Plantain (Apem)", new double[]{40, 120});
        basePrices.put("Plantain (Apantu)", new double[]{50, 150});
        basePrices.put("Tomato", new double[]{400, 1500});
        basePrices.put("Onion", new double[]{500, 1200});
        basePrices.put("Pepper (Fresh)", new double[]{200, 600});
        basePrices.put("Garden Eggs", new double[]{150, 450});
        basePrices.put("Cowpea (White)", new double[]{600, 1000});
        basePrices.put("Soya Beans", new double[]{500, 900});
        basePrices.put("Gari", new double[]{400, 800});
        basePrices.put("Groundnut", new double[]{500, 900});
        basePrices.put("Pineapple", new double[]{60, 150});
        basePrices.put("Mango", new double[]{80, 200});

        for (Commodity comm : allCommodities) {
            double[] range = basePrices.getOrDefault(comm.getName(), new double[]{100, 500});
            double min = range[0];
            double max = range[1];

            for (Market mkt : allMarkets) {
                if (random.nextDouble() > 0.95) continue;

                double cityMultiplier = 0.9 + (random.nextDouble() * 0.4);
                LocalDate current = startDate;
                double lastPrice = min + (random.nextDouble() * (max - min));

                while (current.isBefore(endDate) || current.isEqual(endDate)) {
                    double seasonalFactor = 1.0;
                    int month = current.getMonthValue();
                    if (month >= 5 && month <= 7) seasonalFactor = 1.25;
                    if (month >= 9 && month <= 11) seasonalFactor = 0.75;

                    double volatility = 0.005; 
                    double change = 1 + (random.nextDouble() * volatility * 2 - volatility);
                    double price = lastPrice * change * seasonalFactor * cityMultiplier;
                    
                    price = Math.max(min * 0.6, Math.min(max * 1.8, price));
                    lastPrice = price;

                    batch.add(PriceRecord.builder()
                            .commodity(comm)
                            .market(mkt)
                            .price(BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP))
                            .recordedDate(current)
                            .status(com.ghana.commoditymonitor.enums.PriceRecordStatus.APPROVED)
                            .submittedBy(admin)
                            .build());
                    
                    if (batch.size() >= 5000) {
                        final List<PriceRecord> toSave = new ArrayList<>(batch);
                        transactionTemplate.execute(status -> {
                            priceRecordRepository.saveAll(toSave);
                            entityManager.flush();
                            entityManager.clear();
                            return null;
                        });
                        count += toSave.size();
                        log.info("Saved {} price records so far...", count);
                        batch.clear();
                    }

                    current = current.plusDays(1);
                }
            }
        }

        if (!batch.isEmpty()) {
            final List<PriceRecord> lastBatch = batch;
            transactionTemplate.execute(status -> {
                priceRecordRepository.saveAll(lastBatch);
                entityManager.flush();
                entityManager.clear();
                return null;
            });
            count += lastBatch.size();
        }

        log.info("Saved total of {} new price records.", count);
    }
}
