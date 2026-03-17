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
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

/**
 * Utility class to seed initial data on application startup.
 * Uses JDBC batch inserts for price records to handle remote DB connections reliably.
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
    private final DataSource dataSource;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.seed.recompute-on-startup:true}")
    private boolean recomputeOnStartup;

    private static final boolean FORCE_RESEED = false; // Set to true to bypass count checks
    private static final int JDBC_BATCH_SIZE = 500;

    @Override
    public void run(String... args) throws Exception {
        transactionTemplate.execute(status -> {
            seedAdminUser();
            return null;
        });

        long priceRecordCount = priceRecordRepository.count();
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
        } else if (priceRecordCount < 100000) {
            log.info("Insufficient price records found. Starting fresh seeding...");
            transactionTemplate.execute(status -> {
                seedMarketData();
                return null;
            });
            generateMassivePriceRecords();
        } else {
            log.info("Sufficient data exists. Skipping seeding.");
        }
        
        if (recomputeOnStartup) {
            log.info("Triggering recomputation of health scores and seasonal patterns...");
            try {
                marketHealthScoreService.computeAllMarketScores();
            } catch (Exception e) {
                log.error("Failed to compute all market health scores during startup seeding", e);
            }

            try {
                seasonalPatternService.computeAllPatterns();
            } catch (Exception e) {
                log.error("Failed to compute all seasonal patterns during startup seeding", e);
            }
            log.info("Recomputation process finished.");
        } else {
            log.info("Startup recomputation is disabled via configuration.");
        }
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

    /**
     * Generates massive historical price records using raw JDBC batch inserts.
     * Each batch gets its own connection from the pool, making this resilient
     * to connection timeouts on remote databases (e.g. Render free-tier).
     */
    private void generateMassivePriceRecords() {
        // Load reference data using JPA (small queries, fast)
        List<Market> allMarkets = marketRepository.findAll();
        List<Commodity> allCommodities = commodityRepository.findAll();
        User admin = userRepository.findByUsername(adminUsername).orElse(null);

        if (admin == null) {
            log.warn("Admin user not found. Skipping price record seeding.");
            return;
        }

        Random random = new Random();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(12);

        log.info("Generating MASSIVE historical price records (Daily for 12 months) using JDBC batch inserts...");

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

        // Collect all records as raw data tuples (avoid holding JPA entities in memory)
        List<Object[]> batch = new ArrayList<>();
        int totalCount = 0;
        Long adminId = admin.getId();

        String insertSql = "INSERT INTO price_records (commodity_id, market_id, price, recorded_date, status, submitted_by) "
                         + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";

        for (Commodity comm : allCommodities) {
            double[] range = basePrices.getOrDefault(comm.getName(), new double[]{100, 500});
            double min = range[0];
            double max = range[1];
            Long commId = comm.getId();

            for (Market mkt : allMarkets) {
                if (random.nextDouble() > 0.95) continue;

                double cityMultiplier = 0.9 + (random.nextDouble() * 0.4);
                LocalDate current = startDate;
                double lastPrice = min + (random.nextDouble() * (max - min));
                Long mktId = mkt.getId();

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

                    batch.add(new Object[]{
                        commId, mktId,
                        BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP),
                        Date.valueOf(current),
                        PriceRecordStatus.APPROVED.name(),
                        adminId
                    });
                    
                    if (batch.size() >= JDBC_BATCH_SIZE) {
                        totalCount += flushBatchWithRetry(insertSql, batch);
                        if (totalCount % 5000 == 0) {
                            log.info("Saved {} price records so far...", totalCount);
                        }
                        batch.clear();
                    }

                    current = current.plusDays(1);
                }
            }
        }

        // Flush remaining
        if (!batch.isEmpty()) {
            totalCount += flushBatchWithRetry(insertSql, batch);
            batch.clear();
        }

        log.info("Saved total of {} new price records.", totalCount);
    }

    /**
     * Flushes a batch of records via JDBC with a fresh connection.
     * Retries once on failure to handle transient connection issues.
     */
    private int flushBatchWithRetry(String sql, List<Object[]> batch) {
        int maxRetries = 2;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return flushBatch(sql, batch);
            } catch (SQLException e) {
                log.warn("Batch insert attempt {}/{} failed: {}. {}", 
                    attempt, maxRetries, e.getMessage(),
                    attempt < maxRetries ? "Retrying..." : "Giving up this batch.");
                if (attempt == maxRetries) {
                    log.error("Failed to insert batch of {} records after {} attempts", batch.size(), maxRetries);
                }
            }
        }
        return 0;
    }

    /**
     * Executes a JDBC batch insert using a fresh connection from the pool.
     * The connection is obtained and returned per-batch, preventing long-held connections
     * from timing out on remote databases.
     */
    private int flushBatch(String sql, List<Object[]> batch) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Object[] row : batch) {
                    ps.setLong(1, (Long) row[0]);       // commodity_id
                    ps.setLong(2, (Long) row[1]);       // market_id
                    ps.setBigDecimal(3, (BigDecimal) row[2]); // price
                    ps.setDate(4, (Date) row[3]);       // recorded_date
                    ps.setString(5, (String) row[4]);   // status
                    ps.setLong(6, (Long) row[5]);       // submitted_by_id
                    ps.addBatch();
                }
                int[] results = ps.executeBatch();
                conn.commit();
                return results.length;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
